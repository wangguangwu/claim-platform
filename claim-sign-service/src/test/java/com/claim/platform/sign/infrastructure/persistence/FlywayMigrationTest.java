package com.claim.platform.sign.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/** Guards the stable DDL contract that Flyway applies to an empty MySQL Sign database.
 *
 * @author wangguangwu
 */
class FlywayMigrationTest {
    @Test
    void shouldDefineFiveSignTablesWithoutPhysicalForeignKeys() throws IOException {
        try (var input = getClass().getResourceAsStream("/db/migration/V1__create_claim_sign_tables.sql")) {
            assertThat(input).isNotNull();
            String sql = new String(input.readAllBytes(), StandardCharsets.UTF_8).toUpperCase();
            assertThat(sql).contains("CREATE TABLE T_CLAIM_SIGN", "CREATE TABLE T_CLAIM_SIGN_MATERIAL",
                    "CREATE TABLE T_CLAIM_SIGN_ISSUE", "CREATE TABLE T_CLAIM_SIGN_CASE_REL",
                    "CREATE TABLE T_CLAIM_SIGN_INTERACTION_LOG", "UNIQUE KEY UK_SIGN_NO",
                    "UNIQUE KEY UK_TENANT_EXTERNAL_APPLY_NO", "UNIQUE KEY UK_ISSUE_TENANT_SIGN_NO");
            assertThat(sql).doesNotContain("FOREIGN KEY");
        }
    }
}
