package com.umc.linkyou.integration;

import com.umc.linkyou.domain.Alarm;
import com.umc.linkyou.domain.AlarmSetting;
import com.umc.linkyou.domain.UserAlarm;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.enums.AlarmType;
import com.umc.linkyou.domain.enums.Role;
import com.umc.linkyou.jwt.CustomUserDetails;
import com.umc.linkyou.repository.AlarmRepository;
import com.umc.linkyou.repository.AlarmSettingRepository;
import com.umc.linkyou.repository.UserAlarmRepository;
import com.umc.linkyou.repository.userRepository.UserRepository;
import com.umc.linkyou.support.security.TestSecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestSecurityConfig.class)
@Transactional
class AlarmApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AlarmRepository alarmRepository;

    @Autowired
    private UserAlarmRepository userAlarmRepository;

    @Autowired
    private AlarmSettingRepository alarmSettingRepository;

    @Test
    @DisplayName("알림 리스트 조회 - alarmType 파라미터로 정상 조회된다")
    void viewAlarmList_withAlarmTypeParam_success() throws Exception {
        Users user = createUser("alarm_user_1");
        saveUserAlarm(user, AlarmType.CURATION_UPDATED, 101L);

        mockMvc.perform(get("/api/v1/alarm/list")
                        .param("alarmType", "ALL")
                        .param("size", "10")
                        .with(authentication(authFor(user))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("ALARM2006"))
                .andExpect(jsonPath("$.result.items.length()").value(1))
                .andExpect(jsonPath("$.result.items[0].alarmType").value("CURATION"))
                .andExpect(jsonPath("$.result.items[0].targetId").value(101));
    }

    @Test
    @DisplayName("알림 리스트 조회 - alarmType 필터가 적용된다")
    void viewAlarmList_withAlarmTypeFilter_success() throws Exception {
        Users user = createUser("alarm_user_2");

        saveUserAlarm(user, AlarmType.FOLDER_DELETED, 201L);
        saveUserAlarm(user, AlarmType.CURATION_UPDATED, 202L);

        mockMvc.perform(get("/api/v1/alarm/list")
                        .param("alarmType", "CURATION")
                        .param("size", "10")
                        .with(authentication(authFor(user))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.result.items.length()").value(1))
                .andExpect(jsonPath("$.result.items[0].alarmType").value("CURATION"))
                .andExpect(jsonPath("$.result.items[0].targetId").value(202));
    }

    @Test
    @DisplayName("알림 리스트 조회 - 구 파라미터 alarmSettingType 사용 시 400")
    void viewAlarmList_withLegacyParam_badRequest() throws Exception {
        Users user = createUser("alarm_user_3");
        saveUserAlarm(user, AlarmType.CURATION_UPDATED, 301L);

        mockMvc.perform(get("/api/v1/alarm/list")
                        .param("alarmSettingType", "ALL")
                        .param("size", "10")
                        .with(authentication(authFor(user))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("알림 설정 수정 - AlarmSettingResponseDTO 형태로 반환된다")
    void updateAlarmSetting_returnsFullDto() throws Exception {
        Users user = createUser("alarm_user_4");
        alarmSettingRepository.save(AlarmSetting.createDefault(user));

        mockMvc.perform(patch("/api/v1/alarm/settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"alarmType\": \"LINK\"}")
                        .with(authentication(authFor(user))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.result.isAllEnabled").exists())
                .andExpect(jsonPath("$.result.isLinkEnabled").value(false))
                .andExpect(jsonPath("$.result.isFolderEnabled").value(true))
                .andExpect(jsonPath("$.result.isCurationEnabled").value(true))
                .andExpect(jsonPath("$.result.isNoticeEnabled").value(true));
    }

    @Test
    @DisplayName("알림 설정 수정 - 개별 설정 모두 off 시 isAllEnabled도 false가 된다")
    void updateAlarmSetting_allOff_disablesAllEnabled() throws Exception {
        Users user = createUser("alarm_user_5");
        AlarmSetting setting = AlarmSetting.createDefault(user);
        setting.updateLink(false);
        setting.updateFolder(false);
        setting.updateCuration(false);
        alarmSettingRepository.save(setting);

        // NOTICE만 남은 상태에서 NOTICE off → 전체 off
        mockMvc.perform(patch("/api/v1/alarm/settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"alarmType\": \"NOTICE\"}")
                        .with(authentication(authFor(user))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.isAllEnabled").value(false))
                .andExpect(jsonPath("$.result.isLinkEnabled").value(false))
                .andExpect(jsonPath("$.result.isFolderEnabled").value(false))
                .andExpect(jsonPath("$.result.isCurationEnabled").value(false))
                .andExpect(jsonPath("$.result.isNoticeEnabled").value(false));
    }

    private Users createUser(String nickName) {
        Users user = Users.builder()
                .nickName(nickName)
                .password("password")
                .role(Role.USER)
                .build();
        return userRepository.save(user);
    }

    private void saveUserAlarm(Users user, AlarmType alarmType, Long targetId) {
        Alarm alarm = alarmRepository.save(Alarm.create(alarmType, targetId));
        userAlarmRepository.save(UserAlarm.create(user, alarm));
    }

    private Authentication authFor(Users user) {
        CustomUserDetails principal = new CustomUserDetails(user, "kakao");
        return new UsernamePasswordAuthenticationToken(
                principal,
                null,
                principal.getAuthorities()
        );
    }
}
