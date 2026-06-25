package com.umc.linkyou.domain.mapping;

import com.umc.linkyou.domain.classification.Job;
import com.umc.linkyou.domain.classification.Situation;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "situation_jobs")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SituationJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "situation_job_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "situation_id", nullable = false)
    private Situation situation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;
}
