

##flyway 적용
flyway 데이터베이스 마이그레이션 툴을 적용했습니다.
데이터베이스의 변경 사항을 추적하고, 업데이트나 롤백을 보다 쉽게 할 수 있습니다.
V번호__이름.sql로 작성하고, 이미 존재하는 파일은 수정하지 않는 것이 원칙입니다. 
번호가 큰 것이 자동으로 적용되므로 번호 순서를 잘 지켜주세요. 
migration 폴더에는 schema가, seed 폴더에는 시딩값이 있습니다. 
두 폴더를 합쳐서 번호가 붙으니 꼭 참고해주세요!


> 새로운 마이그레이션 파일들이 생성되면 flyway_schema_history 에서 기존 마이그레이션 이력을 확인합니다. 
최근에 적용된 버전과 새 파일들의 버전을 비교해서 버전 번호가 같거나 작으면 무시합니다. 
따라서 이미 row 에 등록된 스크립트 버전일 경우 중복으로 실행되지 않습니다. 
버전 번호가 큰 마이그레이션들은 pending migrations 로 지정됩니다. 
> pending migrations 들에 대해 버전번호를 기준으로 정렬 후 순서대로 마이그레이션을 적용시키고 flyway_schema_history 을 업데이트합니다.


## 폴더 구조
테이블 생성을 위한 Migration 과 데이터 생성을 위한 Seed를 분리하기 위해 다음과 같이 설정했습니다.
src/
└── main/
└── resources/
└── db/
├── migration/
│     ├── V1__init.sql    # 초기 테이블 생성
│     └── ...             # 스키마 추가 및 수정
│
└── seed/
│     └──V2__seed_master_data.sql # 시드 데이터
├── local/
│     └── ...  # 샘플 데이터 추가 및 수정
└── dev/
└── ...  # 샘플 데이터 추가 및 수정

참고) https://ywoosang.tistory.com/18
참고) https://dev.gmarket.com/76



## 버전의 중요성!
Flyway는 파일명을 보고 실행 순서를 정하기 때문에 V숫자__설명.sql 형식을 지켜야 합니다. 
V1다음에 V2가 실행됩니다.
그래서 V1__init.sql 다음에 V2__seed_master_data.sql이 자동으로 이어서 실행됩니다.

## 도입한 base 라인 시점
이미 운영 중인 데이터베이스에 Flyway를 도입할 때, 준점 이후의 마이그레이션만을 적용될 수 있게 하는 것이 Baseline입니다.

2026.06.08
커밋 : b2dae9d5e5207f3aef865e21c97b8688cb14403c


## 의존성
- build.gradle
```
//mysql
implementation 'com.mysql:mysql-connector-j'
// flyway
implementation 'org.flywaydb:flyway-core'
implementation 'org.flywaydb:flyway-mysql'
```
- data.sql 삭제함
- application.yml도 아래와 같이 변경함
Flyway를 스키마/시드의 단일 실행 주체로 두고, Spring Boot의 기본 SQL 초기화와 JPA 자동 변경은 끄는 방향으로 정리했습니다.

```

  jpa:
    show-sql: true
    hibernate:
      ddl-auto: validate
    defer-datasource-initialization: false
    properties:
      hibernate:
        format_sql: true
        jdbc:
          time_zone: Asia/Seoul
  flyway:
    enabled: true
    baseline-on-migrate: true
    baseline-version: 0
    locations:
      - classpath:db/migration
      - classpath:db/seed
  sql:
    init:
      mode: never
```

 1. JPA 설정 변경 이유

- `ddl-auto: validate`  
  기존의 `update` 대신 `validate`로 변경하여, JPA가 직접 테이블이나 컬럼 구조를 수정하지 않고 엔티티와 DB 스키마의 일치 여부만 검증하도록 했다.  
  즉, 스키마 변경은 Flyway가 담당하고 JPA는 검증만 수행하도록 역할을 나눴다.

- `defer-datasource-initialization: false`  
  이 옵션은 Hibernate 초기화 이후에 `schema.sql`이나 `data.sql` 같은 스크립트 초기화를 지연 실행할 때 사용하는 설정이다.  
  이번 구성에서는 SQL 초기화를 Flyway만 사용하도록 통일했기 때문에 별도의 지연 초기화가 필요하지 않아 `false`로 설정했다.

- `show-sql`, `format_sql`, `jdbc.time_zone`  
  이 설정들은 Flyway와 직접 관련되지는 않지만, 실행되는 JPA 쿼리를 확인하고 시간대를 일관되게 유지하기 위해 그대로 사용했다.

2. Flyway 설정 변경 이유

- `enabled: true`  
  애플리케이션 실행 시 Flyway가 자동으로 마이그레이션을 수행하도록 활성화했다.

- `baseline-on-migrate: true`  
  이미 운영 중인 데이터베이스에 Flyway를 처음 도입하는 상황이므로, 현재 DB 상태를 기준점으로 등록할 수 있도록 설정했다.

- `baseline-version: 0`  
  기존 운영 DB를 버전 0 상태로 간주하고, 이후 마이그레이션 파일을 `V1`, `V2`부터 순차적으로 적용할 수 있도록 했다.

- `locations`  
  스키마 변경 SQL과 시드 데이터 SQL을 목적에 따라 분리해서 관리하기 위해 Flyway 경로를 두 군데로 나누어 지정했다.  
  `db/migration`은 테이블 생성 및 구조 변경용이고, `db/seed`는 공통 초기 데이터 입력용이다.

3. SQL 초기화 비활성화 이유

- `sql.init.mode: never`  
  Spring Boot의 기본 SQL 초기화 기능(`schema.sql`, `data.sql`)은 사용하지 않도록 비활성화했다.  
  Flyway를 사용하는 경우 스키마 생성과 데이터 초기화를 Flyway에 일원화하는 것이 더 안전하며, 중복 실행이나 충돌 가능성을 줄일 수 있다.
