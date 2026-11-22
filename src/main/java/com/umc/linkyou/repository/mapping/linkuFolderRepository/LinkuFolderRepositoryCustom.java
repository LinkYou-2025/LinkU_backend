package com.umc.linkyou.repository.mapping.linkuFolderRepository;

import java.util.List;

import com.umc.linkyou.domain.folder.Folder;
import com.umc.linkyou.domain.mapping.LinkuFolder;

public interface LinkuFolderRepositoryCustom {
    List<LinkuFolder> findByFolder(Folder folder);
}
