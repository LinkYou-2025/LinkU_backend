package com.umc.linkyou.repository.classification;

import com.umc.linkyou.domain.classification.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository("classificationJobRepository")
public interface JobRepository extends JpaRepository<Job, Long>  {
}
