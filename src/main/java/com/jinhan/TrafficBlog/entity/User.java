package com.jinhan.TrafficBlog.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String username;

    @JsonIgnore
    @Column(nullable = false)
    private String password;

    @JsonIgnore
    @Email(message = "이메일 형식으로 입력")
    @Column(nullable = false)
    private String email;

    private LocalDateTime lastLogin;

    @Column(columnDefinition = "json")
    @Convert(converter = DeviceListConverter.class)
    private List<Device> deviceList = new ArrayList<>();

    @CreatedDate
    @Column(insertable = true)
    private LocalDateTime createdDate;

    @LastModifiedDate
    private LocalDateTime updatedDate;

    // 엔티티가 처음 데이터베이스에 저장되기 전에 호출됨. 이 메서드를 통해 생성 날짜를 자동으로 설정.
    @PrePersist
    protected void onCreate() {
        this.createdDate = LocalDateTime.now();
        if (deviceList == null) {
            deviceList = new ArrayList<>(); // 기본값을 빈 배열로 설정
        }
    }

    // 엔티티가 수정되기 전에 호출됨. 이 메서드를 통해 수정 날짜를 자동으로 설정.
    @PreUpdate
    protected void onUpdate() {
        this.updatedDate = LocalDateTime.now();
    }

    //사용자 권한을 통해 접근 제어를 반환
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return null;
    }

    //만약 계정이 만료되었다면(예: 구독), 이 메서드는 false를 반환
    @Override
    public boolean isAccountNonExpired() {
        return false;
    }

    //계정이 잠겼을 경우(예: 너무 많은 로그인 시도 실패), 이 메서드는 false를 반환하여 사용자 로그인을 차단
    @Override
    public boolean isAccountNonLocked() {
        return false;
    }

    //자격 증명(비밀번호 등)이 만료되지 않았는지 여부를 반환
    @Override
    public boolean isCredentialsNonExpired() {
        return false;
    }

    //계정이 비활성화된 경우, 이 메서드는 false를 반환
    @Override
    public boolean isEnabled() {
        return true;
    }
}