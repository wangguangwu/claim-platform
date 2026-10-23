package com.claim.platform.intake.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 验证启动阶段只有存在首列为 sign_no 的非唯一性索引时才允许启动。
 *
 * @author wangguangwu
 */
@ExtendWith(MockitoExtension.class)
class SignNoSchemaValidatorTest {

    @Mock
    private JdbcTemplate jdbcTemplate;
    @InjectMocks
    private SignNoSchemaValidator validator;

    @Test
    void shouldPassWhenGlobalUniqueIndexExists() {
        given(jdbcTemplate.queryForObject(anyString(), eq(Integer.class))).willReturn(1);

        assertThatCode(validator::afterPropertiesSet).doesNotThrowAnyException();
    }

    @Test
    void shouldFailWhenGlobalUniqueIndexMissing() {
        given(jdbcTemplate.queryForObject(anyString(), eq(Integer.class))).willReturn(0);

        assertThatThrownBy(validator::afterPropertiesSet)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("uk_sign_no");
    }

    @Test
    void shouldFailWhenIndexQueryReturnsNoCount() {
        given(jdbcTemplate.queryForObject(anyString(), eq(Integer.class))).willReturn(null);

        assertThatThrownBy(validator::afterPropertiesSet)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("数据库缺少全局唯一索引");
    }
}
