package com.jinhan.TrafficBlog.service;

import com.jinhan.TrafficBlog.entity.User;
import com.jinhan.TrafficBlog.exception.ResourceNotFoundException;
import com.jinhan.TrafficBlog.repository.jpa.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;


@Service
public class ValidationService {

    // 엔티티 검증 및 조회 메서드 (유연한 제네릭 활용)
    public <T> T validate(Long id, RepositoryInterface<T> repository, String entityName) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(entityName + " not found"));
    }

    // 현재 인증된 사용자 검증 및 조회
    public User currentUser(UserRepository userRepository) {
        // 현재 인증된 사용자의 정보를 저장
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // 현재 사용자의 정보를 불러옴
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Author not found"));
    }

    //두개의 시간 변수들의 시간 간격 분 단위로 체크 로직
    public Duration isDifferenceBetweenTimes(LocalDateTime localDateTime) {
        LocalDateTime dateAsLocalDateTime = new Date().toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();

        return Duration.between(localDateTime, dateAsLocalDateTime);
    }

}