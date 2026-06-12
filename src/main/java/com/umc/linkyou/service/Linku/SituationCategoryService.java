package com.umc.linkyou.service.Linku;

import com.umc.linkyou.domain.mapping.SituationCategory;
import com.umc.linkyou.repository.mapping.SituationCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SituationCategoryService{
    private final SituationCategoryRepository situationCategoryRepository;

    public List<Long> getCategoryIdsBySituation(Long situationId) {
        List<SituationCategory> mappings = situationCategoryRepository.findBySituation_Id(situationId);
        return mappings.stream()
                .map(mapping -> mapping.getCategory().getCategoryId())
                .distinct()
                .collect(Collectors.toList());
    }
}
