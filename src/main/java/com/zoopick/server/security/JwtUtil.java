package com.zoopick.server.security;

import com.zoopick.server.exception.AccessTokenException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.jspecify.annotations.NullMarked;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
@NullMarked
public class JwtUtil {
    // 토큰 만료 시간 1일
    private static final long EXPIRATION_TIME = 1000 * 60 * 60 * 24;

    private final SecretKey secretKey;

    public JwtUtil(@Value("${jwt.secret}") String secret) {
        secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(String email) {
        return Jwts.builder()
                .subject(email)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(secretKey)
                .compact();
    }

    public String extractEmail(String token) {
        return getClaims(token).getSubject();
    }

    /**
     * 토근의 만료 시간, 인증 확인후 결과 반환한다.<br/>
     * 토큰의 무효화 검사가 필요할 경우 {@link com.zoopick.server.service.TokenValidationService#validateTokenOrThrow(String)} 사용
     *
     * @param token 검사할 토큰
     * @return True 일 시 만료된 토큰
     * @throws AccessTokenException 토큰이 유효하지 않음
     * @see com.zoopick.server.service.TokenValidationService#validateTokenOrThrow(String)
     */
    public boolean isExpirationValid(String token) {
        return !getClaims(token).getExpiration().before(new Date());
    }

    private Claims getClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException exception) {
            throw new AccessTokenException(exception.getMessage(), exception);
        }
    }

    /**
     * token의 만료까지 남은 시간을 밀리초 단위로 반환한다.
     *
     * @param token 토큰
     * @return 만료까지 남은 시간 (ms)
     */
    public long getRemainingExpirationTime(String token) {
        Date expiration = getClaims(token).getExpiration();
        return (expiration.getTime() - System.currentTimeMillis());
    }
}