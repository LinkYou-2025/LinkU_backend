# backend-server
LinkU 백엔드 레포지토리 입니다.

**✅ 진행 사항 확인**

- **Notion**에서 자세한 진행사항 보러가기 -> 
  [![Notion](https://img.shields.io/badge/Notion-000000?style=flat-square&logo=notion&logoColor=white)](https://sapphire-lamprey-5db.notion.site/1f393020f65580249c69d231b96abbec?source=copy_link)
</br></br>

### 💙 팀원 소개

|장서원|김하진|나현주|조효림|
|:---:|:---:|:---:|:---:|
|<img src="https://github.com/user-attachments/assets/65b56c2f-15f4-4dcd-9871-ac4656773441" width="300" height="230">|<img src="https://github.com/user-attachments/assets/7319fc3a-29cd-48b9-b3a0-860368117081" width="300" height="230">|<img src="https://github.com/HyeonJooooo.png" width="300" height="230">|<img src="https://github.com/user-attachments/assets/8becc477-f7d2-4d85-96ba-0b9e8719413a" width="300" height="230">|
|[@oculo0204](https://github.com/oculo0204)|[@Hajin99](https://github.com/Hajin99)|[@HyeonJooooo](https://github.com/HyeonJooooo)|[@hyorim-jo](https://github.com/hyorim-jo)|


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

# Environment
Java 17
gradle-8.14.2
springboot 3.4.7


