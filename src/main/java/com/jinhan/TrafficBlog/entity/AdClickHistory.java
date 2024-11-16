package com.jinhan.TrafficBlog.entity;

import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Document(collection = "adClickHistory")
@Getter
@Setter
public class AdClickHistory extends BaseEntity{
    @Id
    private String id;

    private Long adId;

    private String username;

    private String clientIp;
}
