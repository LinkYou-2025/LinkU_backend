package com.umc.linkyou.service;

import com.umc.linkyou.apiPayload.code.status.aiarticle.AiArticleErrorStatus;
import com.umc.linkyou.apiPayload.code.status.ErrorStatus;
import com.umc.linkyou.apiPayload.code.status.linku.LinkuErrorStatus;
import com.umc.linkyou.apiPayload.code.status.user.UserErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.converter.AiArticleConverter;
import com.umc.linkyou.converter.LinkuConverter;
import com.umc.linkyou.domain.AiArticle;
import com.umc.linkyou.domain.Linku;
import com.umc.linkyou.domain.enums.AiArticleStatus;
import com.umc.linkyou.domain.mapping.UsersLinku;

import com.umc.linkyou.repository.aiArticleRepository.AiArticleRepository;
import com.umc.linkyou.repository.linkuRepository.LinkuRepository;
import com.umc.linkyou.repository.UserLinkuRepository.UsersLinkuRepository;
import com.umc.linkyou.repository.userRepository.UserRepository;
import com.umc.linkyou.web.dto.AiArticleResponseDTO;
import com.umc.linkyou.web.dto.linku.LinkuResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiArticleService {

    private final UserRepository userRepository;
    private final LinkuRepository linkuRepository;
    private final AiArticleRepository aiArticleRepository;
    private final UsersLinkuRepository usersLinkuRepository;
    private final AiArticleGenerationWorker aiArticleGenerationWorker;

    /**
     * GET /aiarticle/{linkuid} — 순수 조회. 생성은 하지 않는다.
     * 레코드가 아예 없으면 404(_AI_ARTICLE_NOT_FOUND)를 던지고, 프론트는 이 경우에만 POST로 생성을 요청한다.
     * 레코드가 있으면 PENDING/DONE/FAILED 중 현재 상태를 그대로 실어 보낸다 — 프론트는 PENDING이면 계속 폴링한다.
     */
    @Transactional
    public AiArticleResponseDTO.AiArticleResultDTO getAiArticle(Long linkuId, Long userId) {
        Linku linku = linkuRepository.findById(linkuId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._BAD_REQUEST));
        userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(UserErrorStatus._USER_NOT_FOUND));

        // 요청 유저가 이 linku를 저장한 적이 없으면(=UsersLinku 없음) 소유권이 없는 것이므로 예외를 던진다.
        // (다른 유저가 먼저 만든 AiArticle의 linkuId를 알기만 하면 조회되는 것을 막기 위함)
        List<UsersLinku> usersLinkus = usersLinkuRepository.findByUser_IdAndLinku_LinkuId(userId, linkuId);
        UsersLinku usersLinku = usersLinkus.stream()
                .max(Comparator.comparing(UsersLinku::getCreatedAt))
                .orElseThrow(() -> new GeneralException(LinkuErrorStatus._USER_LINKU_NOT_FOUND));

        AiArticle article = aiArticleRepository.findByLinku(linku)
                .orElseThrow(() -> new GeneralException(AiArticleErrorStatus._AI_ARTICLE_NOT_FOUND));

        // 다른 유저가 먼저 만들어둔 요약이라도, 본인이 직접 조회한 시점부터는 본인 소유 UsersLinku에도
        // "AI 요약 있음"으로 남긴다. 단, PENDING/FAILED는 아직 실제로 볼 수 있는 요약이 없으므로 표시하지 않는다.
        if (article.getStatus() == AiArticleStatus.DONE) {
            usersLinkus.forEach(ul -> ul.markAiExist(true));
        }

        return AiArticleConverter.toDto(article, linku, usersLinku, resolveTags(linku));
    }

    /**
     * POST /aiarticle/{linkuid} — 생성 트리거만 한다. 크롤링/Gemini 호출은 여기서 기다리지 않고
     * {@link AiArticleGenerationWorker}에 넘겨 비동기로 처리한다. 응답은 항상 status=PENDING으로 즉시 내려간다.
     * 이미 DONE이면 409(_DUPLICATE_AI_ARTICLE), 이미 PENDING이면 409(_AI_ARTICLE_GENERATING) — 두 경우 모두
     * 프론트는 GET으로 전환해 조회/폴링해야 한다. FAILED였다면 재시도로 간주하고 다시 PENDING으로 돌린다.
     */
    @Transactional
    public AiArticleResponseDTO.AiArticleResultDTO createAiArticle(Long linkuId, Long userId) {
        Linku linku = linkuRepository.findById(linkuId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._BAD_REQUEST));
        userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(UserErrorStatus._USER_NOT_FOUND));

        // 동일 (user, linku) 조합으로 저장된 UsersLinku가 여러 건일 수 있다. 응답에는 최신 1건을
        // 대표로 쓰지만, "AI 요약 있음" 표시는 같은 링크를 여러 번 저장한 모든 건에 동일하게 남아야 한다.
        List<UsersLinku> usersLinkus = usersLinkuRepository.findByUser_IdAndLinku_LinkuId(userId, linkuId);
        UsersLinku usersLinku = usersLinkus.stream()
                .max(Comparator.comparing(UsersLinku::getCreatedAt))
                .orElseThrow(() -> new GeneralException(LinkuErrorStatus._USER_LINKU_NOT_FOUND));

        AiArticle article = aiArticleRepository.findByLinku(linku).orElse(null);

        if (article == null) {
            article = aiArticleRepository.save(AiArticle.pending(linku));
        } else if (article.getStatus() == AiArticleStatus.DONE) {
            throw new GeneralException(AiArticleErrorStatus._DUPLICATE_AI_ARTICLE);
        } else if (article.getStatus() == AiArticleStatus.PENDING) {
            throw new GeneralException(AiArticleErrorStatus._AI_ARTICLE_GENERATING);
        } else {
            // FAILED -> 재시도
            article.restartPending();
        }

        Long articleId = article.getId();

        // 트랜잭션이 실제로 커밋된 뒤에 비동기 워커를 띄운다. 커밋 전에 띄우면 워커 스레드가 아직
        // 다른 트랜잭션에서는 보이지 않는 PENDING 레코드를 읽으려다 실패할 수 있다.
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    aiArticleGenerationWorker.generateAsync(articleId, userId);
                }
            });
        } else {
            aiArticleGenerationWorker.generateAsync(articleId, userId);
        }

        return AiArticleConverter.toDto(article, linku, usersLinku, resolveTags(linku));
    }

    // AI 요약 호출과 별개로, 링크 저장 시 이미 분류되어 저장된 키워드를 그대로 태그로 사용한다
    // (요약할 때마다 태그를 다시 생성하지 않음 - linku 단위로 한 번 분류된 키워드는 항상 동일해야 함).
    private String resolveTags(Linku linku) {
        return linku.getLinkuKeywords().stream()
                .map(lk -> lk.getKeyword().getName())
                .collect(Collectors.joining(", "));
    }

    // categoryId가 null이면(=쿼리 파라미터 생략) "전체" 카테고리로 간주해 필터 없이 조회한다.
    @Transactional(readOnly = true)
    public LinkuResponseDTO.LinkuSliceResultDTO getMyAiArticlesByCategory(Long userId, Long categoryId, Long cursor, int limit) {
        List<UsersLinku> usersLinkus = usersLinkuRepository.fetchAiArticlesByCategoryIdWithCursor(userId, categoryId, cursor, limit);

        boolean hasNext = usersLinkus.size() > limit;
        List<UsersLinku> resultList = hasNext ? usersLinkus.subList(0, limit) : usersLinkus;

        String nextCursor = hasNext
                ? String.valueOf(resultList.get(resultList.size() - 1).getUserLinkuId())
                : null;

        List<LinkuResponseDTO.AiArticleSummaryDTO> linkuResultDTOs = resultList.stream()
                .map(LinkuConverter::toAiArticleSummaryDTO)
                .collect(Collectors.toList());

        return LinkuResponseDTO.LinkuSliceResultDTO.builder()
                .linkuList(linkuResultDTOs)
                .nextCursor(nextCursor)
                .hasNext(hasNext)
                .build();
    }
}
