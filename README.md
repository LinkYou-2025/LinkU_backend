# LinkU Server

**LinkU**는 저장한 링크를 다시 발견하고, 나에게 맞는 콘텐츠를 큐레이션하는 링크 아카이빙 플랫폼입니다.

> 링크와 당신을 잇다, **LinkU**

- 링크 저장·분류·검색과 폴더 기반 아카이빙
- AI 요약과 개인화 큐레이션을 통한 콘텐츠 재발견
- 공유 폴더와 알림으로 이어지는 링크 경험

<img width="600" alt="LinkU cover" src="https://github.com/user-attachments/assets/7e92645c-c528-42fa-a393-73c301b2bf28" />

## 프로젝트 진행 기간

| 구분 | 기간                 |
| --- |--------------------|
| 데모데이 | 2025.06 ~ 2025.08  |
| 런칭 준비 | ~ ing              |

## 🛠 Tech Stack

<p>
  <strong>Language</strong><br />
  <img src="https://img.shields.io/badge/Java_17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java 17" />
</p>

<p>
  <strong>Framework · Security · Test</strong><br />
  <img src="https://img.shields.io/badge/Spring_Boot-6DB33F?style=for-the-badge&logo=springboot&logoColor=white" alt="Spring Boot" />
  <img src="https://img.shields.io/badge/Spring_Data_JPA-6DB33F?style=for-the-badge&logo=spring&logoColor=white" alt="Spring Data JPA" />
  <img src="https://img.shields.io/badge/Spring_Batch-6DB33F?style=for-the-badge&logo=spring&logoColor=white" alt="Spring Batch" />
  <img src="https://img.shields.io/badge/QueryDSL-005571?style=for-the-badge&logoColor=white" alt="QueryDSL" />
  <img src="https://img.shields.io/badge/Spring_Security-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white" alt="Spring Security" />
  <img src="https://img.shields.io/badge/JWT-000000?style=for-the-badge&logo=jsonwebtokens&logoColor=white" alt="JWT" />
  <img src="https://img.shields.io/badge/OAuth2-4285F4?style=for-the-badge&logo=openid&logoColor=white" alt="OAuth2" />
  <img src="https://img.shields.io/badge/JUnit_5-25A162?style=for-the-badge&logo=junit5&logoColor=white" alt="JUnit 5" />
  <img src="https://img.shields.io/badge/Testcontainers-2496ED?style=for-the-badge&logo=docker&logoColor=white" alt="Testcontainers" />
  <img src="https://img.shields.io/badge/JaCoCo-B4A76C?style=for-the-badge&logoColor=white" alt="JaCoCo" />
  <img src="https://img.shields.io/badge/Resilience4j-6DB33F?style=for-the-badge&logo=spring&logoColor=white" alt="Resilience4j" />
</p>

<p>
  <strong>Data · Storage</strong><br />
  <img src="https://img.shields.io/badge/PostgreSQL-4169E1?style=for-the-badge&logo=postgresql&logoColor=white" alt="PostgreSQL" />
  <img src="https://img.shields.io/badge/Redis-DC382D?style=for-the-badge&logo=redis&logoColor=white" alt="Redis" />
  <img src="https://img.shields.io/badge/Flyway-CC0200?style=for-the-badge&logo=flyway&logoColor=white" alt="Flyway" />
  <img src="https://img.shields.io/badge/AWS_S3-569A31?style=for-the-badge&logo=amazons3&logoColor=white" alt="AWS S3" />
</p>

<p>
  <strong>Infra · Monitoring</strong><br />
  <img src="https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white" alt="Docker" />
  <img src="https://img.shields.io/badge/Terraform-844FBA?style=for-the-badge&logo=terraform&logoColor=white" alt="Terraform" />
  <img src="https://img.shields.io/badge/AWS_EC2-FF9900?style=for-the-badge&logo=amazonec2&logoColor=white" alt="AWS EC2" />
  <img src="https://img.shields.io/badge/GitHub_Actions-2088FF?style=for-the-badge&logo=githubactions&logoColor=white" alt="GitHub Actions" />
  <img src="https://img.shields.io/badge/Spring_Actuator-6DB33F?style=for-the-badge&logo=spring&logoColor=white" alt="Spring Actuator" />
  <img src="https://img.shields.io/badge/Prometheus-E6522C?style=for-the-badge&logo=prometheus&logoColor=white" alt="Prometheus" />
  <img src="https://img.shields.io/badge/Grafana-F46800?style=for-the-badge&logo=grafana&logoColor=white" alt="Grafana" />
</p>

<p>
  <strong>Documentation · External</strong><br />
  <img src="https://img.shields.io/badge/Swagger-85EA2D?style=for-the-badge&logo=swagger&logoColor=black" alt="Swagger" />
  <img src="https://img.shields.io/badge/Firebase_Cloud_Messaging-DD2C00?style=for-the-badge&logo=firebase&logoColor=white" alt="Firebase Cloud Messaging" />
  <img src="https://img.shields.io/badge/Google_Gemini-8E75B2?style=for-the-badge&logo=googlegemini&logoColor=white" alt="Google Gemini" />
</p>

---

## 📚 목차

1. [프로젝트 소개](#-프로젝트-소개)
2. [백엔드 팀원 소개](#-백엔드-팀원-소개)
3. [기술 스택](#-tech-stack)
4. [ERD](#-erd)
5. [서버 아키텍처](#-서버-아키텍처)
6. [프로젝트 구조](#-프로젝트-구조)
7. [협업규칙](#-협업-규칙)

---

## 📖 프로젝트 소개

LinkU는 링크를 단순히 저장하는 데서 그치지 않고, AI 요약·분류와 개인화 큐레이션을 통해 사용자가 필요한 정보를 다시 찾을 수 있도록 돕습니다.

**주요 기능**

- 링크 저장, AI 요약·분류, 키워드 및 링크 검색
- 개인 폴더와 공유 폴더를 통한 링크 관리
- 개인화 큐레이션과 추천 링크 제공
- OAuth2·JWT 기반 회원 인증 및 마이페이지
- FCM 기반 알림, 배치 작업, 운영 모니터링

## 👤 백엔드 팀원 소개

<div align="center">

|                                Backend                                | Backend | Backend |
|:---------------------------------------------------------------------:|:---:|:---:|
|  <img src="https://github.com/oculo0204.png" width="150" alt="서원" />  | <img src="https://github.com/JiwonLee42.png" width="150" alt="지원" /> | <img src="https://github.com/hyorim-jo.png" width="150" alt="효림" /> |
| [서원](https://github.com/oculo0204)<br />링크 저장 · 크롤링/AI · 소셜로그인 · 회원정보 | [지원](https://github.com/JiwonLee42)<br />알림 · 서버 배포 · 회원<br />검색 · 배치 작업 | [효림](https://github.com/hyorim-jo)<br />큐레이션 · 모니터링<br />폴더 · 공유 폴더 |

</div>

---

## 🗂 ERD

<img width="700" height="500" alt="LinkU ERD" src="https://github.com/user-attachments/assets/920add42-371d-4b07-b7af-52fda4b913c2" />

---

## 🖥 서버 아키텍처

<img width="700" height="800" alt="LinkU 서비스 아키텍처" src="https://github.com/user-attachments/assets/77e646eb-1ccc-4a62-a5cd-7af6719fa669" />

---

## 📂 프로젝트 구조

```plaintext
src
 ├── main
 │   ├── java/com/umc/linkyou
 │   │   ├── apiPayload     # 공통 응답·예외·상태 코드
 │   │   ├── batch          # 배치 작업
 │   │   ├── config         # 보안·인프라·프로퍼티 설정
 │   │   ├── converter      # Entity ↔ DTO 변환
 │   │   ├── domain         # JPA 엔티티·도메인 enum
 │   │   ├── infra          # AI·S3·FCM·파서 외부 연동
 │   │   ├── jwt            # JWT 인증·세션 관리
 │   │   ├── repository     # 데이터 접근 계층
 │   │   ├── service        # 비즈니스 로직
 │   │   └── web            # Controller·API·DTO
 │   └── resources
 │       ├── application.yml
 │       └── db             # 마이그레이션·초기 데이터
 └── test
     └── java/com/umc/linkyou
```


---

## 🤝 협업 규칙

### 이슈 컨벤션
- 작업 목적에 맞는 이슈 템플릿을 사용하고, assignee, 작업기한 등을 명시합니다.
- 이슈는 담당자 확인을 하고 특별한 사유가 없다면 본인이 담당하는 작업의 이슈를 직접 등록합니다.
- 작업 시점과 목적에 따라 지정된 GitHub Project와 MileStone에 이슈를 등록합니다.

### 브랜치 컨벤션
- 모든 작업은 이슈를 등록한 뒤 `develop`에서 분기합니다. 완료된 변경은 Pull Request에서 리뷰를 거쳐 `develop`에 병합합니다.
- 작업 완료 후 PR 리뷰를 거쳐 `develop`에 병합합니다.
- `develop`에 병합된 후 3개월 이상 지난 작업 브랜치는 팀원들과 협의하에 담당자가 삭제합니다.

- 운영 통합 브랜치: `release`
- 개발 통합 브랜치: `develop`
- 기능 개발: `feat/#이슈번호-간단한설명`
- 버그 수정: `fix/#이슈번호-간단한설명`
- 리팩터링: `refactor/#이슈번호-간단한설명`
- 문서·기타: `chore/#이슈번호-간단한설명`, `docs/#이슈번호-간단한설명`

### 커밋 컨벤션
- 커밋 타입은 영어로, 작업 내용은 한글로 작성합니다.
- 다음 형식을 사용합니다: `type: #이슈번호 작업 내용`
- 예시: `feat: #123 링크 검색 기능 추가`

| 타입 | 사용 목적 |
| --- | --- |
| `feat` | 기능 추가 |
| `fix` | 버그 수정 |
| `refactor` | 기능 변경 없는 코드 개선 |
| `test` | 테스트 추가·수정 |
| `docs` | 문서 작성·수정 |
| `chore` | 빌드·설정·의존성 변경 |
| `style` | 포맷·공백 수정 |


### pr 컨벤션
- PR 제목은 커밋과 동일한 형식으로 작성합니다.
- 리뷰어를 지정하고, 리뷰에서 요청된 사항을 반영한 뒤 병합합니다.
- 3일이내에 리뷰어의 리뷰가 달리지 않거나, 서버가 멈추는 등 치명적 오류의 경우 담당자가 병합할 수 있습니다

---

## Copyright

© 2026 LinkU Team. All rights reserved.
