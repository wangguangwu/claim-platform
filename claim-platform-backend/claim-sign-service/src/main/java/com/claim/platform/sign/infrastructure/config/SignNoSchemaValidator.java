package com.claim.platform.sign.infrastructure.config;

import com.claim.platform.sign.infrastructure.persistence.SignTable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;

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

    private static final String JDBC_INDEX_NAME = "INDEX_NAME";
    private static final String JDBC_COLUMN_NAME = "COLUMN_NAME";
    private static final String JDBC_ORDINAL_POSITION = "ORDINAL_POSITION";
    private static final int FIRST_COLUMN_POSITION = 1;

    private final DataSource dataSource;

    @Override
    public void afterPropertiesSet() {
        if (!hasUniqueSignNoIndex()) {
            log.error("claim sign no schema validation failed table={} index={}",
                    SignTable.NAME, SignTable.INDEX_UK_SIGN_NO);
            throw new IllegalStateException(
                    "数据库缺少全局唯一索引 uk_sign_no(sign_no)，请按 docker/README.md"
                            + "「存量库补建签收号唯一索引」补建后重启");
        }
        log.info("claim sign no schema validated table={} index={}",
                SignTable.NAME, SignTable.INDEX_UK_SIGN_NO);
    }

    private boolean hasUniqueSignNoIndex() {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metadata = connection.getMetaData();
            try (ResultSet indexes = metadata.getIndexInfo(connection.getCatalog(), null,
                    SignTable.NAME, true, false)) {
                while (indexes.next()) {
                    if (isSignNoLeadingUniqueIndex(indexes)) {
                        return true;
                    }
                }
            }
            return false;
        } catch (SQLException exception) {
            log.error("claim sign no schema validation query failed table={} index={}",
                    SignTable.NAME, SignTable.INDEX_UK_SIGN_NO, exception);
            throw new IllegalStateException("无法校验数据库全局唯一索引 uk_sign_no(sign_no)", exception);
        }
    }

    private boolean isSignNoLeadingUniqueIndex(ResultSet indexes) throws SQLException {
        return indexes.getInt(JDBC_ORDINAL_POSITION) == FIRST_COLUMN_POSITION
                && SignTable.INDEX_UK_SIGN_NO.equalsIgnoreCase(indexes.getString(JDBC_INDEX_NAME))
                && SignTable.SIGN_NO.equalsIgnoreCase(indexes.getString(JDBC_COLUMN_NAME));
    }
}
