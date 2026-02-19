package com.umc.linkyou.domain;

import com.umc.linkyou.domain.common.BaseEntity;
import com.umc.linkyou.domain.enums.TermsType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "terms_agreements")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class TermsAgreement extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "terms_agreement_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    @Enumerated(EnumType.STRING)
    @Column(name = "terms_type", length = 50, nullable = false)
    private TermsType termsType; // "TERMS_OF_USE", "PRIVACY_POLICY", "MARKETING"

    @Column(name = "is_required", nullable = false)
    private Boolean isRequired = false; // true=필수, false=선택

    @Column(name = "terms_version", length = 10, nullable = false)
    private String termsVersion; // "v1.0"

    @Column(name = "agreed_at", nullable = false)
    private LocalDateTime agreedAt;

    @Column(name = "is_agreed", nullable = false)
    @NotNull
    private Boolean isAgreed = true;
}
