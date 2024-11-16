package com.jinhan.TrafficBlog.entity;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "adViewHistory")
@Getter
@Setter
public class AdViewHistory extends BaseEntity{
    @Id
    private String id;

    private Long adId;

    private String username;

    private String clientIp;

    private Boolean isTrueView = false;
}
