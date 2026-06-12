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
import com.umc.linkyou.support.config.TestExternalConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

// 2. Java Util 클래스 import
import java.util.Optional;


@ActiveProfiles("test")
@SpringBootTest
@Import(TestExternalConfig.class)
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

    @Test
    @DisplayName("존재하지 않는 사용자 ID로 즉시 삭제 호출 시 GeneralException이 발생해야 한다")
    void testImmediateDelete_UserNotFound_ThrowsException() {
        // given
        Long nonExistentId = 9999L;

        // when & then
        assertThrows(com.umc.linkyou.apiPayload.exception.GeneralException.class, () -> {
            userWithdrawService.testImmediateDelete(nonExistentId);
        });
    }

    @Test
    @DisplayName("복합 연관 관계(Linku, Folder 등)가 있는 사용자도 에러 없이 완전 삭제되어야 한다")
    void testImmediateDelete_WithComplexAssociations_Success() {
        // given
        // 1. 이미 setUp에서 생성된 유저를 가져옴
        Users user = userRepository.findById(testUserId).get();

        // 2. 복합 연관 데이터 추가 (예: 알람, 카테고리 컬러 등 - 빌더나 필드에 따라 조정)
        // 리스트가 초기화되어 있다고 가정하고 데이터를 넣습니다.
        // user.getUserAlarms().add(UserAlarm.builder().user(user).content("테스트알람").build());

        // 영속성 컨텍스트 반영
        userRepository.save(user);
        em.flush();
        em.clear();

        // when
        assertDoesNotThrow(() -> userWithdrawService.testImmediateDelete(testUserId));
        em.flush();
        em.clear();

        // then
        // 부모가 삭제되었는지 확인
        assertFalse(userRepository.existsById(testUserId));

        // 자식(AuthAccount)도 함께 삭제되었는지 다시 확인
        Optional<AuthAccount> authAccount = authAccountRepository.findByProviderAndExternalId(Provider.GENERAL, "test@linku.com");
        assertTrue(authAccount.isEmpty(), "OrphanRemoval 또는 clear() 처리가 정상적으로 동작하지 않았습니다.");
    }

    @Test
    @DisplayName("카카오 웹훅 호출 시 연관 계정이 없으면 유저 상태가 INACTIVE로 변경되어야 한다")
    void handleKakaoUnlinkWebhook_NoOtherAccounts_WithdrawsUser() {
        // given
        em.flush();
        em.clear();

        // 1. 유저와 연관된 기존 계정들을 모두 가져옴
        Users user = userRepository.findById(testUserId).get();

        // 2. [수정 포인트] 레포지토리에 메서드가 없으므로, 리스트를 순회하며 삭제하거나 deleteAll 사용
        if (user.getAuthAccounts() != null && !user.getAuthAccounts().isEmpty()) {
            authAccountRepository.deleteAll(user.getAuthAccounts());
            user.getAuthAccounts().clear(); // 리스트도 비워줌
        }

        em.flush();
        em.clear();

        // 3. 다시 유저 로드 (완전히 비워진 상태)
        user = userRepository.findById(testUserId).get();

        // 4. 이제 테스트 타겟인 '카카오 계정'만 딱 하나 생성
        AuthAccount kakaoAuth = AuthAccount.builder()
                .user(user)
                .provider(Provider.KAKAO)
                .externalId("kakao_12345")
                .email("kakao@test.com")
                .build();

        if (user.getAuthAccounts() == null) user.setAuthAccounts(new java.util.ArrayList<>());
        user.getAuthAccounts().add(kakaoAuth);
        authAccountRepository.save(kakaoAuth);

        em.flush();
        em.clear();

        // when
        userWithdrawService.handleKakaoUnlinkWebhook("kakao_12345");

        em.flush();
        em.clear();

        // then
        Users foundUser = userRepository.findById(testUserId).orElseThrow();

        // 이제 Actual이 INACTIVE로 정상 반영될 거예요!
        assertEquals(UserStatus.INACTIVE, foundUser.getStatus(), "다른 계정이 없으므로 INACTIVE여야 합니다.");

        boolean exists = authAccountRepository.existsByUserIdAndProvider(testUserId, Provider.KAKAO);
        assertFalse(exists, "카카오 연동 정보는 DB에서 삭제되어야 합니다.");
    }
}
