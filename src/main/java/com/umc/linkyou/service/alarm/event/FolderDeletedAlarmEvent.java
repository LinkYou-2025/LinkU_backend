package com.umc.linkyou.service.alarm.event;

import java.util.List;

public record FolderDeletedAlarmEvent(
        Long folderId,
        List<Long> memberIds,
        String deleterNickname,
        String folderName
) {}