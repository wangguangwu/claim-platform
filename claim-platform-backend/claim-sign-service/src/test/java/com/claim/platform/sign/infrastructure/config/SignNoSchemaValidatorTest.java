package com.claim.platform.sign.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.claim.platform.sign.infrastructure.persistence.ClaimIntakeTable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 验证启动阶段通过 JDBC 元数据确认存在首列为 sign_no 的全局唯一索引。
 * 校验过程不执行任何手写 SQL。
 *
 * @author wangguangwu
 */
@ExtendWith(MockitoExtension.class)
class SignNoSchemaValidatorTest {

    private static final String CATALOG = "claim_intake_db";

    @Mock
    private DataSource dataSource;
    @Mock
    private Connection connection;
    @Mock
    private DatabaseMetaData metadata;
    @Mock
    private ResultSet indexes;
    @InjectMocks
    private SignNoSchemaValidator validator;

    @Test
    void shouldPassWhenGlobalUniqueIndexExists() throws SQLException {
        givenIndexMetadata();
        given(indexes.next()).willReturn(true, false);
        given(indexes.getInt("ORDINAL_POSITION")).willReturn(1);
        given(indexes.getString("INDEX_NAME")).willReturn(ClaimIntakeTable.INDEX_UK_SIGN_NO);
        given(indexes.getString("COLUMN_NAME")).willReturn(ClaimIntakeTable.SIGN_NO);

        assertThatCode(validator::afterPropertiesSet).doesNotThrowAnyException();
    }

    @Test
    void shouldFailWhenGlobalUniqueIndexMissing() throws SQLException {
        givenIndexMetadata();
        given(indexes.next()).willReturn(false);

        assertThatThrownBy(validator::afterPropertiesSet)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(ClaimIntakeTable.INDEX_UK_SIGN_NO);
    }

    @Test
    void shouldFailWhenUniqueIndexUsesDifferentColumn() throws SQLException {
        givenIndexMetadata();
        given(indexes.next()).willReturn(true, false);
        given(indexes.getInt("ORDINAL_POSITION")).willReturn(1);
        given(indexes.getString("INDEX_NAME")).willReturn(ClaimIntakeTable.INDEX_UK_SIGN_NO);
        given(indexes.getString("COLUMN_NAME")).willReturn(ClaimIntakeTable.EXTERNAL_APPLY_NO);

        assertThatThrownBy(validator::afterPropertiesSet)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("数据库缺少全局唯一索引");
    }

    @Test
    void shouldFailWhenSignNoIsNotLeadingColumn() throws SQLException {
        givenIndexMetadata();
        given(indexes.next()).willReturn(true, false);
        given(indexes.getInt("ORDINAL_POSITION")).willReturn(2);

        assertThatThrownBy(validator::afterPropertiesSet)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("数据库缺少全局唯一索引");
    }

    @Test
    void shouldFailWhenMetadataQueryFails() throws SQLException {
        given(dataSource.getConnection()).willReturn(connection);
        given(connection.getMetaData()).willReturn(metadata);
        given(connection.getCatalog()).willReturn(CATALOG);
        given(metadata.getIndexInfo(CATALOG, null, ClaimIntakeTable.NAME, true, false))
                .willThrow(new SQLException("metadata unavailable"));

        assertThatThrownBy(validator::afterPropertiesSet)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("无法校验数据库全局唯一索引");
    }

    private void givenIndexMetadata() throws SQLException {
        given(dataSource.getConnection()).willReturn(connection);
        given(connection.getMetaData()).willReturn(metadata);
        given(connection.getCatalog()).willReturn(CATALOG);
        given(metadata.getIndexInfo(CATALOG, null, ClaimIntakeTable.NAME, true, false)).willReturn(indexes);
    }
}
