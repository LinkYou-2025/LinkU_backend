package com.umc.linkyou.repository.dto;

/** 유저별 키워드 빈도 집계 결과. UserProfileKeyword의 weight 원본 값으로 쓰인다. */
public record UserKeywordWeightRow(Long keywordId, Long count) {}
