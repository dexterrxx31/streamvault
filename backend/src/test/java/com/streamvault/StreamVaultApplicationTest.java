package com.streamvault;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/** Catches bean wiring mistakes that slice/unit tests with mocks cannot. */
@SpringBootTest
@DisplayName("Application context")
class StreamVaultApplicationTest {

    @Test
    @DisplayName("Should start with the test configuration")
    void contextLoads() {
    }
}
