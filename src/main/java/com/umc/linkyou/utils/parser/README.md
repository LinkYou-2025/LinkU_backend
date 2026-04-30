### WebContentExtractor에 대한 설명 ###

- ContentExtractorStrategy 인터페이스
  extract(Document doc, String url) 메소드 정의

- DefaultExtractor클래스
  extract(Document doc, String url)를 구현

- NaverBlogExtractor 클래스
  extract(Document doc, String url)를 구현

-BodyExtractor 클래스
extract(Document doc, String url)를 구현

---

-initStrategies(List<Domain> domains)
도메인 리스트를 받아 도메인별 크롤링 전략 객체를 생성해 Map에 저장하고,
도메인별 CrawlStrategy(enum값)에 따라 위에서 정의한 추출 전략 중 적절한 ContentExtractorStrategy 클래스 인스턴스 생성합니다.

-isAllowedByRobotsTxt(String urlStr, String userAgent)
주어진 URL의 robots.txt 내용을 파싱해 크롤링 허용 여부를 판단합니다.
robots.txt가 없거나 오류 발생 시 기본적으로 허용 처리합니다.

-extractTextFromUrl(String url)
전체 크롤링 프로세스를 진행하는 메소드입니다.

---

전체 플로우는 아래와 같습니다.

robots.txt 접근 허용 검사
-> URL 정규화 및 도메인 tail 추출
-> 도메인 기반 크롤링 전략 초기화
-> Jsoup으로 HTML 문서 로드
-> 도메인 맞춤 추출 전략으로 본문 텍스트 추출
-> 추출 실패 시 대체 본문 시도 및 실패 시 예외 발생
-> 본문 텍스트 반환
