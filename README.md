# backend-server  
LinkU 백엔드 레포지토리 입니다.

** 로컬에서 서버를 실행하는 법 **  
1. Redis 실행하기</br>
(0) docker desktop과 wsl설치 </br>
```wsl --install```</br>
```wsl --set-default-version 2```</br>
```docker --version```</br></br>
(1) docker desktop키기</br></br>
(2) redis를 docker로 실행</br></br>
```docker run -d --name redis -p 6379:6379 redis:7```</br></br>
2. spring 서버 실행
3. 트러블 슈팅</br>
-docker: Error response from daemon: Conflict. The container name "/redis" is already in use by container </br>
```docker rm -f redis```
---

## 📖 프로젝트 소개  
<aside>
💡 링큐 (Link U) : **LINK U, THINK YOU**

> 당신을 생각하는 링크, 링큐
“사용자의 라이프스타일에 맞춰 AI가 링크를 추천하고, 맞춤형 큐레이션을 제공하는 앱”
> 
</aside>

- 사용자가 앱에 저장한 링크를 바탕으로 AI가 정보를 간단히 요약하고 분류
- 저장된 링크 기반으로 사용자의 라이프스타일, 감정 상태 등을 고려한 맞춤형 큐레이션 서비스 제공
- 저장한 링크를 맞춤형 뉴스레터 형식으로 제공

---

## 🛠 기술 스택 및 환경

- **Backend**  
  - Java 17  
  - Spring Boot 3.4.7  
  - Gradle 8.14.2  
  - Hibernate ORM 6.0.2  

- **Database**  
  - MariaDB 3.3.3 (JDBC 드라이버)  

- **캐싱 및 세션**  
  - Redis (spring-boot-starter-data-redis)  

- **보안 및 인증**  
  - Spring Security  
  - JWT (jjwt 라이브러리)  

- **API 문서화**  
  - Swagger (springdoc-openapi-starter-webmvc-ui 2.7.0)  

- **UI 템플릿**  
  - Thymeleaf + Spring Security  

- **클라우드 & 인프라**  
  - AWS EC2, S3, Route53  

- **CI/CD**  
  - GitHub Actions  

- **주요 라이브러리**  
  - Lombok, QueryDSL, JavaMail, SendGrid, Jsoup, Spring WebFlux  

- **Gradle 설정**  
  - `java`, `org.springframework.boot`, `io.spring.dependency-management` 플러그인 적용  
  - QueryDSL 자동 생성, 컴파일러 추가 옵션 설정  

---

## 📂 프로젝트 구조  
```
backend-server/
├── src/
│   ├── main/
│   │   ├── java/com/linku/
│   │   │   ├── controller/   # API 엔드포인트
│   │   │   ├── service/      # 비즈니스 로직
│   │   │   ├── repository/   # 데이터 접근 계층
│   │   │   ├── config/       # 설정
│   │   │   └── utils/        # 유틸리티 클래스
│   │   └── resources/
│   │       ├── application.properties
│   │       └── data.sql      # 초기값 설정
│   └── test/
│       └── java/com/linku/
└── build.gradle
```

### 💙 팀원 소개

|장서원|김하진|나현주|조효림|
|:---:|:---:|:---:|:---:|
|<img src="https://github.com/user-attachments/assets/65b56c2f-15f4-4dcd-9871-ac4656773441" width="300" height="230">|<img src="https://github.com/user-attachments/assets/7319fc3a-29cd-48b9-b3a0-860368117081" width="300" height="230">|<img src="https://github.com/HyeonJooooo.png" width="300" height="230">|<img src="https://github.com/user-attachments/assets/8becc477-f7d2-4d85-96ba-0b9e8719413a" width="300" height="230">|
|[@oculo0204](https://github.com/oculo0204)|[@Hajin99](https://github.com/Hajin99)|[@HyeonJooooo](https://github.com/HyeonJooooo)|[@hyorim-jo](https://github.com/hyorim-jo)|
|ai기반 링크 생성 및 추천, 회원 탈퇴, 서버 배포| 회원정보 관련 기능, redis 설정 |ai기반 링크 큐레이션, cloudwatch 설정| 폴더 공유 포함 폴더 관련 기능|


</br></br>

## 📌 Branch 전략 ##
## Branch

본 프로젝트는 Gitflow 브랜치 전략을 따릅니다.


<div align=center>
    <img src="https://techblog.woowahan.com/wp-content/uploads/img/2017-10-30/git-flow_overall_graph.png" width=50% alt="브랜치 전략 설명 이미지"/>
</div>

모든 기능 개발은 다음 흐름을 따릅니다.

1. 개발하고자 하는 기능에 대한 이슈를 등록하여 번호를 발급합니다.
2. `main` 브랜치로부터 분기하여 이슈 번호를 사용해 이름을 붙인 `feature` 브랜치를 만든 후 작업합니다.
3. 작업이 완료되면 `develop` 브랜치에 풀 요청을 작성하고, 팀원의 동의를 얻으면 병합합니다.

# Branch	종류
- main	기능 개발 통합 브랜치 (pull request하고 동료들에게 merge요청, 확인이 오래걸리면 스스로 merge) 
데모용 프로젝트이기 때문에 배포용 브랜치를 따로 두지 않습니다.
- feature/{이슈번호}{간단한설명}	새로운 기능 개발 브랜치
- fix/{이슈번호}{간단한설명}	버그 수정 브랜치
- hotfix/{이슈번호}{간단한설명}	긴급 수정 브랜치
- refactor/{이슈번호}{간단한설명}	리팩토링 브랜치
- chore/{이슈번호}{간단한설명}	기타 설정, 패키지 변경 등
# Branch    설명
1. 기능개발이 완료된 브랜치는 develop브랜치에 merge합니다.
2. merge된 Branch는 삭제합니다.
</br></br>
✅ 예시
- feature/#12-login-api
- fix/#17-cors-error
- chore/#20-env-setting
</br></br>
✅ Git 사용 규칙
# 커밋 메시지 형식
- #이슈번호 <타입>: <변경 요약> 
</br>
- <타입> 종류</br>
태그 이름	설명</br>
[init] 초기설정</br>
[chore]	코드 수정, 내부 파일 수정</br>
[feat]	새로운 기능 구현</br>
[add]	FEAT 이외의 부수적인 코드 추가, 라이브러리 추가, 새로운 파일 생성</br>
[hotfix]	issue나 QA에서 급한 버그 수정에 사용</br>
[fix]	버그, 오류 해결</br>
[del]	쓸모 없는 코드 삭제</br>
[docs]	README나 WIKI 등의 문서 개정</br>
[correct]	주로 문법의 오류나 타입의 변경, 이름 변경에 사용</br>
[move]	프로젝트 내 파일이나 코드의 이동</br>
[rename]	파일 이름 변경이 있을 때 사용</br>
[improve]	향상이 있을 때 사용</br>
[refactor]	전면 수정이 있을 때 사용</br>
[test]	테스트 코드 추가 시 사용 </br>

# 서비스 아키텍처
<img width="876" height="977" alt="링큐 서비스 아키텍쳐 drawio (2)" src="https://github.com/user-attachments/assets/77e646eb-1ccc-4a62-a5cd-7af6719fa669" />


# erd
<img width="5340" height="2092" alt="linkU-BE" src="https://github.com/user-attachments/assets/920add42-371d-4b07-b7af-52fda4b913c2" />
