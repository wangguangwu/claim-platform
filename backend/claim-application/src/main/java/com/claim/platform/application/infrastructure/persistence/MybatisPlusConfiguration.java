package com.claim.platform.application.infrastructure.persistence;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 配置 Application Intake 的 MyBatis-Plus 乐观锁拦截器，保护申请状态并发更新。
 *
 * @author wangguangwu
 */
@Configuration
public class MybatisPlusConfiguration {

    /**
     * 注册版本字段更新拦截器，使申请表的 version 成为实际并发控制条件。
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
