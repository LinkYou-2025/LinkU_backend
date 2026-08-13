package com.umc.linkyou.batch.curation;

import com.querydsl.jpa.JPAExpressions;
import com.umc.linkyou.batch.common.QuerydslPagingItemReader;
import com.umc.linkyou.domain.QCuration;
import com.umc.linkyou.domain.QUsers;
import com.umc.linkyou.domain.Users;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.stereotype.Component;

import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;

@Component("curationItemReader")
public class CurationItemReader extends QuerydslPagingItemReader<Users> {

    public CurationItemReader(EntityManagerFactory entityManagerFactory) {
        super("curationItemReader", 100, entityManagerFactory);
    }

    @Override
    protected List<Users> fetchQuery(Long lastId, int pageSize) {
        QUsers users = QUsers.users;
        QCuration curation = QCuration.curation;
        String baseMonth = YearMonth.now(ZoneId.of("Asia/Seoul")).toString();

        return queryFactory.selectFrom(users)
                .where(
                        users.id.gt(lastId),
                        JPAExpressions.selectOne()
                                .from(curation)
                                .where(
                                        curation.user.eq(users),
                                        curation.baseMonth.eq(baseMonth)
                                )
                                .notExists()
                )
                .orderBy(users.id.asc())
                .limit(pageSize)
                .fetch();
    }

    @Override
    protected Long getLastId(Users item) {
        return item.getId();
    }
}
