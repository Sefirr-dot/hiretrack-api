package com.sefirr.hiretrack;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Boots the full context against a real PostgreSQL + Redis (provided by the CI service containers,
 * or by `docker compose up -d postgres redis` locally). Skipped when no database is configured.
 */
@SpringBootTest
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "DB_URL", matches = ".+")
class HiretrackApplicationTests {

    @Test
    void contextLoads() {
    }
}
