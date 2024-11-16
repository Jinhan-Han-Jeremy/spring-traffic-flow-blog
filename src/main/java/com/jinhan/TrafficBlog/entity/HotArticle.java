package com.jinhan.TrafficBlog.entity;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
public class HotArticle extends BaseEntity implements Serializable {
    private Long id;

    private String title;

    private String content;

    private String authorName;

    private Long viewCount = 0L;
}