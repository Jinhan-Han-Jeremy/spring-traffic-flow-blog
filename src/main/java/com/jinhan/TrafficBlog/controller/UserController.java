package com.jinhan.TrafficBlog.controller;

import com.jinhan.TrafficBlog.dto.SignUpUser;
import com.jinhan.TrafficBlog.entity.User;
import com.jinhan.TrafficBlog.entity.UserNotificationHistory;
import com.jinhan.TrafficBlog.jwt.JwtUtil;
import com.jinhan.TrafficBlog.service.CustomUserDetailsService;
import com.jinhan.TrafficBlog.service.JwtBlacklistService;

import com.jinhan.TrafficBlog.service.UserNotificationHistoryService;
import com.jinhan.TrafficBlog.service.UserService;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    // 사용자 인증 및 관리를 위한 의존성 주입
    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;
    private final JwtBlacklistService jwtBlacklistService;
    private final UserNotificationHistoryService userNotificationHistoryService;

    /**
     * 생성자를 통한 의존성 주입
     * 느슨한 결합과 테스트 용이성을 제공하는 의존성 주입 패턴
     */
    @Autowired
    public UserController(
            UserService userService,
            AuthenticationManager authenticationManager,
            JwtUtil jwtUtil,
            CustomUserDetailsService userDetailsService,
            JwtBlacklistService jwtBlacklistService,
            UserNotificationHistoryService userNotificationHistoryService
    ) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
        this.jwtBlacklistService = jwtBlacklistService;
        this.userNotificationHistoryService = userNotificationHistoryService;
    }

    /**
     * 전체 사용자 목록 조회 API
     * 관리자 권한이 필요한 엔드포인트
     *
     * @return 사용자 목록과 HTTP 200 OK 상태
     */
    @GetMapping("")
    public ResponseEntity<List<User>> getUserS() {
        return ResponseEntity.ok(userService.getUsers());
    }

    /**
     * 사용자 회원가입 API
     * 새로운 사용자 계정 생성
     *
     * @param signUpUser 회원가입 정보를 담은 DTO
     * @return 생성된 사용자 객체와 HTTP 200 OK 상태
     */
    @PostMapping("/signUp")
    public ResponseEntity<User> createUser(@RequestBody SignUpUser signUpUser) {
        User user = userService.createUser(signUpUser);
        return ResponseEntity.ok(user);
    }

    /**
     * 사용자 계정 삭제 API
     * 특정 사용자 계정을 삭제하는 엔드포인트
     *
     * @param userId 삭제할 사용자의 고유 식별자
     * @return HTTP 204 No Content 상태
     */
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(
            @Parameter(description = "삭제할 사용자 ID", required = true)
            @PathVariable Long userId
    ) {
        userService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 사용자 로그인 API
     * JWT 토큰 생성 및 쿠키 설정
     *
     * @param username 사용자 이름
     * @param password 비밀번호
     * @param response HTTP 응답 객체
     * @return 생성된 JWT 토큰
     * @throws AuthenticationException 인증 실패 시 예외
     */
    @PostMapping("/login")
    public String login(
            @RequestParam String username,
            @RequestParam String password,
            HttpServletResponse response
    ) throws AuthenticationException {
        // 사용자 인증 시도
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password)
        );

        // 사용자 상세 정보 조회
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        // JWT 토큰 생성
        String token = jwtUtil.generateToken(userDetails.getUsername());

        // HTTP Only 쿠키 설정
        Cookie cookie = new Cookie("jinhan_token", token);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(60 * 60); // 1시간 유효

        response.addCookie(cookie);
        return token;
    }

    /**
     * 로그아웃 API
     * 쿠키 만료를 통한 로그아웃 처리
     *
     * @param response HTTP 응답 객체
     */
    @PostMapping("/logout")
    public void logout(HttpServletResponse response) {
        // 토큰 쿠키 삭제
        Cookie cookie = new Cookie("jinhan_token", null);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0); // 쿠키 즉시 삭제
        response.addCookie(cookie);
    }

    /**
     * 전체 기기/세션 로그아웃 API
     * 토큰 블랙리스트 처리 및 모든 세션 무효화
     *
     * @param requestToken 요청 파라미터로 받은 토큰
     * @param cookieToken 쿠키에서 받은 토큰
     * @param request HTTP 요청 객체
     * @param response HTTP 응답 객체
     */
    @PostMapping("/logout/all")
    public void logout(
            @RequestParam(required = false) String requestToken,
            @CookieValue(value = "jinhan_token", required = false) String cookieToken,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        // 토큰 추출 로직 (요청 파라미터, 쿠키, Authorization 헤더)
        String token = extractToken(requestToken, cookieToken, request);

        // 토큰 블랙리스트 처리
        Instant instant = new Date().toInstant();
        LocalDateTime expirationTime = instant.atZone(ZoneId.systemDefault()).toLocalDateTime();
        String username = jwtUtil.getUsernameFromToken(token);
        jwtBlacklistService.blacklistToken(token, expirationTime, username);

        // 쿠키 삭제
        Cookie cookie = new Cookie("jinhan_token", null);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    /**
     * JWT 토큰 유효성 검증 API
     *
     * @param token 검증할 JWT 토큰
     * @throws ResponseStatusException 토큰 유효하지 않을 경우
     */
    @PostMapping("/token/validation")
    @ResponseStatus(HttpStatus.OK)
    public void jwtValidate(@RequestParam String token) {
        if (!jwtUtil.validateToken(token)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "유효하지 않은 토큰");
        }
    }

    /**
     * 알림 히스토리 읽음 처리 API
     *
     * @param historyId 읽음 처리할 알림 히스토리 ID
     */
    @PostMapping("/history")
    @ResponseStatus(HttpStatus.OK)
    public void readHistory(@RequestParam String historyId) {
        userNotificationHistoryService.readNotification(historyId);
    }

    /**
     * 알림 히스토리 목록 조회 API
     *
     * @return 알림 히스토리 목록과 HTTP 200 OK 상태
     */
    @GetMapping("/history")
    public ResponseEntity<List<UserNotificationHistory>> getHistoryList() {
        return ResponseEntity.ok(userNotificationHistoryService.getNotificationList());
    }

    // 토큰 추출 헬퍼 메서드 (실제 구현 필요)
    private String extractToken(String requestToken, String cookieToken, HttpServletRequest request) {
        if (requestToken != null) {
            return requestToken;
        } else if (cookieToken != null) {
            return cookieToken;
        }

        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        throw new IllegalArgumentException("토큰을 찾을 수 없습니다.");
    }
}