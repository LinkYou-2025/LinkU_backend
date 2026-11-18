package com.umc.linkyou.repository.classification;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.umc.linkyou.domain.classification.Job;

@Repository("classificationJobRepository")
public interface JobRepository extends JpaRepository<Job, Long> {}
