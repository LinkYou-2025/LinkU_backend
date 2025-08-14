package com.umc.linkyou.repository.FolderRepository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.umc.linkyou.domain.classification.Category;
import com.umc.linkyou.domain.folder.Folder;
import com.umc.linkyou.repository.FolderRepository.FolderRepositoryCustom;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import static com.umc.linkyou.domain.folder.QFolder.folder;

@Repository
@RequiredArgsConstructor
public class FolderRepositoryImpl implements FolderRepositoryCustom {
}
