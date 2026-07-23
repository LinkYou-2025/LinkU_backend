# Batch Reader Test Note

## 정리 내용

- `CurationItemReaderTest`는 제거했다
- Reader 방식 비교용 출력, 시간 측정, 벤치마크 코드는 남기지 않았다
- 현재는 `batch/common/QuerydslPagingItemReader`와 분리된 reader 클래스 구조만 프로덕션 코드에 유지한다

## 이유

- Mock 기반 reader 비교는 실제 DB 성능 비교로 오해될 여지가 있다
- 현재 요구사항은 테스트 비교 문서화보다 reader 구조 정리가 우선이다
- 큐레이션 reader 전용 테스트를 유지하면 이후 구현 변경 시 테스트 의도가 흐려질 수 있다

## 현재 상태

- 큐레이션 배치와 비활성 사용자 배치 모두 `QuerydslPagingItemReader`를 상속하는 reader로 관리한다

### 큐레이션 Querydsl reader 설정

```java
@Component("curationItemReader")
public class CurationItemReader extends QuerydslPagingItemReader<Users> {

    public CurationItemReader(EntityManagerFactory entityManagerFactory) {
        super("curationItemReader", 100, entityManagerFactory);
    }

    @Override
    protected List<Users> fetchQuery(Long lastId, int pageSize) {
        QUsers users = QUsers.users;
        return queryFactory.selectFrom(users)
                .where(users.id.gt(lastId))
                .orderBy(users.id.asc())
                .limit(pageSize)
                .fetch();
    }

    @Override
    protected Long getLastId(Users item) {
        return item.getId();
    }
}
```

- 페이지 번호가 아니라 마지막으로 읽은 ID(`lastId`)를 기준으로 다음 페이지를 조회하는 no-offset cursor 방식이다
- 설정 클래스 안의 익명 객체가 아니라 별도 reader 클래스로 관리한다

## 후속 작업 기준

- 실제 reader 검증이 필요하면 Mock 비교가 아니라 DB 기반 repository/custom query 테스트로 분리한다
- 성능 비교가 필요하면 테스트 코드가 아니라 별도 측정 시나리오나 운영 로그 기반으로 확인한다
