package com.claim.platform.intake.infrastructure.persistence.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 配置 Claim Intake 的 MyBatis-Plus 乐观锁拦截器，保护申请状态并发更新。
 *
 * @author wangguangwu
 */
@Configuration
public class MybatisPlusConfiguration {

    /**
     * 注册版本字段更新拦截器，使受理表的 version 成为实际并发控制条件。
     *
     * @return 配置完成的 MyBatis-Plus 拦截器
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        return interceptor;
    }
}
