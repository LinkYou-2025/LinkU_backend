package com.umc.linkyou.repository;

import com.umc.linkyou.domain.folder.FolderShareLink;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface FolderShareLinkRepository extends JpaRepository<FolderShareLink, Long> {
    // 유효한 토큰 찾기 (토큰값 + 활성상태 + 만료기간)
    Optional<FolderShareLink> findByToken(String token);

    // 해당 폴더에 이미 활성화된 링크가 있는지 확인 (중복 생성 방지용)
    Optional<FolderShareLink> findByFolder_FolderIdAndIsActiveTrue(Long folderId);
}