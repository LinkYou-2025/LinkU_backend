package com.umc.linkyou.integration;

import com.umc.linkyou.domain.Alarm;
import com.umc.linkyou.domain.UserAlarm;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.enums.AlarmType;
import com.umc.linkyou.domain.enums.Role;
import com.umc.linkyou.jwt.CustomUserDetails;
import com.umc.linkyou.repository.AlarmRepository;
import com.umc.linkyou.repository.UserAlarmRepository;
import com.umc.linkyou.repository.userRepository.UserRepository;
import com.umc.linkyou.support.security.TestSecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestSecurityConfig.class)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:alarmtest;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never"
})
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
