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

    private final JwtBlacklistRepository jwtBlacklistRepository;
    private final JwtUtil jwtUtil;

    @Autowired
    public JwtBlacklistService(JwtBlacklistRepository jwtBlacklistRepository, JwtUtil jwtUtil) {
        this.jwtBlacklistRepository = jwtBlacklistRepository;
        this.jwtUtil = jwtUtil;
    }

    public void blacklistToken(String token, LocalDateTime expirationTime, String username) {
        JwtBlacklist jwtBlacklist = new JwtBlacklist();
        jwtBlacklist.setToken(token);
        jwtBlacklist.setExpirationTime(expirationTime);
        jwtBlacklist.setUsername(username);
        jwtBlacklistRepository.save(jwtBlacklist);
    }

    public boolean isTokenBlacklisted(String currentToken) {
        String username = jwtUtil.getUsernameFromToken(currentToken);
        Optional<JwtBlacklist> blacklistedToken = jwtBlacklistRepository.findTopByUsernameOrderByExpirationTime(username);
        if (blacklistedToken.isEmpty()) {
            return false;
        }
        Instant instant = jwtUtil.getExpirationDateFromToken(currentToken).toInstant();
        LocalDateTime localDateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
        return blacklistedToken.get().getExpirationTime().isAfter(localDateTime.minusMinutes(60));
    }
}