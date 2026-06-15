package com.umc.linkyou.batch.common;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.database.AbstractPagingItemReader;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Querydsl 기반 no-offset paging reader
 *
 * <p>페이지 번호 대신 마지막으로 읽은 ID를 기준으로 다음 청크를 조회한다.
 * 삭제/상태 변경이 섞인 배치에서도 OFFSET 밀림을 피하기 위한 공통 reader다.
 */
@Slf4j
public abstract class QuerydslPagingItemReader<T> extends AbstractPagingItemReader<T> {

    private final EntityManagerFactory entityManagerFactory;

    protected EntityManager entityManager;
    protected JPAQueryFactory queryFactory;
    protected Long lastId = 0L;

    protected QuerydslPagingItemReader(String name, int pageSize, EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
        setName(name);
        setPageSize(pageSize);
    }

    @Override
    protected void doOpen() throws Exception {
        super.doOpen();
        entityManager = entityManagerFactory.createEntityManager();
        queryFactory = new JPAQueryFactory(entityManager);
    }

    /**
     * Step이 chunk 트랜잭션을 관리하므로 reader는 조회 상태만 관리한다
     */
    @Override
    protected void doReadPage() {
        // 이전 페이지 엔티티가 남아 있으면 메모리와 영속성 컨텍스트가 불필요하게 커진다
        results = new CopyOnWriteArrayList<>();
        entityManager.clear();

        List<T> queryResult = fetchQuery(lastId, getPageSize());

        if (queryResult.isEmpty()) {
            return;
        }

        // 다음 페이지는 마지막으로 읽은 ID 다음부터 조회한다
        lastId = getLastId(queryResult.get(queryResult.size() - 1));
        results.addAll(queryResult);
    }

    protected void doJumpToPage(int itemIndex) {
        log.debug("No-offset reader does not support random page jump. itemIndex={}", itemIndex);
    }

    @Override
    protected void doClose() throws Exception {
        if (entityManager != null && entityManager.isOpen()) {
            entityManager.close();
        }
        super.doClose();
    }

    protected abstract List<T> fetchQuery(Long lastId, int pageSize);

    protected abstract Long getLastId(T item);
}
