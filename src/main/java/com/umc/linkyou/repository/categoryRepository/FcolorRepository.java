package com.umc.linkyou.repository.categoryRepository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.umc.linkyou.domain.folder.Fcolor;

public interface FcolorRepository extends JpaRepository<Fcolor, Long>, FcolorRepositoryCustom {}
