## 작업 목표
코드를 수정하기 전, 변경 범위와 영향도를 분석한다.
절대 코드를 수정하지 않는다.

## 입력
$ARGUMENTS 에 이슈 번호 또는 작업 설명이 들어온다.

## 수행 절차
1. 다음 파일들을 읽는다 (수정 금지):
    - Entity: src/main/java/**/domain/**/*.java
    - Repository: src/main/java/**/repository/**/*.java
    - Service: src/main/java/**/service/**/*.java
    - Controller: src/main/java/**/controller/**/*.java

2. 아래 항목을 분석하여 보고한다:

### 📁 영향받는 파일 목록
변경이 필요한 파일을 레이어별로 나열한다.
(Entity → Repository → Service → Controller 순서)

### 🔗 의존 관계
변경 시 함께 수정해야 할 연관 클래스/메서드를 정리한다.

### ⚠️ 위험 요소
- 데이터 손실 가능성
- 기존 API 호환성 깨지는 부분
- 트랜잭션 영향 범위

### 📋 작업 순서 제안
안전한 변경 순서를 dependency 기준으로 번호 매겨 제시한다.
(예: 1. Migration SQL → 2. Entity → 3. Repository → 4. Service → 5. Controller)

### ✅ 완료 조건
각 단계별 검증 방법을 제시한다.
(예: ./gradlew test, API 호출 테스트 등)

## 제약
- 이 단계에서 절대 코드를 수정하지 않는다.
- 분석 결과만 출력한다.
- 확신이 없는 부분은 "확인 필요"로 표시한다.

## 출력 형식
- 영향받는 파일 목록
- 작업 순서
- 위험 요소
- 다음 단계 추천

## 출력 저장하기
출력을 마친후, claude-output.md에 저장한다.
