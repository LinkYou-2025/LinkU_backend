package com.umc.linkyou.service;

import com.umc.linkyou.apiPayload.code.status.ErrorStatus;
import com.umc.linkyou.apiPayload.code.status.user.UserErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.converter.AiArticleConverter;
import com.umc.linkyou.domain.*;
import com.umc.linkyou.domain.classification.*;
import com.umc.linkyou.domain.Linku;
import com.umc.linkyou.domain.mapping.UsersLinku;
import com.umc.linkyou.gemini.dto.SummaryResultDTO;
import com.umc.linkyou.gemini.service.GeminiLinkuService;
import com.umc.linkyou.utils.parser.LinkToImageService;
import com.umc.linkyou.repository.*;
import com.umc.linkyou.repository.aiArticleRepository.AiArticleRepository;
import com.umc.linkyou.repository.linkuRepository.LinkuRepository;
import com.umc.linkyou.repository.classification.CategoryRepository;
import com.umc.linkyou.repository.classification.SituationRepository;
import com.umc.linkyou.repository.mapping.SituationJobRepository;
import com.umc.linkyou.repository.mapping.UsersLinkuRepository;
import com.umc.linkyou.repository.userRepository.UserRepository;
import com.umc.linkyou.web.dto.AiArticleResponsetDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Slf4j
@Service
@RequiredArgsConstructor
public class AiArticleServiceImpl implements AiArticleService {

    private final UserRepository userRepository;
    private final LinkuRepository linkuRepository;
    private final SituationJobRepository situationJobRepository;
    private final EmotionRepository emotionRepository;
    private final CategoryRepository categoryRepository;
    private final SituationRepository situationRepository;
    private final AiArticleRepository aiArticleRepository;
    private final UsersLinkuRepository usersLinkuRepository;
    private final LinkToImageService linkToImageService;
    private final GeminiLinkuService geminiLinkuService;

    /**
     * 링크 생성
     */
    @Override
    @Transactional
    public AiArticleResponsetDTO.AiArticleResultDTO saveAiArticle(Long linkuId, Long userId) {
        // 1. 기본 정보 및 생성 시점 매핑 데이터(UsersLinku) 조회
        Linku linku = linkuRepository.findById(linkuId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._BAD_REQUEST));
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(UserErrorStatus._USER_NOT_FOUND));
        UsersLinku usersLinku = usersLinkuRepository.findByUserAndLinku(user, linku)
                .orElseThrow(() -> new GeneralException(ErrorStatus._USER_LINKU_NOT_FOUND));

        // 2. Gemini 호출 (객관적 요약, 제목, 카테고리 추출)
        SummaryResultDTO result = geminiLinkuService.getFullAnalysis(linku.getLinku());

        // 3. 분석 결과 기반 엔티티 확정
        Category selectedCategory = categoryRepository.findById(result.getCategoryId())
                .orElseThrow(() -> new GeneralException(ErrorStatus._CATEGORY_NOT_FOUND));

        // [수정 포인트] 상황은 시스템 기본값, 감정은 유저가 링크 생성 시 선택했던 값을 활용
        Situation defaultSituation = situationRepository.findById(1L)
                .orElseThrow(() -> new GeneralException(ErrorStatus._SITUATION_NOT_FOUND));
        Emotion userSelectedEmotion = usersLinku.getEmotion();

        // 4. 이미지 정보 가져오기
        String imageUrl = linkToImageService.getRelatedImageFromUrl(linku.getLinku(), linku.getTitle());

        // 5. AiArticle 저장 또는 업데이트 (Dirty Checking 활용)
        AiArticle article = aiArticleRepository.findByLinku(linku)
                .map(existing -> {
                    existing.setTitle(result.getTitle());
                    existing.setSummary(result.getSummary());
                    existing.setAiCategoryId(selectedCategory.getCategoryId());
                    existing.setAiFeelingId(userSelectedEmotion.getEmotionId()); // 생성 시점 감정 반영
                    existing.setImgUrl(imageUrl);
                    return existing;
                })
                .orElseGet(() -> aiArticleRepository.save(
                        AiArticleConverter.toEntity(result, defaultSituation, userSelectedEmotion, selectedCategory, linku, imageUrl)
                ));

        // 6. 연관관계 및 상태 업데이트
        if (linku.getAiArticle() == null || !linku.getAiArticle().equals(article)) {
            linku.setAiArticle(article);
        }
        usersLinku.setAiExist(true);

        // 7. DTO 반환
        return AiArticleConverter.toDto(article, linku, usersLinku, defaultSituation, userSelectedEmotion, selectedCategory);
    }

    /**
     * 존재 여부 + title 검증 후 생성 or 조회
     */
    @Override
    @Transactional
    public AiArticleResponsetDTO.AiArticleResultDTO saveOrGetAiArticle(Long linkuId, Long userId) {
        Linku linku = linkuRepository.findById(linkuId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._BAD_REQUEST));

        AiArticle aiArticle = aiArticleRepository.findByLinku(linku).orElse(null);

        if (aiArticle == null || aiArticle.getTitle() == null || aiArticle.getTitle().isBlank()) {
            return saveAiArticle(linkuId, userId); // 새로 생성
        } else {
            return showAiArticle(linkuId, userId); // 기존 반환
        }
    }

    /**
     * ai_article 단순조회 (user 개별 메모/감정 포함)
     */
    public AiArticleResponsetDTO.AiArticleResultDTO showAiArticle(Long linkuId, Long userId) {
        Linku linku = linkuRepository.findById(linkuId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._BAD_REQUEST));
        AiArticle article = aiArticleRepository.findByLinku(linku)
                .orElseThrow(() -> new GeneralException(ErrorStatus._AI_ARTICLE_NOT_FOUND));
        // 유저 개별 정보(memo, 감정 등)는 users_linku에서 별도 조회
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(UserErrorStatus._USER_NOT_FOUND));
        UsersLinku usersLinku = usersLinkuRepository.findByUserAndLinku(user, linku)
                .orElse(null);

        // UsersLinku가 없거나 isAiExist가 false인 경우 false로 처리
        if (usersLinku == null || usersLinku.getAiExist() == null || !usersLinku.getAiExist()) {
            // UsersLinku가 없으면 false로 처리하여 반환
            // usersLinku가 null이면 null로 전달하고, Converter에서 null 처리를 하도록 함
        }

        Situation situation = article.getSituation();
        Emotion emotion = null;
        if (article.getAiFeelingId() != null)
            emotion = emotionRepository.findById(article.getAiFeelingId()).orElse(null);
        Category category = null;
        if (article.getAiCategoryId() != null)
            category = categoryRepository.findById(article.getAiCategoryId()).orElse(null);

        return AiArticleConverter.toDto(
                article,
                linku,
                usersLinku,
                situation,
                emotion,
                category


        );
    }
}
