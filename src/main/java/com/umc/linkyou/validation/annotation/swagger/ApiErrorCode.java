package com.umc.linkyou.validation.annotation.swagger;

import com.umc.linkyou.apiPayload.code.status.CommonErrorStatus;
import com.umc.linkyou.apiPayload.code.status.aiarticle.AiArticleErrorStatus;
import com.umc.linkyou.apiPayload.code.status.ErrorStatus;
import com.umc.linkyou.apiPayload.code.status.alarm.AlarmErrorStatus;
import com.umc.linkyou.apiPayload.code.status.auth.AuthErrorStatus;
import com.umc.linkyou.apiPayload.code.status.curation.CurationErrorStatus;
import com.umc.linkyou.apiPayload.code.status.folder.FolderErrorStatus;
import com.umc.linkyou.apiPayload.code.status.folder.InvitationErrorStatus;
import com.umc.linkyou.apiPayload.code.status.folder.ShareFolderErrorStatus;
import com.umc.linkyou.apiPayload.code.status.linku.LinkuErrorStatus;
import com.umc.linkyou.apiPayload.code.status.user.UserErrorStatus;
import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(ApiErrorCodes.class)
public @interface ApiErrorCode {
    ErrorStatus[] errorStatus() default {};
    UserErrorStatus[] userErrorStatus() default {};
    AlarmErrorStatus[] alarmErrorStatus() default {};
    AiArticleErrorStatus[] aiArticleErrorStatus() default {};
    AuthErrorStatus[] authErrorStatus() default {};
    CommonErrorStatus[] commonErrorStatus() default {};
    CurationErrorStatus[] curationErrorStatus() default {};
    LinkuErrorStatus[] linkuErrorStatus() default {};
    FolderErrorStatus[] folderErrorStatus() default {};
    ShareFolderErrorStatus[] shareFolderErrorStatus() default {};
    InvitationErrorStatus[] invitationErrorStatus() default {};
}
