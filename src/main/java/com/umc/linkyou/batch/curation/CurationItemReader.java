package com.umc.linkyou.batch.curation;

import com.umc.linkyou.batch.common.QuerydslPagingItemReader;
import com.umc.linkyou.domain.QUsers;
import com.umc.linkyou.domain.Users;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("curationItemReader")
public class CurationItemReader extends QuerydslPagingItemReader<Users> {

    public CurationItemReader(EntityManagerFactory entityManagerFactory) {
        super("curationItemReader", 100, entityManagerFactory);
    }

    @Override
    protected List<Users> fetchQuery(Long lastId, int pageSize) {
        QUsers users = QUsers.users;

        return queryFactory.selectFrom(users)
                .where(users.id.gt(lastId))
                .orderBy(users.id.asc())
                .limit(pageSize)
                .fetch();
    }

    @Override
    protected Long getLastId(Users item) {
        return item.getId();
    }
}
