package com.umc.linkyou.service.Linku;

import com.umc.linkyou.repository.UserLinkuRepository.UsersLinkuRepository;
import com.umc.linkyou.repository.linkuRepository.LinkuRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class LinkuViewService {

    private final UsersLinkuRepository usersLinkuRepository;
    private final LinkuRepository linkuRepository;

    @Transactional
    public void recordView(Long userLinkuId, Long linkuId) {
        usersLinkuRepository.incrementViewCount(userLinkuId, LocalDateTime.now());
        linkuRepository.incrementTotalViewCount(linkuId);
    }
}
