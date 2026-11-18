package com.umc.linkyou.service.Linku;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import com.umc.linkyou.apiPayload.code.status.ErrorStatus;
import com.umc.linkyou.apiPayload.code.status.folder.FolderErrorStatus;
import com.umc.linkyou.apiPayload.code.status.user.UserErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.converter.LinkuConverter;
import com.umc.linkyou.domain.Linku;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.classification.Category;
import com.umc.linkyou.domain.classification.Domain;
import com.umc.linkyou.domain.classification.Emotion;
import com.umc.linkyou.domain.classification.Situation;
import com.umc.linkyou.domain.folder.Folder;
import com.umc.linkyou.domain.mapping.LinkuFolder;
import com.umc.linkyou.domain.mapping.UsersLinku;
import com.umc.linkyou.repository.UserLinkuRepository.UsersLinkuRepository;
import com.umc.linkyou.repository.classification.domainRepository.DomainRepository;
import com.umc.linkyou.repository.linkuRepository.LinkuRepository;
import com.umc.linkyou.repository.mapping.linkuFolderRepository.LinkuFolderRepository;
import com.umc.linkyou.repository.userRepository.UserRepository;
import com.umc.linkyou.repository.usersFolderRepository.UsersFolderRepository;
import com.umc.linkyou.service.keyword.KeywordService;
import com.umc.linkyou.web.dto.linku.LinkuResponseDTO;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class LinkuPersister {
    private static final Long DEFAULT_DOMAIN_ID = 1L;
    private final LinkuRepository linkuRepository;
    private final DomainRepository domainRepository;
    private final LinkuFolderRepository linkuFolderRepository;
    private final UsersLinkuRepository usersLinkuRepository;
    private final UserRepository userRepository;
    private final UsersFolderRepository usersFolderRepository;
    private final KeywordService keywordService;
    private final LinkuUpsertService linkuUpsertService;
    private final LinkuClassificationResolver classificationResolver;
    private final TransactionTemplate transactionTemplate;

    public LinkuResponseDTO.LinkuResultDTO persist(PersistCommand command) {
        return transactionTemplate.execute(
                (TransactionCallback<LinkuResponseDTO.LinkuResultDTO>)
                        status -> persistLinku(command));
    }

    // 트랜잭션 안이라 외부 HTTP 호출을 하지 않는다.
    private LinkuResponseDTO.LinkuResultDTO persistLinku(PersistCommand cmd) {
        Domain domain =
                cmd.preResolvedDomain() != null
                        ? cmd.preResolvedDomain()
                        : resolveDomain(cmd.domainTailCandidates());
        Optional<Linku> existingLinku = linkuRepository.findByLinku(cmd.linkUrl());
        Linku linku;
        Category category;
        String aiTitle;
        Long aiEmotionId;
        Long aiSituationId;
        String keywords;
        if (existingLinku.isPresent()) {
            linku = existingLinku.get();
            category = linku.getCategory();
            aiTitle = linku.getTitle();
            aiEmotionId = linku.getEmotion().getEmotionId();
            aiSituationId = linku.getSituation().getId();
            keywords =
                    linku.getLinkuKeywords().stream()
                            .map(lk -> lk.getKeyword().getName())
                            .collect(Collectors.joining(", "));
        } else {
            // 비콘텐츠이거나 크롤링하지 못한 링크는 제목만 만든다.
            NewLinkuAiData aiData = cmd.aiData();
            if (aiData == null)
                aiData =
                        new NewLinkuAiData(
                                null,
                                null,
                                null,
                                null,
                                LinkuTitleResolver.resolve(null, cmd.domainTail(), cmd.linkUrl()),
                                null);
            category = classificationResolver.resolveCategory(aiData.aiCategoryId());
            keywords = aiData.keywords();
            aiEmotionId = aiData.aiEmotionId();
            aiSituationId = aiData.aiSituationId();
            aiTitle = aiData.aiTitle();
            Emotion aiEmotion = classificationResolver.resolveEmotion(null, aiEmotionId);
            Situation aiSituation = classificationResolver.resolveSituation(null, aiSituationId);
            linku =
                    linkuUpsertService.upsert(
                            cmd.linkUrl(),
                            category,
                            domain,
                            aiTitle,
                            aiData.crawledImgUrl(),
                            aiEmotion,
                            aiSituation);
            keywordService.saveKeywords(linku, keywords);
            linku.updatePageType(cmd.pageType(), LocalDateTime.now());
        }
        boolean userProvidedEmotion =
                cmd.dto().getEmotionId() != null && cmd.dto().getEmotionId() > 0;
        boolean userProvidedSituation =
                cmd.dto().getSituationId() != null && cmd.dto().getSituationId() > 0;
        boolean userProvidedTitle = cmd.dto().getTitle() != null && !cmd.dto().getTitle().isBlank();
        Emotion emotion =
                classificationResolver.resolveEmotion(
                        userProvidedEmotion ? cmd.dto().getEmotionId() : null, aiEmotionId);
        Situation situation =
                classificationResolver.resolveSituation(
                        userProvidedSituation ? cmd.dto().getSituationId() : null, aiSituationId);
        Users user = findUser(cmd.userId());
        UsersLinku usersLinku =
                createUsersLinku(
                        user,
                        linku,
                        emotion,
                        situation,
                        cmd.savedUrl(),
                        cmd.dto().getMemo(),
                        cmd.userImageUrl(),
                        userProvidedTitle ? cmd.dto().getTitle() : aiTitle,
                        !userProvidedEmotion,
                        !userProvidedSituation);
        Folder folder =
                usersFolderRepository
                        .findFolderByUserIdAndCategory(cmd.userId(), category)
                        .orElseThrow(
                                () -> new GeneralException(FolderErrorStatus._FOLDER_NOT_FOUND));
        LinkuFolder linkuFolder = LinkuConverter.toLinkuFolder(folder, usersLinku);
        linkuFolderRepository.save(linkuFolder);
        String domainName = domain != null ? domain.getName() : null;
        String domainImageUrl = domain != null ? domain.getImageUrl() : null;
        return LinkuConverter.toLinkuResultDTO(
                cmd.userId(),
                linku,
                usersLinku,
                linkuFolder,
                category,
                domainName,
                domainImageUrl,
                false,
                keywords,
                "");
    }

    public Domain resolveDomain(List<String> domainTailCandidates) {
        return domainTailCandidates.stream()
                .map(domainRepository::findByDomainTail)
                .flatMap(Optional::stream)
                .findFirst()
                .orElseGet(
                        () ->
                                domainRepository
                                        .findById(DEFAULT_DOMAIN_ID)
                                        .orElseThrow(
                                                () ->
                                                        new GeneralException(
                                                                ErrorStatus._DOMAIN_NOT_FOUND)));
    }

    public Users findUser(Long userId) {
        return userRepository
                .findById(userId)
                .orElseThrow(() -> new GeneralException(UserErrorStatus._USER_NOT_FOUND));
    }

    public UsersLinku createUsersLinku(
            Users user,
            Linku linku,
            Emotion emotion,
            Situation situation,
            String savedUrl,
            String memo,
            String imageUrl,
            String title,
            boolean emotionAi,
            boolean situationAi) {
        UsersLinku usersLinku =
                LinkuConverter.toUsersLinku(
                        user,
                        linku,
                        emotion,
                        situation,
                        savedUrl,
                        memo,
                        imageUrl,
                        title,
                        emotionAi,
                        situationAi);
        // 같은 링크를 이미 요약했다면 새 저장 건에도 요약 있음을 표시한다.
        boolean userAlreadyHasAiArticle =
                usersLinkuRepository
                        .findByUser_IdAndLinku_LinkuId(user.getId(), linku.getLinkuId())
                        .stream()
                        .anyMatch(ul -> Boolean.TRUE.equals(ul.getAiExist()));
        if (userAlreadyHasAiArticle) usersLinku.markAiExist(true);
        UsersLinku saved = usersLinkuRepository.save(usersLinku);
        return saved;
    }
}
