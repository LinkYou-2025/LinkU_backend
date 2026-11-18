package com.umc.linkyou.repository.mapping.linkuFolderRepository;

import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.springframework.stereotype.Repository;

import com.umc.linkyou.domain.folder.Folder;
import com.umc.linkyou.domain.mapping.LinkuFolder;

@Repository
public class LinkuFolderRepositoryImpl implements LinkuFolderRepositoryCustom {
    @PersistenceContext private EntityManager em;

    @Override
    public List<LinkuFolder> findByFolder(Folder folder) {
        return em.createQuery(
                        "SELECT lf FROM LinkuFolder lf WHERE lf.folder = :folder",
                        LinkuFolder.class)
                .setParameter("folder", folder)
                .getResultList();
    }
}
