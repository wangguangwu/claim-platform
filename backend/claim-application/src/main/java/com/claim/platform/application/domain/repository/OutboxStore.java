package com.claim.platform.application.domain.repository;

import com.claim.platform.application.domain.event.OutboxEvent;

/**
 * 定义签收领域事件的原子 Outbox 持久化端口。
 *
 * @author wangguangwu
 */
public interface OutboxStore {

    /**
     * 追加待发布事件意图；实现必须与领域写入使用同一事务。
     */
    void append(OutboxEvent event);
}
