package com.jinhan.TrafficBlog.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Comment extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 대용량 데이터를 저장할 때 사용.
    @Lob
    @Column(nullable = false)
    private String content;

    // 여러 개의 엔티티가 하나의 `User` 엔티티와 관계를 맺음
    @ManyToOne
    @JoinColumn(foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))// 외래 키 제약 조건을 사용하지 않음.
    private User author;

    // 여러 개의 엔티티가 하나의 `Article` 엔티티와 관계를 맺음
    @ManyToOne
    @JsonIgnore //JSON 직렬화 및 응답에서 제외됨.
    @JoinColumn(foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))// 외래 키 제약 조건을 사용하지 않음.
    private Article article;

    @Column(nullable = false)
    private Boolean isDeleted = false;


}