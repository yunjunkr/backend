package com.zoopick.server.service;

import com.zoopick.server.dto.auth.*;
import com.zoopick.server.entity.EmailAuth;
import com.zoopick.server.entity.User;
import com.zoopick.server.exception.AccessTokenException;
import com.zoopick.server.exception.BadRequestException;
import com.zoopick.server.exception.DataNotFoundException;
import com.zoopick.server.repository.EmailAuthRedisRepository;
import com.zoopick.server.repository.UserRepository;
import com.zoopick.server.security.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private EmailAuthRedisRepository emailAuthRedisRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private EmailService emailService;
    @Mock private JwtUtil jwtUtil;
    @Mock private TokenValidationService tokenValidationService;

    @InjectMocks
    private AuthService authService;


    // 1. 닉네임 중복 검사 테스트 (checkNickname)


    @Test
    @DisplayName("사용 가능한 닉네임일 경우 성공 결과를 반환한다.")
    void checkNickname_Success() {
        // Given
        String nickname = "새로운닉네임";
        given(userRepository.findByNickname(nickname)).willReturn(Optional.empty());

        // When
        NicknameCheckResult result = authService.checkNickname(nickname);

        // Then
        assertThat(result.isAvailable()).isTrue();
        assertThat(result.getMessage()).isEqualTo("사용 가능한 닉네임입니다.");
    }

    @Test
    @DisplayName("이미 존재하는 닉네임일 경우 예외가 발생한다.")
    void checkNickname_Fail_Duplicated() {
        // Given
        String nickname = "중복닉네임";
        User existingUser = User.builder().nickname(nickname).build();
        given(userRepository.findByNickname(nickname)).willReturn(Optional.of(existingUser));

        // When & Then
        assertThatThrownBy(() -> authService.checkNickname(nickname))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(nickname+" is already in use.");
    }


    // 2. 회원가입 테스트 (signup)


    @Test
    @DisplayName("이메일 인증이 완료되지 않은 상태로 가입 시 예외가 발생한다.")
    void signup_Fail_NotVerifiedEmail() {
        // Given
        SignupRequest request = createSignupRequest();
        EmailAuth unverifiedAuth = new EmailAuth(request.getSchoolEmail(), "123456", LocalDateTime.now().plusMinutes(5), false);

        given(emailAuthRedisRepository.getOrThrow(request.getSchoolEmail())).willReturn(unverifiedAuth);

        // When & Then
        assertThatThrownBy(() -> authService.signup(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(request.getSchoolEmail() + " is not verified.");
    }

    @Test
    @DisplayName("가입 제한 시간(만료 시간)이 지난 상태로 가입 시 예외가 발생한다.")
    void signup_Fail_SignupExpired() {
        // Given
        SignupRequest request = createSignupRequest();
        LocalDateTime expiredTime = LocalDateTime.now().minusMinutes(35);
        EmailAuth expiredAuth = new EmailAuth(request.getSchoolEmail(), "123456", expiredTime, true);

        given(emailAuthRedisRepository.getOrThrow(request.getSchoolEmail())).willReturn(expiredAuth);

        // When & Then
        assertThatThrownBy(() -> authService.signup(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(request.getSchoolEmail() + " is signup expired.");
    }

    @Test
    @DisplayName("이메일 인증은 완료되었으나, 닉네임이 이미 사용 중인 경우 예외가 발생한다.")
    void signup_Fail_DuplicatedNickname() {
        // Given
        SignupRequest request = createSignupRequest();
        EmailAuth validAuth = new EmailAuth(request.getSchoolEmail(), "123456", LocalDateTime.now().plusMinutes(5), true);

        given(emailAuthRedisRepository.getOrThrow(request.getSchoolEmail())).willReturn(validAuth);
        given(userRepository.findByNickname(request.getNickname())).willReturn(Optional.of(User.builder().build()));

        // When & Then
        assertThatThrownBy(() -> authService.signup(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("is already in use.");
    }

    @Test
    @DisplayName("정상적인 정보로 회원가입에 성공한다.")
    void signup_Success() {
        // Given
        SignupRequest request = createSignupRequest();
        EmailAuth validAuth = new EmailAuth(request.getSchoolEmail(), "123456", LocalDateTime.now().plusMinutes(5), true);
        User savedUser = User.builder().id(1L).schoolEmail(request.getSchoolEmail()).nickname(request.getNickname()).build();

        given(emailAuthRedisRepository.getOrThrow(request.getSchoolEmail())).willReturn(validAuth);
        given(userRepository.findByNickname(request.getNickname())).willReturn(Optional.empty());
        given(passwordEncoder.encode(request.getPassword())).willReturn("encodedPassword");
        given(userRepository.save(any(User.class))).willReturn(savedUser);
        given(jwtUtil.generateToken(request.getSchoolEmail())).willReturn("mock.jwt.token");

        // When
        SignupResult result = authService.signup(request);

        // Then
        assertThat(result.getUserId()).isEqualTo(1L);
        assertThat(result.getMessage()).isEqualTo("회원가입이 완료되었습니다.");
        assertThat(result.getAccessToken()).isEqualTo("mock.jwt.token");
        verify(emailAuthRedisRepository).delete(request.getSchoolEmail());
    }

    // 테스트용 픽스처(Fixture) 생성 메서드
    private SignupRequest createSignupRequest() {
        SignupRequest request = new SignupRequest();
        request.setSchoolEmail("test@mju.ac.kr");
        request.setPassword("password123!");
        request.setNickname("캡스톤테스트");
        request.setDepartment("컴퓨터공학과");
        request.setGrade("4");
        return request;
    }


    // 3. 로그인 테스트 (login)


    @Test
    @DisplayName("가입되지 않은 이메일로 로그인 시 예외가 발생한다.")
    void login_Fail_UserNotFound() {
        // Given
        LoginRequest request = new LoginRequest();
        ReflectionTestUtils.setField(request, "schoolEmail", "notfound@mju.ac.kr");
        ReflectionTestUtils.setField(request, "password", "password123!");

        given(userRepository.findBySchoolEmail(request.getSchoolEmail())).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(DataNotFoundException.class);
    }

    @Test
    @DisplayName("비밀번호가 일치하지 않으면 로그인에 실패하고 예외가 발생한다.")
    void login_Fail_WrongPassword() {
        // Given
        LoginRequest request = new LoginRequest();
        ReflectionTestUtils.setField(request, "schoolEmail", "test@mju.ac.kr");
        ReflectionTestUtils.setField(request, "password", "wrongPassword!");

        User existingUser = User.builder()
                .schoolEmail("test@mju.ac.kr")
                .password("encodedPassword")
                .build();

        given(userRepository.findBySchoolEmail(request.getSchoolEmail())).willReturn(Optional.of(existingUser));
        given(passwordEncoder.matches(request.getPassword(), existingUser.getPassword())).willReturn(false);

        // When & Then
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(request.getSchoolEmail() + " failed password.");
    }

    @Test
    @DisplayName("정상적인 이메일과 비밀번호로 로그인에 성공한다.")
    void login_Success() {
        // Given
        LoginRequest request = new LoginRequest();
        ReflectionTestUtils.setField(request, "schoolEmail", "test@mju.ac.kr");
        ReflectionTestUtils.setField(request, "password", "password123!");

        User existingUser = User.builder()
                .schoolEmail("test@mju.ac.kr")
                .password("encodedPassword")
                .nickname("주픽이")
                .department("컴퓨터공학")
                .grade("3")
                .build();

        given(userRepository.findBySchoolEmail(request.getSchoolEmail())).willReturn(Optional.of(existingUser));
        given(passwordEncoder.matches(request.getPassword(), existingUser.getPassword())).willReturn(true);
        given(jwtUtil.generateToken(existingUser.getSchoolEmail())).willReturn("mock.access.token");

        // When
        LoginResult result = authService.login(request);

        // Then
        assertThat(result.getAccessToken()).isEqualTo("mock.access.token");
        assertThat(result.getNickname()).isEqualTo("주픽이");
        assertThat(result.getGrade()).isEqualTo("3");
        assertThat(result.getMessage()).isEqualTo("로그인 성공");
    }


    // 4. 이메일 인증 발송 테스트 (sendCertificationEmail)


    @Test
    @DisplayName("이미 가입된 이메일로 인증을 요청하면 예외가 발생한다.")
    void sendCertificationEmail_Fail_AlreadyJoined() {
        // Given
        EmailCertificationRequest request = new EmailCertificationRequest();
        request.setEmail("test@mju.ac.kr");

        given(userRepository.findBySchoolEmail(request.getEmail())).willReturn(Optional.of(User.builder().build()));

        // When & Then
        assertThatThrownBy(() -> authService.sendCertificationEmail(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(request.getEmail() + " is already in use.");
    }

    @Test
    @DisplayName("정상적인 이메일이면 인증 메일이 발송되고 Redis에 저장된다.")
    void sendCertificationEmail_Success() throws Exception {
        // Given
        EmailCertificationRequest request = new EmailCertificationRequest();
        request.setEmail("test@mju.ac.kr");

        given(userRepository.findBySchoolEmail(request.getEmail())).willReturn(Optional.empty());

        // When
        authService.sendCertificationEmail(request);

        // Then
        verify(emailAuthRedisRepository).save(any(EmailAuth.class));
        verify(emailService).senderCertificationMail(anyString(), anyString());
    }


    // 5. 이메일 인증 코드 검증 테스트 (verifyCertificationCode)


    @Test
    @DisplayName("제출한 인증번호가 일치하지 않으면 예외가 발생한다.")
    void verifyCertificationCode_Fail_Mismatch() {
        // Given
        CheckCertificationRequest request = new CheckCertificationRequest();
        request.setEmail("test@mju.ac.kr");
        request.setCertificationNumber("000000");

        EmailAuth emailAuth = new EmailAuth(request.getEmail(), "123456", LocalDateTime.now().plusMinutes(3), false);
        given(emailAuthRedisRepository.getOrThrow(request.getEmail())).willReturn(emailAuth);

        // When & Then
        assertThatThrownBy(() -> authService.verifyCertificationCode(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("does not match");
    }

    @Test
    @DisplayName("제출한 인증번호는 일치하지만, 인증 시간이 만료된 경우 예외가 발생하고 Redis에서 삭제된다.")
    void verifyCertificationCode_Fail_Expired() {
        // Given
        CheckCertificationRequest request = new CheckCertificationRequest();
        request.setEmail("test@mju.ac.kr");
        request.setCertificationNumber("123456");

        // 만료된 인증 객체 생성 (현재 시간보다 1분 과거)
        EmailAuth expiredAuth = new EmailAuth(request.getEmail(), "123456", LocalDateTime.now().minusMinutes(1), false);
        given(emailAuthRedisRepository.getOrThrow(request.getEmail())).willReturn(expiredAuth);

        // When & Then
        assertThatThrownBy(() -> authService.verifyCertificationCode(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("expired for certification");

        verify(emailAuthRedisRepository).delete(request.getEmail());
    }

    @Test
    @DisplayName("제출한 인증번호가 일치하고 만료되지 않았다면 이메일 인증이 완료된다.")
    void verifyCertificationCode_Success() {
        // Given
        CheckCertificationRequest request = new CheckCertificationRequest();
        request.setEmail("test@mju.ac.kr");
        request.setCertificationNumber("123456");

        EmailAuth emailAuth = new EmailAuth(request.getEmail(), "123456", LocalDateTime.now().plusMinutes(3), false);
        given(emailAuthRedisRepository.getOrThrow(request.getEmail())).willReturn(emailAuth);

        // When
        authService.verifyCertificationCode(request);

        // Then
        assertThat(emailAuth.isVerified()).isTrue();
        verify(emailAuthRedisRepository).save(emailAuth);
    }


    // 6. Access Token 검증 및 재발급 테스트 (validateAccessToken)


    @Test
    @DisplayName("유효하지 않은 토큰일 경우 AccessTokenException이 발생한다.")
    void validateAccessToken_Fail_InvalidToken() {
        // Given
        String invalidToken = "invalid.token";
        given(tokenValidationService.validateTokenOrThrow(invalidToken)).willReturn(false);

        // When & Then
        assertThatThrownBy(() -> authService.validateAccessToken(invalidToken))
                .isInstanceOf(AccessTokenException.class)
                .hasMessageContaining("expired or invalidated");
    }

    @Test
    @DisplayName("토큰은 유효하지만 DB에 해당하는 유저가 없을 경우 예외가 발생한다.")
    void validateAccessToken_Fail_UserNotFound() {
        // Given
        String oldToken = "old.jwt.token";
        String email = "notfound@mju.ac.kr";

        given(tokenValidationService.validateTokenOrThrow(oldToken)).willReturn(true);
        given(jwtUtil.extractEmail(oldToken)).willReturn(email);
        given(userRepository.findBySchoolEmail(email)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> authService.validateAccessToken(oldToken))
                .isInstanceOf(DataNotFoundException.class);
    }

    @Test
    @DisplayName("유효한 토큰일 경우 기존 토큰을 무효화하고 새로운 토큰을 발급한다.")
    void validateAccessToken_Success() {
        // Given
        String oldToken = "old.jwt.token";
        String email = "test@mju.ac.kr";

        given(tokenValidationService.validateTokenOrThrow(oldToken)).willReturn(true);
        given(jwtUtil.extractEmail(oldToken)).willReturn(email);
        given(userRepository.findBySchoolEmail(email)).willReturn(Optional.of(User.builder().build()));
        given(jwtUtil.generateToken(email)).willReturn("new.jwt.token");

        // When
        String resultToken = authService.validateAccessToken(oldToken);

        // Then
        assertThat(resultToken).isEqualTo("new.jwt.token");
        verify(tokenValidationService).invalidateToken(oldToken);
    }


    // 7. 로그아웃 테스트 (logout)


    @Test
    @DisplayName("유효하지 않은 토큰으로 로그아웃 시 예외가 발생한다.")
    void logout_Fail_InvalidToken() {
        // Given
        String invalidToken = "invalid.token";
        given(tokenValidationService.validateTokenOrThrow(invalidToken)).willReturn(false);

        // When & Then
        assertThatThrownBy(() -> authService.logout(invalidToken))
                .isInstanceOf(AccessTokenException.class)
                .hasMessageContaining("expired or invalidated");
    }

    @Test
    @DisplayName("로그아웃 시 현재 유효한 토큰을 무효화(블랙리스트 처리)한다.")
    void logout_Success() {
        // Given
        String token = "valid.jwt.token";
        given(tokenValidationService.validateTokenOrThrow(token)).willReturn(true);

        // When
        authService.logout(token);

        // Then
        verify(tokenValidationService).invalidateToken(token);
    }
}