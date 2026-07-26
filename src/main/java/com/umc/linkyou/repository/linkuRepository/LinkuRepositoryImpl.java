package com.umc.linkyou.repository.linkuRepository;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.*;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.umc.linkyou.awss3.AwsS3Service;
import com.umc.linkyou.domain.Image;
import com.umc.linkyou.domain.Linku;
import com.umc.linkyou.domain.QImage;
import com.umc.linkyou.domain.QKeyword;
import com.umc.linkyou.domain.QLinku;
import com.umc.linkyou.domain.classification.QDomain;
import com.umc.linkyou.domain.mapping.QLinkuKeyword;
import com.umc.linkyou.domain.mapping.QUsersLinku;
import com.umc.linkyou.web.dto.linku.LinkuQuickSearchResponseDTO;
import com.umc.linkyou.web.dto.linku.LinkuSearchResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class LinkuRepositoryImpl implements LinkuRepositoryCustom {

    private final JPAQueryFactory queryFactory;
    private final AwsS3Service awsS3Service;

    // 링크 검색 (커서 페이징) - 제목/태그 매칭, 최신 저장 순
    @Override
    public List<LinkuSearchResponseDTO.LinkuSearchItemDTO> searchUserLinks(Long userId, String keyword, Long cursor, int size) {
        QUsersLinku ul = QUsersLinku.usersLinku;
        QLinku l = QLinku.linku;
        QDomain d = QDomain.domain;
        QLinkuKeyword lk = QLinkuKeyword.linkuKeyword;
        QKeyword k = QKeyword.keyword;
        QImage ui = new QImage("ui"); // UsersLinku 이미지
        QImage li = new QImage("li"); // Linku(크롤링) 이미지
        QImage di = new QImage("di"); // Domain 이미지

        // 화면에 노출되는 제목은 사용자 지정 값 우선, 없으면 원본 값
        StringExpression displayTitle = ul.title.coalesce(l.title);

        BooleanExpression titleMatches = displayTitle.containsIgnoreCase(keyword);

        // 링크에 붙은 태그 중 검색어와 일치하는 것이 있는지 (exists라 태그 여러 개여도 행이 늘지 않음)
        BooleanExpression tagMatches = JPAExpressions
                .selectOne()
                .from(lk)
                .where(lk.linku.eq(l), lk.keyword.name.containsIgnoreCase(keyword))
                .exists();

        // hasNext 판단용으로 size+1개 조회
        List<Tuple> rows = queryFactory
                .select(ul.userLinkuId, l.linkuId, displayTitle, ui, li, d.name, di)
                .from(ul)
                .join(ul.linku, l)
                .leftJoin(l.domain, d)
                .leftJoin(ul.image, ui)
                .leftJoin(l.image, li)
                .leftJoin(d.image, di)
                .where(
                        ul.user.id.eq(userId),
                        titleMatches.or(tagMatches),
                        // cursor 0 = 첫 페이지 (필터 없음), 그 외에는 해당 커서 이전 항목부터
                        cursor != null && cursor > 0 ? ul.userLinkuId.lt(cursor) : null
                )
                .orderBy(ul.userLinkuId.desc())
                .limit(size + 1L)
                .fetch();

        List<Long> linkuIds = rows.stream()
                .map(r -> r.get(l.linkuId))
                .distinct()
                .toList();

        // 태그는 IN 조회 한 번으로 모아서 조립 (N+1 방지)
        Map<Long, List<String>> tagsByLinkuId = linkuIds.isEmpty() ? Map.of() : queryFactory
                .select(lk.linku.linkuId, k.name)
                .from(lk)
                .join(lk.keyword, k)
                .where(lk.linku.linkuId.in(linkuIds))
                .fetch()
                .stream()
                .collect(Collectors.groupingBy(
                        t -> t.get(lk.linku.linkuId),
                        Collectors.mapping(t -> t.get(k.name), Collectors.toList())
                ));

        return rows.stream()
                .map(r -> {
                    // 화면에 노출되는 이미지는 사용자 지정 값 우선, 없으면 원본(크롤링) 값
                    Image displayImage = r.get(ui) != null ? r.get(ui) : r.get(li);
                    return new LinkuSearchResponseDTO.LinkuSearchItemDTO(
                            r.get(ul.userLinkuId),
                            r.get(l.linkuId),
                            r.get(displayTitle),
                            awsS3Service.resolveUrl(displayImage),
                            tagsByLinkuId.getOrDefault(r.get(l.linkuId), List.of()),
                            awsS3Service.resolveUrl(r.get(di)),
                            r.get(d.name)
                    );
                })
                .toList();
    }

    // 검색어 자동완성 (최대 3개) - 검색과 동일한 제목+태그 매칭, 최신 저장 순 상위 3개
    @Override
    public List<LinkuQuickSearchResponseDTO> findQuickByKeyword(Long userId, String keyword) {
        QUsersLinku ul = QUsersLinku.usersLinku;
        QLinku l = QLinku.linku;
        QDomain d = QDomain.domain;
        QLinkuKeyword lk = QLinkuKeyword.linkuKeyword;
        QImage di = new QImage("di");

        StringExpression displayTitle = ul.title.coalesce(l.title);

        BooleanExpression titleMatches = displayTitle.containsIgnoreCase(keyword);
        BooleanExpression tagMatches = JPAExpressions
                .selectOne()
                .from(lk)
                .where(lk.linku.eq(l), lk.keyword.name.containsIgnoreCase(keyword))
                .exists();

        List<Tuple> rows = queryFactory
                .select(displayTitle, di, ul.userLinkuId)
                .from(ul)
                .join(ul.linku, l)
                .leftJoin(l.domain, d)
                .leftJoin(d.image, di)
                .where(
                        ul.user.id.eq(userId),
                        titleMatches.or(tagMatches)
                )
                .orderBy(ul.userLinkuId.desc())
                .limit(3)
                .fetch();

        return rows.stream()
                .map(r -> new LinkuQuickSearchResponseDTO(
                        r.get(displayTitle),
                        awsS3Service.resolveUrl(r.get(di)),
                        r.get(ul.userLinkuId)
                ))
                .toList();
    }

    @Override
    public Optional<Linku> findByLinku(String normalizedLink) {
        QLinku l = QLinku.linku;

        // AiArticle만 fetch join (이후 바로 null 체크 및 세팅하기 때문)
        Linku result = queryFactory
                .selectFrom(l)
                .leftJoin(l.aiArticle).fetchJoin()
                .where(l.linkuUrl.eq(normalizedLink))
                .fetchFirst();

        return Optional.ofNullable(result);
    }

}
