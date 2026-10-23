package com.claim.platform.intake.infrastructure.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * 提供按配置时区计算业务日期的系统时钟。
 * 测试可注入固定 {@link Clock} 覆盖，生产环境不允许运行期变更时区。
 *
 * @author wangguangwu
 */
@Configuration
public class SignNoClockConfiguration {

    @Bean
    @ConditionalOnMissingBean(Clock.class)
    public Clock signNoClock(SignNoProperties properties) {
        return Clock.system(properties.getZone());
    }
}
