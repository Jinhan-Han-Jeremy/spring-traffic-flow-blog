package com.jinhan.TrafficBlog.service;

import com.jinhan.TrafficBlog.dto.WriteNotice;
import com.jinhan.TrafficBlog.entity.Notice;
import com.jinhan.TrafficBlog.entity.User;
import com.jinhan.TrafficBlog.entity.UserNotificationHistory;
import com.jinhan.TrafficBlog.exception.ResourceNotFoundException;
import com.jinhan.TrafficBlog.repository.jpa.NoticeRepository;
import com.jinhan.TrafficBlog.repository.jpa.UserRepository;
import com.jinhan.TrafficBlog.repository.mongo.UserNotificationHistoryRepository;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
@Service
public class NoticeService {
    // 공지사항 데이터 접근을 위한 JPA 리포지토리
    private final NoticeRepository noticeRepository;

    // 사용자 데이터 접근을 위한 JPA 리포지토리
    private final UserRepository userRepository;

    // 유효성 검사를 위한 서비스
    private final ValidationService validationService;

    // 사용자 알림 이력 관리를 위한 MongoDB 리포지토리
    private final UserNotificationHistoryRepository userNotificationHistoryRepository;

    public NoticeService(NoticeRepository noticeRepository,
                         UserRepository userRepository,
                         ValidationService validationService,
                         UserNotificationHistoryRepository userNotificationHistoryRepository) {
        this.noticeRepository = noticeRepository;
        this.userRepository = userRepository;
        this.validationService = validationService;
        this.userNotificationHistoryRepository = userNotificationHistoryRepository;
    }

    /**
     * 새로운 공지사항을 작성하는 메소드
     * @param dto 공지사항 작성에 필요한 데이터를 담은 DTO
     * @return 저장된 공지사항 엔티티
     */
    public Notice writeNotice(WriteNotice dto) {
        // 현재 로그인한 사용자 정보 조회
        User author = validationService.currentUser(userRepository);

        // 새로운 공지사항 엔티티 생성 및 데이터 설정
        Notice notice = new Notice();
        notice.setTitle(dto.getTitle());
        notice.setContent(dto.getContent());
        notice.setAuthor(author);

        // 공지사항 저장 및 반환
        noticeRepository.save(notice);
        return notice;
    }

    /**
     * 공지사항을 조회하고 사용자의 알림 이력을 기록하는 메소드
     * @param noticeId 조회할 공지사항 ID
     * @return 조회된 공지사항 엔티티
     */
    public Notice getNotice(Long noticeId) {
        // 현재 로그인한 사용자 정보 조회
        User user = validationService.currentUser(userRepository);

        // 공지사항 조회
        Optional<Notice> notice = noticeRepository.findById(noticeId);

        // 사용자 알림 이력 생성 및 설정
        UserNotificationHistory userNotificationHistory = new UserNotificationHistory();
        userNotificationHistory.setTitle("공지사항이 작성되었습니다.");
        userNotificationHistory.setContent(notice.get().getTitle());
        userNotificationHistory.setUserId(user.getId());
        userNotificationHistory.setIsRead(true);
        userNotificationHistory.setNoticeId(noticeId);
        userNotificationHistory.setCreatedDate(notice.get().getCreatedDate());
        userNotificationHistory.setUpdatedDate(LocalDateTime.now());

        // 알림 이력 저장
        userNotificationHistoryRepository.save(userNotificationHistory);

        return notice.get();
    }
}
