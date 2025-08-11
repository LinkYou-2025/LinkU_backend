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

    private final JPAQueryFactory queryFactory;

    @Override
    public boolean existsDuplicateFolder(String folderName, Folder parentFolder, Category category) {
        return queryFactory.selectOne()
                .from(folder)
                .where(
                        folder.folderName.eq(folderName),
                        folder.parentFolder.eq(parentFolder),
                        folder.category.eq(category)
                )
                .fetchFirst() != null;
    }
}
