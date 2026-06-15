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

- 큐레이션 배치는 `RepositoryItemReader`를 상속한 `CurationItemReader`로 분리했다
- 비활성 사용자 배치는 `QuerydslPagingItemReader`를 상속하는 `InactiveUserItemReader`로 유지한다

### 큐레이션 Repository reader 설정

```java
@Component("curationItemReader")
public class CurationItemReader extends RepositoryItemReader<Users> {

    public CurationItemReader(UserRepository userRepository) {
        setName("curationItemReader");
        setRepository(userRepository);
        setMethodName("findAll");
        setArguments(Collections.emptyList());
        setPageSize(100);
        setSort(Collections.singletonMap("id", Sort.Direction.ASC));
    }
}
```

- `findAll`을 page 단위로 호출하면서 `id ASC` 순서로 사용자를 순회한다
- 설정 클래스 안의 익명 객체가 아니라 별도 reader 클래스로 관리한다

## 후속 작업 기준

- 실제 reader 검증이 필요하면 Mock 비교가 아니라 DB 기반 repository/custom query 테스트로 분리한다
- 성능 비교가 필요하면 테스트 코드가 아니라 별도 측정 시나리오나 운영 로그 기반으로 확인한다
