

##flyway 적용

새로운 마이그레이션 파일들이 생성되면 flyway_schema_history 에서 기존 마이그레이션 이력을 확인한다. 최근에 적용된 버전과 새 파일들의 버전을 비교해서 버전 번호가 같거나 작으면 무시한다. 따라서 이미 row 에 등록된 스크립트 버전일 경우 중복으로 실행되지 않는다. 버전 번호가 큰 마이그레이션들은 pending migrations 로 지정된다.


pending migrations 들에 대해 버전번호를 기준으로 정렬 후 순서대로 마이그레이션을 적용시키고 flyway_schema_history 을 업데이트한다.

## 도입한 base 라인 시점
이미 운영 중인 데이터베이스에 Flyway를 도입할 때, 준점 이후의 마이그레이션만을 적용될 수 있게 하는 것이 Baseline입니다.

커밋 : b2dae9d5e5207f3aef865e21c97b8688cb14403c

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

## 의존성

```
//mysql
implementation 'com.mysql:mysql-connector-j'
// flyway
implementation 'org.flywaydb:flyway-core'
implementation 'org.flywaydb:flyway-mysql'
```

application.yml도 변경함

## 
Flyway는 파일명을 보고 실행 순서를 정하기 때문에 V숫자__설명.sql 형식을 지켜야 합니다. 
V1다음에 V2가 실행됩니다.
그래서 V1__init.sql 다음에 V2__seed_master_data.sql이 자동으로 이어서 실행됩니다.

