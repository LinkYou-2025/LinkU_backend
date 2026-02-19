package com.umc.linkyou.domain.enums;

import com.umc.linkyou.apiPayload.code.status.ErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;

public enum TermsType {
    TERMS_OF_USE,        // 이용약관
    PRIVACY_POLICY,      // 개인정보처리방침
    MARKETING;          // 마케팅수신동의
    public static TermsType fromString(String value) {
        try{
            return TermsType.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new GeneralException(ErrorStatus.INVALID_TERMS_TYPE);
        }
    }
}
