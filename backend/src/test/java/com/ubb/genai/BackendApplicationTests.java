package com.ubb.genai;

import com.ubb.genai.events.authoring.support.PostgresTestConfiguration;
import org.springframework.context.annotation.Import;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@Import(PostgresTestConfiguration.class)
@SpringBootTest
class BackendApplicationTests {

    @Test
    void contextLoads() {
    }

}
