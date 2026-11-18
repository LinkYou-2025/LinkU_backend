package com.umc.linkyou.domain;

import jakarta.persistence.*;

import com.umc.linkyou.domain.common.BaseEntity;

import lombok.*;

@Entity
@Table(name = "linku_search_histories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class LinkuSearchHistory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "linku_search_history_id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "keyword", nullable = false, length = 200)
    private String keyword;

    public static LinkuSearchHistory of(Long userId, String keyword) {
        return LinkuSearchHistory.builder().userId(userId).keyword(keyword).build();
    }
}
