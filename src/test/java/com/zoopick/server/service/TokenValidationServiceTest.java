package com.zoopick.server.service;

import com.zoopick.server.exception.AccessTokenException;
import com.zoopick.server.security.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TokenValidationServiceTest {

    @InjectMocks
    private TokenValidationService tokenValidationService;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Test
    @DisplayName("토큰 무효화 - 만료 시간이 남아있다면 Redis에 블랙리스트 등록")
    void invalidateToken_Success() throws Exception {
        // given
        String token = "valid.access.token";
        long remainingTime = 10000L; // 10초

        given(jwtUtil.getRemainingExpirationTime(token)).willReturn(remainingTime);
        given(redisTemplate.opsForValue()).willReturn(valueOperations);

        // when
        tokenValidationService.invalidateToken(token);

        // then
        verify(valueOperations).set(token, "logout", remainingTime, TimeUnit.MILLISECONDS);
    }

    @Test
    @DisplayName("토큰 무효화 - 이미 만료 시간이 지났다면 Redis에 저장하지 않고 무시")
    void invalidateToken_IgnoreIfExpired() throws Exception {
        // given
        String token = "expired.access.token";
        long remainingTime = 0L; // 만료됨

        given(jwtUtil.getRemainingExpirationTime(token)).willReturn(remainingTime);

        // when
        tokenValidationService.invalidateToken(token);

        // then
        // opsForValue().set()이 호출되지 않았는지 검증
        verify(redisTemplate, never()).opsForValue();
    }

    @Test
    @DisplayName("토큰 검증 (Throw) - 블랙리스트에 없고 유효하면 true 반환")
    void validateTokenOrThrow_Success() {
        // given
        String token = "valid.access.token";

        given(redisTemplate.hasKey(token)).willReturn(false); // 로그아웃 안 된 토큰
        given(jwtUtil.isExpirationValid(token)).willReturn(true); // 만료 안 된 토큰

        // when
        boolean isValid = tokenValidationService.validateTokenOrThrow(token);

        // then
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("토큰 검증 (Throw) - Redis 블랙리스트에 존재하면 false 반환")
    void validateTokenOrThrow_Fail_Blacklisted() {
        // given
        String token = "logout.access.token";

        given(redisTemplate.hasKey(token)).willReturn(true); // 로그아웃(블랙리스트) 처리됨

        // when
        boolean isValid = tokenValidationService.validateTokenOrThrow(token);

        // then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("토큰 예외 없는 검증 - 유효한 토큰일 때 true 반환")
    void validateToken_Success() {
        // given
        String token = "valid.access.token";

        given(redisTemplate.hasKey(token)).willReturn(false);
        given(jwtUtil.isExpirationValid(token)).willReturn(true);

        // when
        boolean isValid = tokenValidationService.validateToken(token);

        // then
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("토큰 예외 없는 검증 - 예외가 발생하면 예외를 잡아서 false 반환")
    void validateToken_Fail_ReturnsFalseOnException() {
        // given
        String token = "invalid.access.token";

        given(redisTemplate.hasKey(token)).willReturn(false);
        // JwtUtil에서 검증 실패로 AccessTokenException이 발생한다고 가정
        given(jwtUtil.isExpirationValid(token)).willThrow(new AccessTokenException("Invalid token"));

        // when
        boolean isValid = tokenValidationService.validateToken(token);

        // then
        assertThat(isValid).isFalse();
    }
}