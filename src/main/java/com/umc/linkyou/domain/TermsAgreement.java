package com.umc.linkyou.domain;

import com.umc.linkyou.domain.common.BaseEntity;
import com.umc.linkyou.domain.enums.TermsType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "terms_agreements",
        uniqueConstraints={
                @UniqueConstraint(
                        name = "uk_terms_agreements_user_type",
                        columnNames={"user_id", "terms_type"}
                )
        }
)
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class TermsAgreement extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "terms_agreement_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Users user;

    @Enumerated(EnumType.STRING)
    @Column(name = "terms_type", length = 50, nullable = false)
    private TermsType termsType; // "TERMS_OF_USE", "PRIVACY_POLICY", "MARKETING"

    @Column(name = "is_required", nullable = false)
    @Builder.Default
    private Boolean isRequired = false; // true=필수, false=선택

    @Column(name = "terms_version", length = 10, nullable = false)
    private String termsVersion; // "v1.0"

    @Column(name = "agreed_at", nullable = false)
    private LocalDateTime agreedAt;

    @Column(name = "is_agreed", nullable = false)
    @NotNull
    @Builder.Default
    private Boolean isAgreed = true;

    public void updateAgreement(boolean isAgreed, LocalDateTime agreedAt) {
        this.isAgreed = isAgreed;
        this.agreedAt = agreedAt;
    }
}
