package com.umc.linkyou;

import com.umc.linkyou.support.config.TestExternalConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestExternalConfig.class)
class LinkyouApplicationTests {

	@Test
	void contextLoads() {
	}

}
