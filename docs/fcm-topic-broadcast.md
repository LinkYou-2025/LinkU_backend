# FCM Topic 구독 기반 멀티캐스트 전송 — 공지사항은 왜 Topic 방식이 나았나

## 한 줄 요약

전체 사용자 대상 **공지사항**은 서버가 수신자 목록을 들고 직접 뿌리는 대신,
FCM **Topic 구독** 방식으로 전환해 **단 1번의 API 호출로 전체 브로드캐스트**를 처리했다.
개인 알림은 기존처럼 토큰 멀티캐스트(`sendEachForMulticast`)를 유지하고,
**"수신자가 소수로 특정되면 멀티캐스트, 전체/그룹 브로드캐스트면 Topic"** 이라는 기준으로 두 경로를 분리했다.

---

## 배경 — 알림에는 두 가지 성격이 있다

| 구분 | 예시 | 수신자 | 특징 |
|------|------|--------|------|
| 개인 알림 | 폴더 권한 변경, 링크 관련 알림 | 특정 유저 1명(의 여러 기기) | 수신자가 명확히 특정됨 |
| 공지사항(브로드캐스트) | 서비스 공지, 업데이트 안내 | **전체 사용자** | 수신자 수가 크고 가변적 |

초기에는 모든 알림을 동일하게 **"DB에서 대상 토큰을 조회 → 멀티캐스트 전송"** 방식으로 처리했다.
개인 알림에는 잘 맞았지만, **전체 공지에 그대로 적용하면 다음 문제가 생긴다.**

---

## 문제 — 전체 브로드캐스트를 멀티캐스트로 하면?

멀티캐스트(`sendEachForMulticast`)로 전체 공지를 보내려면:

1. **전 사용자의 활성 FCM 토큰을 DB에서 전부 조회**해야 한다.
   - 유저가 늘수록 조회 비용·메모리 부담이 선형 증가.
2. FCM 멀티캐스트는 **1회 요청당 토큰 500개 제한**이 있다.
   - 사용자 N명 → `ceil(전체 토큰 수 / 500)` 번 요청을 쪼개서 반복 호출.
3. **수신자 목록 관리 책임이 전부 서버에 있다.**
   - 누가 공지 알림을 켰는지/껐는지 서버가 매번 필터링해서 대상을 계산해야 함.
4. 사용자 증가 = 서버의 팬아웃(fan-out) 부하 증가. **확장성이 사용자 수에 종속**된다.

> 핵심 통증: 공지 1건을 보내는 비용이 **사용자 수에 비례**한다.

---

## 해결 — Topic 구독 기반 브로드캐스트

FCM **Topic**은 "이 토픽을 구독한 모든 기기"에게 **서버가 단 1번 호출**하면
**Firebase가 팬아웃을 대신 처리**해주는 구조다.

```java
// 공지: 대상 목록이 필요 없다. 토픽 하나로 발송 끝.
firebaseMessaging.send(buildTopicMessage("alarm-notice", requestDTO));
```

```java
// 개인 알림: 대상이 특정되므로 기존 멀티캐스트 유지
BatchResponse response = firebaseMessaging.sendEachForMulticast(message);
```

### 구독은 "알림 설정"과 연동된다

사용자가 알림 설정(공지/링크/폴더/큐레이션)을 켜고 끌 때,
해당 기기 토큰을 토픽에 **구독/해지**시킨다. 즉 **수신 대상 필터링을 서버가 아닌 구독 상태가 대신한다.**

```java
// AlarmSettingEventListener — 설정 변경 커밋 후 비동기로 구독 상태 반영
@Async("fcmTaskExecutor")
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void handleAlarmSettingChanged(AlarmSettingChangedEvent event) {
    List<UsersFcmToken> tokens =
        userFcmTokenRepository.findAllActiveAndNotExpiredByUserId(event.userId(), now);
    for (UsersFcmToken token : tokens) {
        fcmSubscriber.updateTopicSubscription(
            token.getFcmToken(), event.topics(), event.shouldSubscribe());
    }
}
```

설정 타입은 토픽 이름으로 매핑된다:

```java
private String resolveTopic(FcmSendRequestDTO requestDTO) {
    return switch (requestDTO.getType().getSettingType()) {
        case NOTICE   -> "alarm-notice";
        case LINK     -> "alarm-link";
        case FOLDER   -> "alarm-folder";
        case CURATION -> "alarm-curation";
        case ALL      -> throw new GeneralException(...); // ALL은 발송 토픽이 아님
    };
}
```

### 발송은 트랜잭션 커밋 이후 비동기로

공지 발송은 DB 트랜잭션이 **정상 커밋된 뒤**(`AFTER_COMMIT`),
전용 스레드풀에서 **비동기**(`@Async`)로 나간다.
→ 알림 전송 실패가 원본 비즈니스 트랜잭션을 롤백시키지 않고, 응답 지연에도 영향을 주지 않는다.

```java
// BroadCastAlarmEventListener
@Async("fcmTaskExecutor")
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void handle(BroadCastAlarmEvent event) throws FirebaseMessagingException {
    fcmPushSender.sendToTopic(FcmSendRequestDTO.of(event.alarmType(), event.targetId()));
}
```

---

## FCM 메시지 페이로드 구조

토픽/토큰 발송 모두 동일한 JSON 구조를 사용한다.

```json
{
  "message": {
    "topic": "alarm-notice",
    "notification": {
      "title": "AlarmType.getTitle()",
      "body": "AlarmMessageRenderer.render(type.getBody(), values)"
    },
    "data": {
      "title": "AlarmType.getTitle()",
      "body": "AlarmMessageRenderer.render(type.getBody(), values)",
      "type": "NOTICE",
      "targetId": "123"
    },
    "android": {
      "notification": {
        "click_action": "notice_icon_click"
      }
    }
  }
}
```

### notification vs data — 왜 둘 다 넣는가

| 필드 | 처리 주체 | 동작 |
|------|-----------|------|
| `notification` 블록 | OS / FCM SDK | 앱이 백그라운드/종료 상태일 때 OS가 자동으로 알림창 표시 |
| `data` 블록 | 앱 코드 | 포그라운드/백그라운드 구분 없이 앱이 직접 파싱해서 처리 |

둘 중 하나만 쓰면:
- `notification`만: 앱이 포그라운드일 때 알림을 직접 핸들링할 수 없다.
- `data`만: 백그라운드/종료 상태에서는 OS가 알림창을 자동 표시하지 않는다.

**두 블록을 모두 포함해야 포그라운드·백그라운드·종료 상태 모두에서 알림이 정상 작동한다.**

### data 필드 상세

| 키 | 값 출처 | 설명 |
|----|---------|------|
| `title` | `AlarmType.getTitle()` | 알림 제목 (AlarmType enum에 정의된 고정 문자열) |
| `body` | `AlarmMessageRenderer.render(...)` | 알림 본문 (`%s` placeholder가 있으면 values로 치환) |
| `type` | `AlarmType.name()` | 알림 유형 (앱에서 탭 이동 등 분기 처리에 사용) |
| `targetId` | `requestDTO.getTargetId()` | 탭했을 때 이동할 대상 리소스의 ID |

`data.body`의 getter가 `getMessage()`인 이유: body라는 용어가 이미 FCM 필드명으로 예약되어 있어 혼동을 피하기 위한 네이밍이다.

---

## 왜 공지사항에 Topic이 나은가 — 정리

| 관점 | 멀티캐스트(개인 알림) | **Topic(공지사항)** |
|------|----------------------|---------------------|
| API 호출 횟수 | 토큰 500개당 1회 → 여러 번 | **항상 1회** (수신자 수 무관) |
| 수신자 목록 | 서버가 DB 조회 후 보유 | **서버가 목록을 몰라도 됨** (Firebase가 구독 관리) |
| 팬아웃 부하 | 서버가 부담 | **Firebase가 대신 처리** |
| 확장성 | 사용자 수에 비례해 비용 증가 | **사용자 수와 무관하게 O(1) 발송** |
| 수신 대상 필터링 | 발송 시점에 서버가 계산 | **구독 상태(설정)로 사전 결정** |
| DB 부하 | 전체 토큰 조회 필요 | **조회 불필요** |

**결론:** 수신자가 크고 가변적인 전체 공지에서는
"보내는 비용이 사용자 수에 비례"하는 멀티캐스트보다,
**호출 1번으로 끝나고 팬아웃을 Firebase에 위임하는 Topic** 방식이
성능·확장성·서버 책임 분리 측면에서 명확히 우월했다.

---

## 설계에서 신경 쓴 점

- **경로 분리**: `FcmPushSender`(발송)와 `FcmSubscriber`(구독)를 인터페이스로 분리해,
  개인 멀티캐스트 / 토픽 브로드캐스트 / 구독 관리 책임을 명확히 나눴다.
- **죽은 토큰 정리**: 멀티캐스트 응답에서 `UNREGISTERED`·`INVALID_ARGUMENT` 토큰은
  자동 비활성화(`deactivate()`)해 다음 발송 대상에서 제외한다.
- **부분 실패 처리**: 토픽 구독 변경 시 실패한 토픽만 모아 예외로 전달해,
  일부 실패가 전체를 막지 않도록 했다.
- **트랜잭션 안전성**: 모든 발송/구독 반영은 `AFTER_COMMIT` + `@Async`로 처리해
  알림 사이드이펙트가 원본 트랜잭션과 응답 경로에서 분리되도록 했다.

---

## 사용 기술

`Firebase Admin SDK (FCM)` · `FCM Topic Messaging` · `Spring `@Async`` · `@TransactionalEventListener(AFTER_COMMIT)`
