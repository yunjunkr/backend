package com.zoopick.server.service;

import com.zoopick.server.dto.auth.*;
import com.zoopick.server.entity.EmailAuth;
import com.zoopick.server.entity.Role;
import com.zoopick.server.entity.User;
import com.zoopick.server.exception.AccessTokenException;
import com.zoopick.server.exception.BadRequestException;
import com.zoopick.server.exception.DataNotFoundException;
import com.zoopick.server.repository.EmailAuthRedisRepository;
import com.zoopick.server.repository.UserRepository;
import com.zoopick.server.security.JwtUtil;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
@NullMarked
public class AuthService {
    // Expiration duration in minutes
    private static final int EMAIL_CERTIFICATION_EXPIRE_DURATION = 3;

    private final UserRepository userRepository;
    private final EmailAuthRedisRepository emailAuthRedisRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final JwtUtil jwtUtil;
    private final TokenValidationService tokenValidationService;

    private static LocalDateTime createNewExpireTime() {
        return LocalDateTime.now().plusMinutes(EMAIL_CERTIFICATION_EXPIRE_DURATION);
    }

    public SignupResult signup(SignupRequest request) {
        EmailAuth emailAuth = emailAuthRedisRepository.getOrThrow(request.getSchoolEmail());
        if (!emailAuth.isVerified())
            throw new BadRequestException("이메일 인증이 완료되지 않았습니다.", request.getSchoolEmail() + " is not verified.");
        if (emailAuth.isSignupExpired()) {
            emailAuthRedisRepository.delete(emailAuth.getEmail());
            throw new BadRequestException("인증 코드가 만료되었습니다.", request.getSchoolEmail() + " is signup expired.");
        }
        if (userRepository.findByNickname(request.getNickname()).isPresent())
            throw new BadRequestException("이미 사용중인 닉네임입니다.", request.getNickname() + " is already in use.");

        User user = User.builder()
                .schoolEmail(request.getSchoolEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .nickname(request.getNickname())
                .role(Role.STUDENT)
                .department(request.getDepartment())
                .grade(request.getGrade())
                .build();

        User savedUser = userRepository.save(user);
        emailAuthRedisRepository.delete(emailAuth.getEmail());

        String accessToken = jwtUtil.generateToken(savedUser.getSchoolEmail());

        return new SignupResult(savedUser.getId(), "회원가입이 완료되었습니다.", accessToken);
    }

    public NicknameCheckResult checkNickname(String nickname) {
        if (userRepository.findByNickname(nickname).isEmpty())
            return new NicknameCheckResult(true, "사용 가능한 닉네임입니다.");
        throw new BadRequestException("이미 사용 중인 닉네임입니다.", nickname + " is already in use.");
    }

    public LoginResult login(LoginRequest request) {
        User user = userRepository.findBySchoolEmail(request.getSchoolEmail())
                .orElseThrow(() -> DataNotFoundException.from("사용자", request.getSchoolEmail()));

        if (passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            String accessToken = jwtUtil.generateToken(user.getSchoolEmail());
            return LoginResult.builder()
                    .accessToken(accessToken)
                    .grade(user.getGrade())
                    .department(user.getDepartment())
                    .nickname(user.getNickname())
                    .message("로그인 성공")
                    .build();
        }
        throw new BadRequestException("로그인에 실패했습니다.", request.getSchoolEmail() + " failed password.");
    }

    /**
     * originalToken이 유효한지 확인하고 새로운 토큰 반환
     *
     * @param originalToken 기존 토큰
     * @return 만료 시간이 갱신된 새로운 토큰
     */
    public String validateAccessToken(String originalToken) {
        if (tokenValidationService.validateTokenOrThrow(originalToken)) {
            String email = jwtUtil.extractEmail(originalToken);
            userRepository.findBySchoolEmail(email)
                    .orElseThrow(() -> DataNotFoundException.from("사용자", email));
            tokenValidationService.invalidateToken(originalToken);
            return jwtUtil.generateToken(email);
        }
        throw new AccessTokenException(originalToken + " is expired or invalidated.");
    }

    @Transactional
    public void sendCertificationEmail(EmailCertificationRequest request)
            throws MessagingException, IOException {
        String email = request.getEmail();

        if (userRepository.findBySchoolEmail(email).isPresent())
            throw new BadRequestException("이미 가입된 이메일입니다.", email + " is already in use.");

        String certificationNumber = generateCertificationNumber();

        EmailAuth emailAuth = new EmailAuth(email, certificationNumber, createNewExpireTime(), false);
        emailAuthRedisRepository.save(emailAuth);

        emailService.senderCertificationMail(email, certificationNumber);
    }

    @Transactional
    public void verifyCertificationCode(CheckCertificationRequest request) {
        EmailAuth emailAuth = emailAuthRedisRepository.getOrThrow(request.getEmail());

        if (!emailAuth.getCertificationCode().equals(request.getCertificationNumber())) {
            throw new BadRequestException(
                    "인증번호가 일치하지 않습니다.",
                    "%s's Certification code does not match.".formatted(emailAuth.getEmail())
            );
        }
        if (emailAuth.isCertificationCodeExpired()) {
            emailAuthRedisRepository.delete(emailAuth.getEmail());
            throw new BadRequestException("이메일 인증이 만료되었습니다.", request.getEmail() + " has expired for certification.");
        }
        emailAuth.setVerified(true);
        emailAuthRedisRepository.save(emailAuth);
    }

    private String generateCertificationNumber() {
        StringBuilder certificationNumber = new StringBuilder();
        Random random = new Random();

        for (int count = 0; count < 6; count++) {
            int digit = random.nextInt(10);
            certificationNumber.append(digit);
        }
        return certificationNumber.toString();
    }

    public void logout(String accessToken) {
        if (!tokenValidationService.validateTokenOrThrow(accessToken))
            throw new AccessTokenException(accessToken + " is expired or invalidated.");

        tokenValidationService.invalidateToken(accessToken);
    }
}
