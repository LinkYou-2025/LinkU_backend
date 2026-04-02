package com.umc.linkyou.service.curation;

import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.umc.linkyou.domain.log.CurationTopLog;
import com.umc.linkyou.domain.log.QEmotionLog;
import com.umc.linkyou.domain.log.QSituationLog;
import com.umc.linkyou.domain.enums.CurationTopLogType;
import com.umc.linkyou.domain.Curation;
import com.umc.linkyou.repository.LogRepository.CurationTopLogRepository;
import com.umc.linkyou.repository.LogRepository.EmotionLogRepository;
import com.umc.linkyou.repository.LogRepository.SituationLogRepository;
import com.umc.linkyou.repository.EmotionRepository;
import com.umc.linkyou.repository.mapping.SituationJobRepository;
import com.umc.linkyou.web.dto.curation.CurationTopLogDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CurationTopLogServiceImpl implements CurationTopLogService {

    private final CurationTopLogRepository curationTopLogRepository;
    private final EmotionLogRepository emotionLogRepository;
    private final SituationLogRepository situationLogRepository;
    private final EmotionRepository emotionRepository;
    private final SituationJobRepository situationJobRepository;
    private final JPAQueryFactory queryFactory;

    @Override
    public List<String> getTopTagNamesByCuration(Long curationId) {
        return curationTopLogRepository.findTop3ByCurationId(curationId).stream()
                .map(CurationTopLog::getTagName)
                .collect(Collectors.toList());
    }

    @Transactional
    public void calculateAndSaveTopLogs(Long userId, Curation curation) {
        QEmotionLog emotionLog = QEmotionLog.emotionLog;
        QSituationLog situationLog = QSituationLog.situationLog;

        // 1. 기간 설정 (최근 30일 역할을 하는 해당 월 기준)
        YearMonth yearMonth = YearMonth.parse(curation.getMonth(), DateTimeFormatter.ofPattern("yyyy-MM"));
        LocalDateTime startOfMonth = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime startOfNextMonth = yearMonth.plusMonths(1).atDay(1).atStartOfDay();

        // 2. 감정 로그 집계 (조건 없음)
        List<Tuple> emotionCounts = queryFactory
                .select(emotionLog.emotion.emotionId, emotionLog.count())
                .from(emotionLog)
                .where(
                        emotionLog.user.id.eq(userId),
                        emotionLog.createdAt.goe(startOfMonth),
                        emotionLog.createdAt.lt(startOfNextMonth)
                )
                .groupBy(emotionLog.emotion.emotionId)
                .fetch();

        // 3. 상황 로그 집계 (직업 변경 유저 처리: 현재 직업과 일치하는 것만!)
        // 3. 상황 로그 집계 (직업 변경 유저 처리 및 NPE 방지)
        List<Tuple> situationCounts;

        // 유저가 직업을 설정한 상태인지 먼저 확인!
        if (curation.getUser().getJob() != null) {
            Long currentJobId = curation.getUser().getJob().getId();

            situationCounts = queryFactory
                    .select(situationLog.situationJob.id, situationLog.count())
                    .from(situationLog)
                    .where(
                            situationLog.user.id.eq(userId),
                            situationLog.createdAt.goe(startOfMonth),
                            situationLog.createdAt.lt(startOfNextMonth),
                            // null 체크를 통과한 안전한 jobId를 사용
                            situationLog.situationJob.job.id.eq(currentJobId)
                    )
                    .groupBy(situationLog.situationJob.id)
                    .fetch();
        } else {
            // 직업이 없는 유저라면 상황 로그를 아예 집계하지 않고 빈 리스트 반환
            situationCounts = new ArrayList<>();
        }

        // 4. 감정/상황 통합 리스트 생성
        List<TagScoreDto> allTags = new ArrayList<>();

        for (Tuple row : emotionCounts) {
            Long refId = row.get(emotionLog.emotion.emotionId);
            int count = row.get(emotionLog.count()).intValue();
            String tagName = emotionRepository.findById(refId)
                    .orElseThrow(() -> new IllegalArgumentException("Emotion not found")).getName();
            allTags.add(new TagScoreDto(CurationTopLogType.EMOTION, refId, tagName, count));
        }

        for (Tuple row : situationCounts) {
            Long refId = row.get(situationLog.situationJob.id);
            int count = row.get(situationLog.count()).intValue();
            String tagName = situationJobRepository.findById(refId)
                    .orElseThrow(() -> new IllegalArgumentException("Situation not found"))
                    .getSituation().getName();
            allTags.add(new TagScoreDto(CurationTopLogType.SITUATION, refId, tagName, count));
        }

        // 5. 점수(선택 횟수) 기준 내림차순 정렬 (동점 시 랜덤 처리)
        allTags.sort((t1, t2) -> {
            if (t1.getCount() != t2.getCount()) {
                return Integer.compare(t2.getCount(), t1.getCount()); // 내림차순
            }
            return Math.random() > 0.5 ? 1 : -1; // 동점일 경우 랜덤
        });

        // 6. 예외 처리: 데이터 부족 (유효 태그 2개 미만이면 아예 저장 안 함 -> 화면 미노출)
        if (allTags.size() < 2) {
            return;
        }

        // 7. Top 3 추출 (2개면 2개만 추출)
        List<TagScoreDto> top3Tags = new ArrayList<>();
        for (int i = 0; i < Math.min(3, allTags.size()); i++) {
            top3Tags.add(allTags.get(i));
        }

        // 8. 교체 로직: 타입 비율 제한 (감정만 3개 OR 상황만 3개 방지)
        if (top3Tags.size() == 3) {
            long emotionCount = top3Tags.stream().filter(t -> t.getType() == CurationTopLogType.EMOTION).count();
            long situationCount = top3Tags.stream().filter(t -> t.getType() == CurationTopLogType.SITUATION).count();

            if (emotionCount == 3) {
                // 남은 후보들 중 1등 상황 태그 찾기
                TagScoreDto bestSituation = allTags.stream()
                        .skip(3)
                        .filter(t -> t.getType() == CurationTopLogType.SITUATION)
                        .findFirst()
                        .orElse(null);

                if (bestSituation != null) {
                    top3Tags.remove(2); // 3등 감정 제거
                    top3Tags.add(bestSituation); // 1등 상황 투입
                }
            } else if (situationCount == 3) {
                // 남은 후보들 중 1등 감정 태그 찾기
                TagScoreDto bestEmotion = allTags.stream()
                        .skip(3)
                        .filter(t -> t.getType() == CurationTopLogType.EMOTION)
                        .findFirst()
                        .orElse(null);

                if (bestEmotion != null) {
                    top3Tags.remove(2); // 3등 상황 제거
                    top3Tags.add(bestEmotion); // 1등 감정 투입
                }
            }
        }

        // 9. 최종 선정된 태그들만 DB에 저장
        List<CurationTopLog> logsToSave = top3Tags.stream().map(tag ->
                CurationTopLog.builder()
                        .curation(curation)
                        .type(tag.getType())
                        .refId(tag.getRefId())
                        .count(tag.getCount())
                        .tagName(tag.getTagName())
                        .build()
        ).collect(Collectors.toList());

        curationTopLogRepository.saveAll(logsToSave);
    }

    @Override
    public List<CurationTopLogDTO> getTopLogDtoByCuration(Long curationId) {
        // 기존 코드 유지 (어차피 최대 3개만 저장되어 있으므로 그대로 리턴하면 됨)
        return curationTopLogRepository.findTop3ByCurationId(curationId).stream()
                .map(log -> CurationTopLogDTO.builder()
                        .type(log.getType())
                        .tagName(log.getTagName())
                        .count(log.getCount())
                        .build())
                .collect(Collectors.toList());
    }

    // 내부에서 랭킹 계산용으로 쓸 임시 DTO 클래스
    @Getter
    @AllArgsConstructor
    private static class TagScoreDto {
        private CurationTopLogType type;
        private Long refId;
        private String tagName;
        private int count;
    }
}