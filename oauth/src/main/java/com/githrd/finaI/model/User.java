package com.githrd.finaI.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicUpdate;


@Entity
@Table(name = "users")
@Getter
@DynamicUpdate // Entity update시, 원하는 데이터만 update하기 위함
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@SequenceGenerator(
        name = "SEQ_GENERATOR",
        sequenceName = "USERS_SEQ",
        allocationSize = 1
)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_GENERATOR")
    @Column(name = "user_id")
    private Long id;

    @Column(name = "username", nullable = false)
    private String username; // 로그인한 사용자의 이름

    @Column(name = "email", nullable = false)
    private String email; // 로그인한 사용자의 이메일

    @Column(name = "provider", nullable = false)
    private String provider; // 사용자가 로그인한 서비스: google, naver, kakao, prod)

    // 사용자의 이름이나 이메일을 업데이트하는 메소드
    public User updateUser(String username, String email) {
        this.username = username;
        this.email = email;

        return this;
    }
}
