package com.umc.linkyou.service.users;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.Optional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.umc.linkyou.domain.AuthAccount;
import com.umc.linkyou.domain.Curation;
import com.umc.linkyou.domain.Linku;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.classification.Category;
import com.umc.linkyou.domain.classification.Domain;
import com.umc.linkyou.domain.classification.Emotion;
import com.umc.linkyou.domain.classification.Interests;
import com.umc.linkyou.domain.classification.Purposes;
import com.umc.linkyou.domain.classification.Situation;
import com.umc.linkyou.domain.enums.Provider;
import com.umc.linkyou.domain.enums.UserStatus;
import com.umc.linkyou.domain.folder.Fcolor;
import com.umc.linkyou.domain.folder.Folder;
import com.umc.linkyou.domain.mapping.CurationLinku;
import com.umc.linkyou.domain.mapping.LinkuFolder;
import com.umc.linkyou.domain.mapping.UsersInterest;
import com.umc.linkyou.domain.mapping.UsersLinku;
import com.umc.linkyou.domain.mapping.UsersPurpose;
import com.umc.linkyou.repository.EmotionRepository;
import com.umc.linkyou.repository.FolderRepository.FolderRepository;
import com.umc.linkyou.repository.UserLinkuRepository.UsersLinkuRepository;
import com.umc.linkyou.repository.authAccountRepository.AuthAccountRepository;
import com.umc.linkyou.repository.categoryRepository.FcolorRepository;
import com.umc.linkyou.repository.classification.CategoryRepository;
import com.umc.linkyou.repository.classification.InterestRepository;
import com.umc.linkyou.repository.classification.PurposeRepository;
import com.umc.linkyou.repository.classification.SituationRepository;
import com.umc.linkyou.repository.classification.UsersInterestRepository;
import com.umc.linkyou.repository.classification.UsersPurposeRepository;
import com.umc.linkyou.repository.classification.domainRepository.DomainRepository;
import com.umc.linkyou.repository.curationRepository.CurationLinkuRepository;
import com.umc.linkyou.repository.curationRepository.CurationRepository;
import com.umc.linkyou.repository.linkuRepository.LinkuRepository;
import com.umc.linkyou.repository.mapping.linkuFolderRepository.LinkuFolderRepository;
import com.umc.linkyou.repository.userRepository.UserRepository;
import com.umc.linkyou.support.config.TestExternalConfig;
import com.umc.linkyou.web.dto.user.UserRequestDTO;

@ActiveProfiles("test")
@SpringBootTest
@Import(TestExternalConfig.class)
@Transactional
class UserWithdrawServiceTest {

    @Autowired private UserWithdrawService userWithdrawService;

    @Autowired private UserRepository userRepository;

    @Autowired private AuthAccountRepository authAccountRepository;

    @Autowired private InterestRepository interestRepository;
    @Autowired private PurposeRepository purposeRepository;
    @Autowired private UsersInterestRepository usersInterestRepository;
    @Autowired private UsersPurposeRepository usersPurposeRepository;

    @Autowired private FcolorRepository fcolorRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private DomainRepository domainRepository;
    @Autowired private EmotionRepository emotionRepository;
    @Autowired private SituationRepository situationRepository;
    @Autowired private LinkuRepository linkuRepository;
    @Autowired private FolderRepository folderRepository;
    @Autowired private UsersLinkuRepository usersLinkuRepository;
    @Autowired private LinkuFolderRepository linkuFolderRepository;
    @Autowired private CurationRepository curationRepository;
    @Autowired private CurationLinkuRepository curationLinkuRepository;

    @PersistenceContext private EntityManager em;

    private Long testUserId;

    @BeforeEach
    void setUp() {
        // 1. 테스트용 부모 유저 생성
        Users user =
                Users.builder()
                        .nickName("탈퇴테스트유저")
                        .password("testPassword123!")
                        .status(UserStatus.ACTIVE)
                        .build();
        userRepository.save(user);
        testUserId = user.getId();

        // 2. 외래 키 제약을 테스트하기 위해 자식 데이터(AuthAccount) 생성
        authAccountRepository.save(
                AuthAccount.builder()
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
        LocalDateTime beforeWithdraw = LocalDateTime.now();
        userWithdrawService.withdrawUser(testUserId, reasonDTO);
        em.flush();
        em.clear();
        LocalDateTime afterWithdraw = LocalDateTime.now();

        // then
        Users foundUser = userRepository.findById(testUserId).orElseThrow();
        assertEquals(UserStatus.INACTIVE, foundUser.getStatus());
        assertEquals("더 이상 사용하지 않음", foundUser.getDeleted_reason());
        assertFalse(foundUser.getInactiveDate().isBefore(beforeWithdraw));
        assertFalse(foundUser.getInactiveDate().isAfter(afterWithdraw));
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
        boolean authExists =
                authAccountRepository.existsByUserIdAndProvider(testUserId, Provider.GENERAL);
        assertFalse(authExists, "연관된 AuthAccount 데이터가 삭제되지 않았습니다.");
    }

    @Test
    @DisplayName("존재하지 않는 사용자 ID로 즉시 삭제 호출 시 GeneralException이 발생해야 한다")
    void testImmediateDelete_UserNotFound_ThrowsException() {
        // given
        Long nonExistentId = 9999L;

        // when & then
        assertThrows(
                com.umc.linkyou.apiPayload.exception.GeneralException.class,
                () -> {
                    userWithdrawService.testImmediateDelete(nonExistentId);
                });
    }

    @Test
    @DisplayName("복합 연관 관계(Linku, Folder 등)가 있는 사용자도 에러 없이 완전 삭제되어야 한다")
    void testImmediateDelete_WithComplexAssociations_Success() {
        // given
        // 1. 이미 setUp에서 생성된 유저를 가져옴
        Users user = userRepository.findById(testUserId).get();

        // 2. 관심사/목적 (다대다 조인 - users_interests / users_purposes)
        Interests interest = interestRepository.save(Interests.of("IT"));
        UsersInterest usersInterest =
                usersInterestRepository.save(UsersInterest.of(user, interest));
        Long usersInterestId = usersInterest.getId();

        Purposes purpose = purposeRepository.save(Purposes.of("STUDY"));
        UsersPurpose usersPurpose = usersPurposeRepository.save(UsersPurpose.of(user, purpose));
        Long usersPurposeId = usersPurpose.getId();

        // 3. 개인 링크 저장 기록(users_linkus) 및 하위 연관(linku_folders, curation_linkus)
        Fcolor fcolor =
                fcolorRepository.save(
                        Fcolor.builder()
                                .colorName("blue")
                                .colorCode1("#111111")
                                .colorCode2("#222222")
                                .colorCode3("#333333")
                                .colorCode4("#444444")
                                .build());
        Category category =
                categoryRepository.save(
                        Category.builder().categoryName("기술").fcolor(fcolor).build());
        Domain domain =
                domainRepository.save(
                        Domain.builder().name("example").domainTail("example.com").build());
        Emotion emotion = emotionRepository.save(Emotion.builder().name("기대").build());
        Situation situation = situationRepository.save(Situation.builder().name("공부").build());

        Linku linku =
                linkuRepository.save(
                        Linku.builder()
                                .category(category)
                                .domain(domain)
                                .linkuUrl("https://example.com/withdraw-test")
                                .title("공용 링크")
                                .emotion(emotion)
                                .situation(situation)
                                .build());
        Long linkuId = linku.getLinkuId();

        Folder folder =
                folderRepository.save(
                        Folder.builder().folderName("탈퇴테스트 폴더").category(category).build());

        UsersLinku usersLinku =
                usersLinkuRepository.save(
                        UsersLinku.builder().user(user).linku(linku).emotion(emotion).build());
        Long usersLinkuId = usersLinku.getUserLinkuId();

        LinkuFolder linkuFolder =
                linkuFolderRepository.save(
                        LinkuFolder.builder().folder(folder).usersLinku(usersLinku).build());
        Long linkuFolderId = linkuFolder.getLinkuFolderId();

        Curation curation =
                curationRepository.save(Curation.builder().user(user).baseMonth("2026-07").build());
        Long curationId = curation.getCurationId();

        CurationLinku curationLinku =
                curationLinkuRepository.save(
                        CurationLinku.ofInternal(curation, usersLinku, null));
        Long curationLinkuId = curationLinku.getCurationLinkuId();

        // 영속성 컨텍스트 반영
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
        Optional<AuthAccount> authAccount =
                authAccountRepository.findByProviderAndExternalId(
                        Provider.GENERAL, "test@linku.com");
        assertTrue(authAccount.isEmpty(), "OrphanRemoval 또는 clear() 처리가 정상적으로 동작하지 않았습니다.");

        // 개인 데이터(관심사/목적 연결, 링크 저장 기록과 그 하위 연관)는 모두 CASCADE로 삭제되어야 한다
        assertTrue(usersInterestRepository.findById(usersInterestId).isEmpty(), "관심사 연결이 삭제되지 않았습니다.");
        assertTrue(usersPurposeRepository.findById(usersPurposeId).isEmpty(), "목적 연결이 삭제되지 않았습니다.");
        assertTrue(usersLinkuRepository.findById(usersLinkuId).isEmpty(), "개인 링크 저장 기록이 삭제되지 않았습니다.");
        assertTrue(linkuFolderRepository.findById(linkuFolderId).isEmpty(), "linku_folders 가 CASCADE로 삭제되지 않았습니다.");
        assertTrue(curationRepository.findById(curationId).isEmpty(), "curations 가 삭제되지 않았습니다.");
        assertTrue(
                curationLinkuRepository.findById(curationLinkuId).isEmpty(),
                "curation_linkus 가 CASCADE로 삭제되지 않았습니다.");

        // 공용/마스터 데이터는 유저 삭제와 무관하게 유지되어야 한다
        assertTrue(linkuRepository.existsById(linkuId), "공용 데이터인 linkus 가 함께 삭제되면 안 됩니다.");
        assertTrue(categoryRepository.existsById(category.getCategoryId()), "공용 분류 데이터가 삭제되면 안 됩니다.");
        assertTrue(interestRepository.existsById(interest.getId()), "관심사 마스터(카탈로그)는 유지되어야 합니다.");
        assertTrue(purposeRepository.existsById(purpose.getId()), "목적 마스터(카탈로그)는 유지되어야 합니다.");
    }

    @Test
    @DisplayName("카카오 웹훅 호출 시 연관 계정이 없으면 유저 상태가 INACTIVE로 변경되어야 한다")
    void handleKakaoUnlinkWebhook_NoOtherAccounts_WithdrawsUser() {
        // given
        em.flush();
        em.clear();

        authAccountRepository.deleteAllByUser_Id(testUserId);

        em.flush();
        em.clear();

        // 3. 다시 유저 로드 (완전히 비워진 상태)
        Users user = userRepository.findById(testUserId).get();

        // 4. 이제 테스트 타겟인 '카카오 계정'만 딱 하나 생성
        AuthAccount kakaoAuth =
                AuthAccount.builder()
                        .user(user)
                        .provider(Provider.KAKAO)
                        .externalId("kakao_12345")
                        .email("kakao@test.com")
                        .build();

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

        boolean exists =
                authAccountRepository.existsByUserIdAndProvider(testUserId, Provider.KAKAO);
        assertFalse(exists, "카카오 연동 정보는 DB에서 삭제되어야 합니다.");
    }
}
