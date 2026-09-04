

##flyway 적용
flyway 데이터베이스 마이그레이션 툴을 적용했습니다.
데이터베이스의 변경 사항을 추적하고, 업데이트나 롤백을 보다 쉽게 할 수 있습니다.
V날짜__이름.sql로 작성하고, 이미 존재하는 파일은 수정하지 않는 것이 원칙입니다.
V날짜__이름.sql의 다음르오 V날짜_2__이름.sql가 실행되므로 같은 날짜에 여러 파일을 올리니다면 번호를 붙여주세요
migration 폴더에는 schema가, seed 폴더에는 시딩값이 있습니다. 


> 새로운 마이그레이션 파일들이 생성되면 flyway_schema_history 에서 기존 마이그레이션 이력을 확인합니다. 
따라서 이미 row 에 등록된 스크립트 버전일 경우 중복으로 실행되지 않습니다. 

## 폴더 구조
테이블 생성을 위한 migration과 데이터 생성을 위한 Seed를 분리하기 위해 다음과 같이 설정했습니다.

```
src/
└── main/
└── resources/
└── db/
├── dev/
│     └──migration/
│         ├── V1__init.sql    # 초기 테이블 생성
│         └── ...             # 스키마 추가 및 수정
├── migration/
│     ├── V1__init.sql    # 초기 테이블 생성
│     └── ...             # 스키마 추가 및 수정
│
├── seed/
│     └── V2__seed_master_data.sql # 공통 마스터 데이터 (categories/emotions/situations/jobs 등) — 모든 환경(local/dev/prod)에 적용
│
└── local/
└── V14__... # 로컬 전용 테스트 계정/데이터 — application-local.yml에서만 flyway locations에 포함, prod/dev엔 미적용

```

> `db/local`은 실서비스 콘텐츠가 아닌 개발자 개인 테스트 데이터(테스트 계정, 임의로 채워넣는 저장 링크 등)를
> 위한 폴더다. `db/seed`(공통 마스터 데이터)와 절대 섞지 말 것 — 섞으면 테스트 데이터가 운영 DB에도 들어간다.
> `db/dev`(팀 공유 dev 서버용 픽스처)는 필요해지면 같은 패턴으로 추가하고 `application-dev.yml`에도
> `classpath:db/dev`를 등록한다.

## 환경별 사용 원칙

**local**: 내 컴퓨터에서만 도는 개인 환경이라, 유저·링크 같은 시딩값을 자유롭게 넣어도 된다. `db/local`에 테스트 계정, 임의로 채운 저장 링크 등을 마음대로 추가해서 실험하면 된다.

**dev**: 팀이 같이 쓰는 공유 서버다. `db/migration`, `db/seed`, `db/dev`에 뭔가 올리기 전에 팀한테 먼저 얘기하고 올리자. 말 없이 스키마나 시드가 바뀌면 다른 사람 작업이 예고 없이 깨질 수 있다.

**prod**: 실서비스 DB다. `db/migration`, `db/seed`에 올라가는 건 전부 운영 DB에 그대로 적용된다. dev보다 더 신경 써서, 반드시 미리 얘기하고 올리자. 특히 `db/seed`는 prod에도 적용되는 위치라서, 여기에 유저·링크 같은 실데이터성 시드가 섞여 들어가지 않게 주의한다 — 그런 시드는 local에만.


참고) https://ywoosang.tistory.com/18
참고) https://dev.gmarket.com/76


