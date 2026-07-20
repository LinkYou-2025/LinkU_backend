package com.umc.linkyou.domain.classification;

import jakarta.persistence.*;
import lombok.*;

// 관심사 마스터(카탈로그) 엔티티. Users 와는 UsersInterest 조인 엔티티를 통해 다대다로 연결된다.
@Entity
@Table(name = "interests")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Interests {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, unique = true, length = 50)
    private String name;

    public static Interests of(String name) {
        return Interests.builder().name(name).build();
    }
}
