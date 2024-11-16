package com.jinhan.TrafficBlog.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity  // JPA 엔티티로 설정하여 데이터베이스 테이블과 매핑
@Getter  // Lombok을 사용해 모든 필드의 getter 메서드를 자동으로 생성
@Setter  // Lombok을 사용해 모든 필드의 setter 메서드를 자동으로 생성
@NoArgsConstructor  // 기본 생성자를 자동으로 생성
public class Advertisement extends BaseEntity implements Serializable {

    @Id  // 데이터베이스에서 기본 키로 설정
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    // ID 자동 생성, 데이터베이스의 IDENTITY 전략 사용
    private Long id;

    @Column(nullable = false)  // title 필드는 NULL 값을 허용하지 않음
    private String title;

    @Lob  // content 필드를 대용량 텍스트로 설정 (CLOB으로 매핑)
    @Column(nullable = false)  // content 필드는 NULL 값을 허용하지 않음
    private String content = "";

    @Column(nullable = false)  // isDeleted 필드는 NULL 값을 허용하지 않음
    private Boolean isDeleted = false;  // 기본값으로 false 설정

    @Column(nullable = false)  // isVisible 필드는 NULL 값을 허용하지 않음
    private Boolean isVisible = true;  // 기본값으로 true 설정

    @Column(insertable = true)  // insert 시 해당 필드를 포함
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")  // 날짜 형식을 "yyyy-MM-dd HH:mm:ss" 형식으로 설정
    private LocalDateTime startDate;

    @Column(insertable = true)  // insert 시 해당 필드를 포함
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")  // 날짜 형식을 "yyyy-MM-dd HH:mm:ss" 형식으로 설정
    private LocalDateTime endDate;

    @Column(nullable = false)  // viewCount 필드는 NULL 값을 허용하지 않음
    private Integer viewCount = 0;  // 기본값으로 0 설정

    @Column(nullable = false)  // clickCount 필드는 NULL 값을 허용하지 않음
    private Integer clickCount = 0;  // 기본값으로 0 설정

}