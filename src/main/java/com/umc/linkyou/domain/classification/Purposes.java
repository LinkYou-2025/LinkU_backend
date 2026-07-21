package com.umc.linkyou.domain.classification;

import jakarta.persistence.*;
import lombok.*;

// 사용 목적 마스터(카탈로그) 엔티티. Users 와는 UsersPurpose 조인 엔티티를 통해 다대다로 연결된다.
@Entity
@Table(name = "purposes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Purposes {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, unique = true, length = 50)
    private String name;

    @Builder(access = AccessLevel.PRIVATE)
    private Purposes(String name) {
        this.name = name;
    }

    public static Purposes of(String name) {
        return Purposes.builder().name(name).build();
    }
}
