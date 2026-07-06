package com.umc.linkyou.domain;

import com.umc.linkyou.domain.enums.AlarmPlaceholder;

import java.util.Map;

/**
 * 알림 본문 placeholder 치환에 필요한 값들을 담는 payload
 * - 매직 스트링 제거: 키는 {@link AlarmPlaceholder} 에서만 정의
 * - 값 누락을 컴파일타임에 차단 (record 생성자 강제)
 * 렌더러 경계에서만 {@link #toValues()} 로 Map 으로 변환
 */
public sealed interface AlarmPayload {

    // placeholder, 필요한 정보
    Map<String, String> toValues();

    /** placeholder 가 없는 알림 (공지 등) */
    record Empty() implements AlarmPayload {
        @Override
        public Map<String, String> toValues() {
            return Map.of();
        }
    }

    /** {linkTitle} 만 필요 (LINK_SUMMARY_COMPLETE) */
    record LinkTitle(String linkTitle) implements AlarmPayload {
        @Override
        public Map<String, String> toValues() {
            return Map.of(AlarmPlaceholder.LINK_TITLE.key(), linkTitle);
        }
    }

    /** {nickname} 만 필요 (CURATION_UPDATED) */
    record Nickname(String nickname) implements AlarmPayload {
        @Override
        public Map<String, String> toValues() {
            return Map.of(AlarmPlaceholder.NICKNAME.key(), nickname);
        }
    }

    /** {folderName} 만 필요 (FOLDER_PERMISSION_CHANGED) */
    record FolderName(String folderName) implements AlarmPayload {
        @Override
        public Map<String, String> toValues() {
            return Map.of(AlarmPlaceholder.FOLDER_NAME.key(), folderName);
        }
    }

    /** {nickname} + {folderName} 필요 (SHARED_FOLDER_INVITE, FOLDER_DELETED) */
    record NicknameAndFolder(String nickname, String folderName) implements AlarmPayload {
        @Override
        public Map<String, String> toValues() {
            return Map.of(
                    AlarmPlaceholder.NICKNAME.key(), nickname,
                    AlarmPlaceholder.FOLDER_NAME.key(), folderName
            );
        }
    }
}
