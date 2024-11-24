package com.jinhan.TrafficBlog.service;

import com.jinhan.TrafficBlog.entity.JwtBlacklist;
import com.jinhan.TrafficBlog.jwt.JwtUtil;
import com.jinhan.TrafficBlog.repository.jpa.JwtBlacklistRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
@Service
public class JwtBlacklistService {

    // 블랙리스트 토큰 데이터 접근을 위한 리포지토리
    private final JwtBlacklistRepository jwtBlacklistRepository;

    // JWT 토큰 처리를 위한 유틸리티 클래스
    private final JwtUtil jwtUtil;

    @Autowired
    public JwtBlacklistService(JwtBlacklistRepository jwtBlacklistRepository, JwtUtil jwtUtil) {
        this.jwtBlacklistRepository = jwtBlacklistRepository;
        this.jwtUtil = jwtUtil;
    }

    /**
     * JWT 토큰을 블랙리스트에 추가하는 메소드
     * 주로 로그아웃 시 사용됨
     *
     * @param token 블랙리스트에 추가할 JWT 토큰
     * @param expirationTime 토큰 만료 시간
     * @param username 사용자명
     */
    public void blacklistToken(String token, LocalDateTime expirationTime, String username) {
        // 블랙리스트 엔티티 생성 및 데이터 설정
        JwtBlacklist jwtBlacklist = new JwtBlacklist();
        jwtBlacklist.setToken(token);
        jwtBlacklist.setExpirationTime(expirationTime);
        jwtBlacklist.setUsername(username);

        // 블랙리스트 저장
        jwtBlacklistRepository.save(jwtBlacklist);
    }

    /**
     * 주어진 토큰이 블랙리스트에 있는지 확인하는 메소드
     *
     * @param currentToken 검사할 JWT 토큰
     * @return 블랙리스트 포함 여부 (true: 블랙리스트에 있음, false: 유효한 토큰)
     */
    public boolean isTokenBlacklisted(String currentToken) {
        // 토큰에서 사용자명 추출
        String username = jwtUtil.getUsernameFromToken(currentToken);

        // 해당 사용자의 가장 최근 블랙리스트 토큰 조회
        Optional<JwtBlacklist> blacklistedToken =
                jwtBlacklistRepository.findTopByUsernameOrderByExpirationTime(username);

        // 블랙리스트 토큰이 없으면 유효한 토큰으로 판단
        if (blacklistedToken.isEmpty()) {
            return false;
        }

        // 현재 토큰의 만료 시간을 LocalDateTime으로 변환
        Instant instant = jwtUtil.getExpirationDateFromToken(currentToken).toInstant();
        LocalDateTime localDateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());

        // 블랙리스트 토큰의 만료 시간과 비교 (60분의 여유 시간 고려)
        return blacklistedToken.get().getExpirationTime().isAfter(localDateTime.minusMinutes(60));
    }
}