package com.umc.linkyou;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.umc.linkyou.support.config.TestExternalConfig;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestExternalConfig.class)
class LinkyouApplicationTests {

    @Test
    void contextLoads() {}
}
