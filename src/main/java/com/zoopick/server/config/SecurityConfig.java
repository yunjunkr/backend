package com.zoopick.server.config;

import com.zoopick.server.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        http.csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable);

        http.authorizeHttpRequests((auth) -> auth
                // 1. 누구나 접근 가능한 경로 (로그인 전)
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                .requestMatchers("/", "/Login", "/join").permitAll()
                .requestMatchers(HttpMethod.GET, "/images/**").permitAll()

                // Vision 및 CCTV API 허용 (테스트용)
                .requestMatchers("/api/vision/**", "/api/cctv/**", "/api/internal/**").permitAll()

                .requestMatchers("/api/metadata/**").permitAll()
                
                // [수정 포인트] 실제 테스트 주소인 /api/auth/... 계열을 모두 허용 목록에 추가
                .requestMatchers("/api/auth/login", "/api/auth/signup", "/api/auth/check-nickname", "/api/auth/validate").permitAll()
                .requestMatchers("/api/auth/certification", "/api/auth/check-certification", "/api/auth/verify").permitAll()

                .requestMatchers("/api/items/post/list/**").permitAll()

                // 2. 관리자 및 기타
                .requestMatchers("/admin/**").hasRole("ADMIN")

                // 3. 그 외 모든 API (로그아웃, 내 정보 조회 등)는 반드시 '인증' 필요
                .anyRequest().authenticated());

        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        http.sessionManagement((session) -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        return http.build();
    }
}