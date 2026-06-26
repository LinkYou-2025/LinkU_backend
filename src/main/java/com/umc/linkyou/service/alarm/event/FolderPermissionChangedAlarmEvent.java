package com.umc.linkyou.service.alarm.event;

public record FolderPermissionChangedAlarmEvent(
        Long memberId,
        Long folderId,
        String folderName
) {}
