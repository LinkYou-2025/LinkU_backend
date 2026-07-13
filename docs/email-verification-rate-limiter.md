# AWS SES 이메일 인증 & Redis 기반 Rate Limiter

## 한 줄 요약

AWS SES로 회원가입 이메일 인증을 보내되, **아무나 무제한으로 메일을 트리거하지 못하도록**
Redis 기반 **3중 방어 Rate Limiter**를 붙였다.
① 재전송 쿨다운, ② 일일 전송 횟수 제한, ③ 인증 코드 검증 실패 횟수 제한.
Rate Limiter는 **파라미터화된 재사용 컴포넌트**로 분리해 인증 메일·비밀번호 재설정 등에 공통 적용한다.

---

## 왜 Rate Limiter가 필요했나

이메일 발송 API는 **인증 전(비로그인) 상태에서 열려 있는 엔드포인트**다. 방치하면:

| 위협 | 설명 |
|------|------|
| **메일 폭탄** | 특정 이메일 주소로 대량 인증 메일을 발송시켜 피해자를 괴롭힘 |
| **비용 남용** | SES는 발송량 과금 → 무제한 호출은 곧 비용 공격 |
| **발송 평판(reputation) 하락** | 과도한 발송/바운스는 SES 계정의 전송 평판을 떨어뜨려 정상 메일까지 막힘 |
| **인증 코드 브루트포스** | 6자리 숫자 코드를 무한 시도로 뚫으려는 공격 |

→ "발송"과 "검증" 양쪽 모두에 **속도·횟수 제한**이 필요했다.

---

## 전체 흐름

```text
[발송] sendCode(email)
  1. 이메일 포맷·도메인 검증 (실배송 가능 주소인가)
  2. 이미 가입된 이메일인지 확인
  3. Rate Limiter enforce  ── 쿨다운 + 일일 횟수 검사 (Redis)
  4. SecureRandom 6자리 코드 생성 → Redis 저장 (TTL 10분)
  5. AWS SES 발송 (Thymeleaf 템플릿)
  6. 검증 실패 카운터 초기화

[검증] verifyCode(email, code)
  1. Redis에서 코드 조회 (없으면 만료)
  2. 불일치 → 실패 카운터 +1
       └ 5회 이상 → 코드 삭제 + 차단 (브루트포스 차단)
  3. 일치 → 코드 삭제 + 실패 카운터 초기화
```

---

## 핵심 1 — 재사용 가능한 Rate Limiter 컴포넌트

쿨다운과 일일 횟수 제한을 **파라미터로 받는 별도 컴포넌트**로 분리했다.
덕분에 인증 메일뿐 아니라 비밀번호 재설정 등 **다른 발송 시나리오에서도 키 프리픽스/한도만 바꿔 재사용**할 수 있다.

```java
@Component
@RequiredArgsConstructor
public class EmailRateLimiter {

    private final StringRedisTemplate stringRedisTemplate;

    public void enforce(String email,
                        String cooldownKeyPrefix, String dailyKeyPrefix,
                        Duration cooldown, Duration dailyTtl, int maxDailyCount) {
        String hashedEmail = hashEmail(email);

        // ① 쿨다운: SETNX + TTL — 원자적 단일 연산
        Boolean cooldownApplied = stringRedisTemplate.opsForValue()
                .setIfAbsent(cooldownKeyPrefix + hashedEmail, "1", cooldown);
        if (!Boolean.TRUE.equals(cooldownApplied)) {
            throw new UserHandler(ErrorStatus._TOO_MANY_REQUESTS);
        }

        // ② 일일 횟수: INCR 후 첫 증가 시에만 TTL 부여
        String dailyCountKey = dailyKeyPrefix + hashedEmail;
        Long dailyCount = stringRedisTemplate.opsForValue().increment(dailyCountKey);
        if (dailyCount == 1L) {
            stringRedisTemplate.expire(dailyCountKey, dailyTtl);
        }
        if (dailyCount > maxDailyCount) {
            throw new UserHandler(ErrorStatus._TOO_MANY_REQUESTS);
        }
    }
}
```

### 설계 포인트

- **쿨다운을 `setIfAbsent`(SETNX)로 구현** — "키가 없을 때만 세팅 + TTL"을 **단일 원자 연산**으로 처리.
  조회 후 세팅하는 2단계가 아니라 race condition 없이 "쿨다운 중인지"를 판정한다.
  이미 키가 있으면(= 쿨다운 진행 중) 바로 `429 Too Many Requests`.
- **일일 카운터는 `INCR` + 첫 증가 시에만 `EXPIRE`** — 카운트를 올리는 것과 만료를 거는 것을 분리해,
  하루 단위 TTL이 매 요청마다 갱신되어 창이 밀리는 문제를 막았다. (자정 롤오버가 아닌 "첫 발송 후 24시간" 슬라이딩 윈도우)
- **이메일은 SHA-256 해시를 Redis 키로 사용** — 개인정보(이메일 평문)를 Redis에 그대로 저장하지 않는다.
  소문자 정규화 후 해시하므로 `A@x.com` / `a@x.com` 이 같은 키로 취급된다.

```java
public static String hashEmail(String email) {
    MessageDigest md = MessageDigest.getInstance("SHA-256");
    byte[] digest = md.digest(email.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8));
    return HexFormat.of().formatHex(digest);
}
```

적용 한도(현재 값):

| 항목 | 값 |
|------|-----|
| 재전송 쿨다운 | 60초 |
| 일일 최대 발송 | 5회 / 이메일 |
| 인증 코드 TTL | 10분 |
| 검증 실패 허용 | 5회 |

---

## 핵심 2 — 인증 코드 브루트포스 차단 (검증 단계)

6자리 숫자 코드는 이론상 100만 분의 1이지만, **무한 시도를 허용하면 뚫린다.**
검증 실패 시 실패 카운터를 올리고 **5회 초과 시 코드 자체를 폐기**해 재발급을 강제한다.

```java
public void verifyCode(String email, String code) {
    String hashedEmail = EmailRateLimiter.hashEmail(email);
    EmailVerificationCache cache = emailVerificationRedisRepository.findById(hashedEmail)
            .orElseThrow(() -> new UserHandler(UserErrorStatus._EXPIRED_VERIFICATION_CODE));

    if (!Objects.equals(cache.getCode(), code)) {
        long failureCount = increaseVerifyFailureCount(hashedEmail);
        if (failureCount >= MAX_VERIFY_FAILURE_COUNT) {   // 5회
            emailVerificationRedisRepository.deleteById(hashedEmail); // 코드 폐기
            resetVerifyFailureCount(hashedEmail);
            throw new UserHandler(UserErrorStatus._EXPIRED_VERIFICATION_CODE);
        }
        throw new UserHandler(UserErrorStatus._VERIFICATION_FAILED);
    }

    // 성공: 코드 1회용 소비 + 실패 카운터 초기화
    emailVerificationRedisRepository.deleteById(hashedEmail);
    resetVerifyFailureCount(hashedEmail);
}
```

- 실패 카운터도 **첫 실패 시에만 TTL(10분) 부여** → 코드 유효시간과 수명을 맞춤.
- 코드는 **1회용**: 성공하든 실패 한도를 넘든 Redis에서 즉시 삭제.
- 코드는 `SecureRandom`으로 생성 — 예측 가능한 `Random` 대신 암호학적 난수 사용.

---

## 핵심 3 — 발송 자체는 AWS SES + Thymeleaf

발송 책임은 `EmailService`로 분리했다. AWS SES v2 SDK로 HTML 메일을 전송하고,
본문은 Thymeleaf 템플릿으로 렌더링한다.

```java
String htmlContent = templateEngine.process("email/email-verification", context);
sesV2Client.sendEmail(SendEmailRequest.builder()
        .fromEmailAddress(sesProperties.from())
        .destination(Destination.builder().toAddresses(toEmail).build())
        .content(EmailContent.builder().simple(...).build())
        .build());
```

- **실배송 가능 주소 검증**(`EmailDomainValidator`)을 발송 전에 두어, 존재하지 않는 도메인으로의
  발송 → 바운스 → SES 평판 하락 경로를 사전에 차단.
- SES 예외(`SdkException`)는 도메인 예외(`_SEND_MAIL_FAILED`)로 변환해 일관된 응답 제공.

---

## 책임 분리 구조

| 컴포넌트 | 책임 |
|----------|------|
| `EmailVerificationService` | 인증 유스케이스 오케스트레이션 (검증·중복확인·코드 발급/검증) |
| `EmailRateLimiter` | 쿨다운·일일 횟수 제한 (재사용 컴포넌트) |
| `EmailDomainValidator` | 실배송 가능 이메일 주소 검증 |
| `EmailService` | AWS SES 발송 + Thymeleaf 렌더링 |
| Redis | 인증 코드/카운터 저장 (전부 TTL 기반, 무상태 서버) |

---

## 무엇을 방어했나 — 정리

| 방어 대상 | 메커니즘 | 한도 |
|-----------|----------|------|
| 연타·재전송 남용 | 쿨다운 (SETNX + TTL) | 60초 |
| 메일 폭탄 / 비용 남용 | 일일 발송 카운터 (INCR + TTL) | 5회/일 |
| 코드 브루트포스 | 검증 실패 카운터 + 코드 폐기 | 5회 |
| 개인정보 노출 | 이메일 SHA-256 해시를 키로 사용 | — |
| 바운스로 인한 평판 하락 | 발송 전 도메인 검증 | — |

**결론:** 비로그인 상태로 열려 있는 이메일 발송 엔드포인트에 대해,
Redis의 원자적 연산(SETNX·INCR)과 TTL만으로 **서버 상태를 두지 않고** 발송·검증 양쪽을 방어했고,
제한 로직을 파라미터화된 컴포넌트로 분리해 **다른 발송 기능에도 재사용 가능한 구조**로 만들었다.

---

## 사용 기술

`AWS SES v2 SDK` · `Redis (StringRedisTemplate: SETNX / INCR / EXPIRE)` · `Thymeleaf` ·
`SecureRandom` · `SHA-256` · `Spring @Component 재사용 컴포넌트`
