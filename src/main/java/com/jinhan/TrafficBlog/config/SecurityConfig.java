package com.jinhan.TrafficBlog.config;

import com.jinhan.TrafficBlog.jwt.JwtAuthenticationFilter;
import com.jinhan.TrafficBlog.jwt.JwtUtil;
import com.jinhan.TrafficBlog.service.CustomUserDetailsService;
import com.jinhan.TrafficBlog.service.JwtBlacklistService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

//Spring Security 및 인증 설정을 위한 구성 클래스
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    // 사용자 인증을 위한 커스텀 UserDetailsService
    @Autowired
    private CustomUserDetailsService userDetailsService;

    // JWT 토큰 생성 및 검증을 위한 유틸리티
    @Autowired
    private JwtUtil jwtUtil;

    // JWT 블랙리스트 관리 서비스 (로그아웃, 무효화된 토큰 처리)
    @Autowired
    private JwtBlacklistService jwtBlacklistService;

    /**
     * Security 필터 체인 설정
     * 애플리케이션의 보안 정책을 정의하는 메서드
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CSRF (Cross-Site Request Forgery) 보호 비활성화
                // API 기반 서비스에서는 주로 비활성화 (토큰 기반 인증 사용)
                .csrf(csrf -> csrf.disable())

                // HTTP 요청에 대한 접근 권한 설정
                .authorizeHttpRequests(authz -> authz
                        // 특정 엔드포인트에 대해 무제한 접근 허용
                        .requestMatchers(
                                "/swagger-ui/**",           // Swagger UI
                                "/v3/api-docs/**",          // API 문서
                                "/api/users/signUp",        // 회원가입
                                "/api/users/login",         // 로그인
                                "/api/ads",                 // 광고 관련
                                "/api/ads/**"               // 모든 광고 관련 경로
                        ).permitAll()
                        // 그 외 모든 요청은 인증 필요
                        .anyRequest().authenticated()
                )

                // 세션 관리 정책 설정
                .sessionManagement(session -> session
                        // Stateless 세션 정책 - JWT 토큰 기반 인증에 적합
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // JWT 인증 필터 추가
                // 기본 인증 필터 이전에 실행되어 토큰 기반 인증 수행
                .addFilterBefore(
                        new JwtAuthenticationFilter(
                                jwtUtil,
                                userDetailsService,
                                jwtBlacklistService
                        ),
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    /**
     * 패스워드 암호화를 위한 인코더 빈 설정
     * BCrypt 알고리즘을 사용한 안전한 비밀번호 해싱
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        // 비밀번호를 안전하게 해시화하는 BCrypt 인코더 생성
        return new BCryptPasswordEncoder();
    }

    /**
     * 애플리케이션 전역 인증 관리자 설정
     * 사용자 인증 프로세스 구성
     */
    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {

        // 인증 관리자 빌더 생성
        AuthenticationManagerBuilder authManagerBuilder =
                http.getSharedObject(AuthenticationManagerBuilder.class);

        // 사용자 세부 정보 서비스와 패스워드 인코더 설정
        authManagerBuilder
                .userDetailsService(userDetailsService)  // 사용자 조회 로직
                .passwordEncoder(passwordEncoder());     // 비밀번호 검증 로직

        return authManagerBuilder.build();
    }
}