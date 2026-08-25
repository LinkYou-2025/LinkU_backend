package com.umc.linkyou.infra.ai;

import com.umc.linkyou.domain.classification.Category;
import com.umc.linkyou.domain.classification.Emotion;
import com.umc.linkyou.domain.classification.Situation;
import com.umc.linkyou.infra.ai.dto.LinkuResultDTO;
import org.jsoup.nodes.Document;

import java.util.List;
import java.util.Optional;

public interface AiLinkuAnalyzer {
    Optional<LinkuResultDTO> analyzeByUrl(String url, Document doc, List<Category> categories, List<Situation> situations, List<Emotion> emotions);
}
