package com.umc.linkyou.service.curation;

import com.umc.linkyou.domain.Curation;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.mapping.CurationLike;
import com.umc.linkyou.repository.CurationRepository;
import com.umc.linkyou.repository.userRepository.UserRepository;
import com.umc.linkyou.repository.mapping.CurationLikeRepository;
import com.umc.linkyou.web.dto.curation.CurationListResponse;
import com.umc.linkyou.web.dto.curation.LikedCurationResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import com.umc.linkyou.web.dto.curation.CurationListResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CurationLikeServiceImpl implements CurationLikeService {

    private final CurationLikeRepository curationLikeRepository;
    private final CurationRepository curationRepository;
    private final UserRepository userRepository;

    @Override
    public void likeCuration(Long userId, Long curationId) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자 없음"));
        Curation curation = curationRepository.findById(curationId)
                .orElseThrow(() -> new IllegalArgumentException("큐레이션 없음"));

        if (curationLikeRepository.existsByUserAndCuration(user, curation)) {
            throw new IllegalStateException("이미 좋아요를 눌렀습니다.");
        }

        CurationLike like = CurationLike.builder()
                .user(user)
                .curation(curation)
                .build();

        curationLikeRepository.save(like);
    }

    @Override
    public void unlikeCuration(Long userId, Long curationId) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자 없음"));
        Curation curation = curationRepository.findById(curationId)
                .orElseThrow(() -> new IllegalArgumentException("큐레이션 없음"));

        CurationLike like = curationLikeRepository.findByUserAndCuration(user, curation)
                .orElseThrow(() -> new IllegalStateException("좋아요를 누르지 않았습니다."));

        curationLikeRepository.delete(like);
    }

    @Override
    public boolean isLiked(Long userId, Long curationId) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자 없음"));
        Curation curation = curationRepository.findById(curationId)
                .orElseThrow(() -> new IllegalArgumentException("큐레이션 없음"));

        return curationLikeRepository.existsByUserAndCuration(user, curation);
    }

    @Override
    public List<LikedCurationResponse> getRecentLikedCurations(Long userId) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자 없음"));

        return curationLikeRepository.findTop6ByUserOrderByCreatedAtDesc(user).stream()
                .map(like -> {
                    Curation c = like.getCuration();
                    return new LikedCurationResponse(
                            c.getCurationId(),
                            c.getMonth(),
                            c.getThumbnailUrl()
                    );
                })
                .toList();
    }

    // [▼▼▼ 메서드 추가: 좋아요한 큐레이션 전체 보기]
    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public Page<CurationListResponse> getLikedCurationList(Long userId, Pageable pageable) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // Repository에서 데이터 가져오기 (Curation 정보 포함)
        Page<CurationLike> likePage = curationLikeRepository.findAllByUserOrderByCreatedAtDesc(user, pageable);

        // CurationLike -> Curation -> DTO 변환
        return likePage.map(like -> {
            Curation c = like.getCuration();
            return CurationListResponse.builder()
                    .curationId(c.getCurationId())
                    .month(c.getMonth())
                    .thumbnailUrl(c.getThumbnailUrl())
                    .build();
        });
    }

}
