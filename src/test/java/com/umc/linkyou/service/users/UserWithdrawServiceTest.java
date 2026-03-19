package com.umc.linkyou.service.users;

import com.umc.linkyou.domain.AuthAccount;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.enums.Provider;
import com.umc.linkyou.domain.enums.UserStatus;
import com.umc.linkyou.repository.authAccountRepository.AuthAccountRepository;
import com.umc.linkyou.repository.userRepository.UserRepository;
import com.umc.linkyou.web.dto.UserRequestDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

// 2. Java Util 클래스 import
import java.util.Optional;

@SpringBootTest
@Transactional
class UserWithdrawServiceTest {

    @Autowired
    private UserWithdrawService userWithdrawService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthAccountRepository authAccountRepository;

    @PersistenceContext
    private EntityManager em;

    private Long testUserId;

    @BeforeEach
    void setUp() {
        // 1. 테스트용 부모 유저 생성
        Users user = Users.builder()
                .nickName("탈퇴테스트유저")
                .password("testPassword123!")
                .status(UserStatus.ACTIVE)
                .build();
        userRepository.save(user);
        testUserId = user.getId();

        // 2. 외래 키 제약을 테스트하기 위해 자식 데이터(AuthAccount) 생성
        authAccountRepository.save(AuthAccount.builder()
                .user(user)
                .provider(Provider.GENERAL)
                .email("test@linku.com")
                .externalId("test@linku.com")
                .build());

        // 영속성 컨텍스트를 DB와 동기화
        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("회원 탈퇴(Soft Delete) 시 상태가 INACTIVE로 변경되어야 한다")
    void withdrawUser_Success() {
        // given
        UserRequestDTO.DeleteReasonDTO reasonDTO = new UserRequestDTO.DeleteReasonDTO();
        reasonDTO.setReason("더 이상 사용하지 않음");

        // when
        userWithdrawService.withdrawUser(testUserId, reasonDTO);
        em.flush();
        em.clear();

        // then
        Users foundUser = userRepository.findById(testUserId).orElseThrow();
        assertEquals(UserStatus.INACTIVE, foundUser.getStatus());
        assertEquals("더 이상 사용하지 않음", foundUser.getDeleted_reason());
        assertNotNull(foundUser.getInactiveDate());
    }

    @Test
    @DisplayName("즉시 삭제(Hard Delete) 호출 시 모든 연관 데이터가 에러 없이 삭제되어야 한다")
    void testImmediateDelete_Success() {
        // when
        // 이 메서드 내부에서 연관관계를 clear()하고 userRepository.delete()를 호출함
        userWithdrawService.testImmediateDelete(testUserId);
        em.flush();
        em.clear();

        // then
        // 1. 유저 삭제 확인
        Optional<Users> deletedUser = userRepository.findById(testUserId);
        assertTrue(deletedUser.isEmpty(), "유저가 DB에서 삭제되지 않았습니다.");

        // 2. 자식 데이터(AuthAccount) 삭제 확인
        boolean authExists = authAccountRepository.existsByUserIdAndProvider(testUserId, Provider.GENERAL);
        assertFalse(authExists, "연관된 AuthAccount 데이터가 삭제되지 않았습니다.");
    }
}