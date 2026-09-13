package com.umc.linkyou.batch.curation;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.JPAExpressions;
import com.umc.linkyou.batch.common.QuerydslPagingItemReader;
import com.umc.linkyou.domain.QAlarm;
import com.umc.linkyou.domain.QAlarmSetting;
import com.umc.linkyou.domain.QCuration;
import com.umc.linkyou.domain.QUsers;
import com.umc.linkyou.domain.enums.AlarmType;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.stereotype.Component;

import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;

/**
 * 큐레이션 알림 발송 배치 reader
 *
 * <p>이번 달(이슈 월 N)에 생성된 큐레이션 중, CURATION_UPDATED 알림이 아직 발송되지 않은 건만 조회
 * 발송 여부는 Alarm 테이블에 (alarmType, targetId=curationId) 조합이 존재하는지로 판단하므로,
 * 이전 실행에서 알림 발송에 실패한 유저도 다음 실행에서 다시 조회 대상이 됨
 */
@Component("curationAlarmItemReader")
public class CurationAlarmItemReader extends QuerydslPagingItemReader<CurationAlarmCandidate> {

    public CurationAlarmItemReader(EntityManagerFactory entityManagerFactory) {
        super("curationAlarmItemReader", 100, entityManagerFactory);
    }

    @Override
    protected List<CurationAlarmCandidate> fetchQuery(Long lastId, int pageSize) {
        QCuration curation = QCuration.curation;
        QUsers users = QUsers.users;
        QAlarm alarm = QAlarm.alarm;
        QAlarmSetting alarmSetting = QAlarmSetting.alarmSetting;
        String baseMonth = YearMonth.now(ZoneId.of("Asia/Seoul")).toString();

        return queryFactory
                .select(Projections.constructor(
                        CurationAlarmCandidate.class,
                        curation.curationId,
                        users.id,
                        users.nickName))
                .from(curation)
                .join(curation.user, users)
                .join(alarmSetting).on(alarmSetting.user.eq(users))
                .where(
                        curation.curationId.gt(lastId),
                        curation.baseMonth.eq(baseMonth),
                        alarmSetting.alarmAllEnabled.isTrue(),
                        alarmSetting.curationEnabled.isTrue(),
                        JPAExpressions.selectOne()
                                .from(alarm)
                                .where(
                                        alarm.alarmType.eq(AlarmType.CURATION_UPDATED),
                                        alarm.targetId.eq(curation.curationId)
                                )
                                .notExists()
                )
                .orderBy(curation.curationId.asc())
                .limit(pageSize)
                .fetch();
    }

    @Override
    protected Long getLastId(CurationAlarmCandidate item) {
        return item.curationId();
    }
}
