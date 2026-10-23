package com.claim.platform.intake.infrastructure.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 在启动阶段校验签收号全局唯一索引。
 * 缺少 {@code uk_sign_no(sign_no)} 时拒绝启动，避免 Redis 回退和 MySQL 降级失去最终唯一性屏障。
 *
 * @author wangguangwu
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SignNoSchemaValidator implements InitializingBean {

    private static final String INDEX_SQL = """
            SELECT COUNT(*)
            FROM information_schema.statistics
            WHERE table_schema = DATABASE()
              AND table_name = 't_claim_intake'
              AND index_name = 'uk_sign_no'
              AND non_unique = 0
              AND seq_in_index = 1
              AND column_name = 'sign_no'
            """;

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void afterPropertiesSet() {
        Integer count = jdbcTemplate.queryForObject(INDEX_SQL, Integer.class);
        if (count == null || count == 0) {
            log.error("claim intake sign no schema validation failed table=t_claim_intake index=uk_sign_no");
            throw new IllegalStateException(
                    "数据库缺少全局唯一索引 uk_sign_no(sign_no)，请先执行存量库迁移脚本");
        }
        log.info("claim intake sign no schema validated table=t_claim_intake index=uk_sign_no");
    }
}
