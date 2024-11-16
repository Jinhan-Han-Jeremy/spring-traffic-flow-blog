package com.jinhan.TrafficBlog.entity;

import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "userNotificationHistory")
@Getter
@Setter
public class UserNotificationHistory extends BaseEntity {
    @Id
    private String id;

    private String title;

    private String content;

    private Long noticeId;

    private Long userId;

    private Boolean isRead = false;

}