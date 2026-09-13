package com.umc.linkyou.batch.common;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.database.AbstractPagingItemReader;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Querydsl 기반 no-offset paging reader
 *
 * <p>페이지 번호 대신 마지막으로 읽은 ID를 기준으로 다음 청크를 조회
 * 삭제/상태 변경이 섞인 배치에서도 OFFSET 밀림을 피하기 위한 공통 reader
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
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        String key = getExecutionContextKey("lastId");
        lastId = executionContext.containsKey(key) ? executionContext.getLong(key) : 0L;
        super.open(executionContext);
    }

    @Override
    public void update(@NonNull ExecutionContext executionContext) throws ItemStreamException {
        super.update(executionContext);
        executionContext.putLong(getExecutionContextKey("lastId"), lastId);
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
        // 이전 페이지 엔티티가 남아 있으면 메모리와 영속성 컨텍스트가 불필요하게 커지므로 clear
        results = new CopyOnWriteArrayList<>();
        entityManager.clear();

        List<T> queryResult = fetchQuery(lastId, getPageSize());

        if (queryResult.isEmpty()) {
            return;
        }
        results.addAll(queryResult);
    }

    @Override
    protected void doClose() throws Exception {
        if (entityManager != null && entityManager.isOpen()) {
            entityManager.close();
        }
        super.doClose();
    }


    // 커밋 시점에 영속화되는 lastId 값을 정확하게 가져올 수 있게 , 한번에 읽어오지 않고 read를 오버라이드 해서 읽어올때 시점에 lastId를 가져옴
    @Override
    public T read() throws Exception {
        T item = super.read();
        if (item != null) {
            lastId = getLastId(item);
        }
        return item;
    }

    protected abstract List<T> fetchQuery(Long lastId, int pageSize);

    protected abstract Long getLastId(T item);
}
