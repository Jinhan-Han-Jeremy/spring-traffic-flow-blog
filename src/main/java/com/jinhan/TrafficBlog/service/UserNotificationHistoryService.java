package com.jinhan.TrafficBlog.service;

import com.jinhan.TrafficBlog.entity.*;
import com.jinhan.TrafficBlog.exception.ResourceNotFoundException;
import com.jinhan.TrafficBlog.repository.jpa.NoticeRepository;
import com.jinhan.TrafficBlog.repository.jpa.UserRepository;
import com.jinhan.TrafficBlog.repository.mongo.UserNotificationHistoryRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

@Service
public class UserNotificationHistoryService {
    // 공지사항 데이터 접근을 위한 JPA 리포지토리
    private final NoticeRepository noticeRepository;

    // 사용자 데이터 접근을 위한 JPA 리포지토리
    private final UserRepository userRepository;

    // 사용자 알림 이력 관리를 위한 MongoDB 리포지토리
    private final UserNotificationHistoryRepository userNotificationHistoryRepository;

    /**
     * 생성자를 통한 의존성 주입
     */
    public UserNotificationHistoryService(UserNotificationHistoryRepository userNotificationHistoryRepository,
                                          NoticeRepository noticeRepository,
                                          UserRepository userRepository) {
        this.userNotificationHistoryRepository = userNotificationHistoryRepository;
        this.noticeRepository = noticeRepository;
        this.userRepository = userRepository;
    }

    /**
     * 새 게시글 작성 시 알림 생성
     * @param article 작성된 게시글
     * @param userId 알림을 받을 사용자 ID
     */
    public void insertArticleNotification(Article article, Long userId) {
        UserNotificationHistory history = new UserNotificationHistory();
        history.setTitle("글이 작성되었습니다.");
        history.setContent(article.getTitle());
        history.setUserId(userId);
        userNotificationHistoryRepository.save(history);
    }

    /**
     * 새 댓글 작성 시 알림 생성
     * @param comment 작성된 댓글
     * @param userId 알림을 받을 사용자 ID
     */
    public void insertCommentNotification(Comment comment, Long userId) {
        UserNotificationHistory history = new UserNotificationHistory();
        history.setTitle("댓글이 작성되었습니다.");
        history.setContent(comment.getContent());
        history.setUserId(userId);
        userNotificationHistoryRepository.save(history);
    }

    /**
     * 알림을 읽음 상태로 변경
     * @param id 알림 이력 ID
     */
    public void readNotification(String id) {
        Optional<UserNotificationHistory> history = userNotificationHistoryRepository.findById(id);
        if (history.isEmpty()) {
            return;
        }
        // 알림을 읽음 상태로 변경하고 업데이트 시간 기록
        history.get().setIsRead(true);
        history.get().setUpdatedDate(LocalDateTime.now());
        userNotificationHistoryRepository.save(history.get());
    }

    /**
     * 현재 사용자의 알림 목록 조회
     * - 최근 7주 이내의 알림만 표시
     * - 공지사항 알림은 중복 제거하여 표시
     * @return 알림 목록
     */
    public List<UserNotificationHistory> getNotificationList() {
        // 현재 로그인한 사용자 정보 조회
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        Optional<User> user = userRepository.findByUsername(userDetails.getUsername());
        if (user.isEmpty()) {
            throw new ResourceNotFoundException("author not found");
        }

        // 7주 전 날짜 계산
        LocalDateTime weekDate = LocalDateTime.now().minusWeeks(7);

        // 7주 이내의 알림 목록 조회
        List<UserNotificationHistory> userNotificationHistoryList
                = userNotificationHistoryRepository.findByUserIdAndCreatedDateAfter(
                user.get().getId(), weekDate);

        // 7주 이내의 공지사항 목록 조회
        List<Notice> notices = noticeRepository.findByCreatedDate(weekDate);

        // 최종 결과 목록 생성
        List<UserNotificationHistory> results = new ArrayList<>();
        // 공지사항 알림 중복 제거를 위한 HashMap
        HashMap<Long, UserNotificationHistory> hashMap = new HashMap<>();

        // 기존 알림 목록 처리
        for (UserNotificationHistory history : userNotificationHistoryList) {
            if (history.getNoticeId() != null) {
                // 공지사항 알림은 HashMap에 저장하여 중복 제거
                hashMap.put(history.getNoticeId(), history);
            } else {
                // 일반 알림은 바로 결과 목록에 추가
                results.add(history);
            }
        }

        // 공지사항 알림 처리
        for (Notice notice : notices) {
            UserNotificationHistory history = hashMap.get(notice.getId());
            if (history != null) {
                // 기존 공지사항 알림이 있으면 그대로 사용
                results.add(history);
            } else {
                // 새로운 공지사항 알림 생성
                history = new UserNotificationHistory();
                history.setTitle("공지사항이 작성되었습니다.");
                history.setContent(notice.getTitle());
                history.setUserId(user.get().getId());
                history.setIsRead(false);
                history.setNoticeId(notice.getId());
                history.setCreatedDate(notice.getCreatedDate());
                history.setUpdatedDate(null);
                results.add(history);
            }
        }

        return results;
    }
}