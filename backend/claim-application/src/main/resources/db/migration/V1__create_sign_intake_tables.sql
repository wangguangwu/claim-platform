CREATE TABLE t_sign_application (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_code VARCHAR(64) NOT NULL COMMENT '租户编码',
    sign_no VARCHAR(64) NOT NULL COMMENT '平台签收单号',
    external_apply_no VARCHAR(64) NOT NULL COMMENT '租户侧申请单号',
    applicant_name VARCHAR(64) DEFAULT NULL COMMENT '申请人姓名',
    applicant_info JSON DEFAULT NULL COMMENT '申请人信息',
    accident_person_info JSON DEFAULT NULL COMMENT '出险人信息',
    main_insured_info JSON DEFAULT NULL COMMENT '主被保险人信息',
    payee_info JSON DEFAULT NULL COMMENT '收款人信息',
    application_status VARCHAR(32) NOT NULL DEFAULT 'IMAGE_NOT_UPLOAD' COMMENT '申请状态',
    ext_json JSON DEFAULT NULL COMMENT '租户扩展信息',
    version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0-否，1-是',
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_sign_no (tenant_code, sign_no),
    UNIQUE KEY uk_tenant_external_apply_no (tenant_code, external_apply_no),
    KEY idx_tenant_status_created_at (tenant_code, application_status, created_at),
    KEY idx_tenant_applicant_name (tenant_code, applicant_name),
    KEY idx_tenant_created_at (tenant_code, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='签收申请表';

CREATE TABLE t_sign_material (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_code VARCHAR(64) NOT NULL COMMENT '租户编码',
    sign_no VARCHAR(64) NOT NULL COMMENT '平台签收单号',
    batch_no VARCHAR(64) NOT NULL COMMENT '材料批次号',
    seq INT DEFAULT NULL COMMENT '材料序号',
    type VARCHAR(32) DEFAULT NULL COMMENT '材料类型',
    name VARCHAR(255) NOT NULL COMMENT '材料名称',
    path VARCHAR(1000) DEFAULT NULL COMMENT '平台对象存储路径',
    original_path VARCHAR(1000) DEFAULT NULL COMMENT '租户侧原始材料路径',
    is_downloaded TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已下载：0-否，1-是',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0-否，1-是',
    PRIMARY KEY (id),
    KEY idx_tenant_sign_no (tenant_code, sign_no),
    KEY idx_tenant_sign_batch (tenant_code, sign_no, batch_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='签收材料表';

CREATE TABLE t_sign_problem (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_code VARCHAR(64) NOT NULL COMMENT '租户编码',
    sign_no VARCHAR(64) NOT NULL COMMENT '平台签收单号',
    auditor VARCHAR(64) DEFAULT NULL COMMENT '审核人员',
    reason VARCHAR(1000) NOT NULL COMMENT '问题原因',
    is_sync_tenant TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已同步租户：0-否，1-是',
    is_supplement TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已补充材料：0-否，1-是',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0-否，1-是',
    PRIMARY KEY (id),
    KEY idx_tenant_sign_no (tenant_code, sign_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='签收问题件表';

CREATE TABLE t_sign_interaction_log (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_code VARCHAR(64) NOT NULL COMMENT '租户编码',
    sign_no VARCHAR(64) NOT NULL COMMENT '平台签收单号',
    interaction_type VARCHAR(32) NOT NULL COMMENT '交互类型',
    direction VARCHAR(8) NOT NULL COMMENT '交互方向：IN-入站，OUT-出站',
    target_system VARCHAR(64) DEFAULT NULL COMMENT '目标系统标识',
    request_id VARCHAR(64) DEFAULT NULL COMMENT '外部请求标识',
    trace_id VARCHAR(64) DEFAULT NULL COMMENT '全链路追踪标识',
    result VARCHAR(16) NOT NULL COMMENT '交互结果',
    message VARCHAR(500) DEFAULT NULL COMMENT '结果说明或错误信息',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_tenant_sign_time (tenant_code, sign_no, created_at),
    KEY idx_tenant_trace (tenant_code, trace_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='签收交互日志表';

CREATE TABLE t_sign_idempotency (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_code VARCHAR(64) NOT NULL COMMENT '租户编码',
    operation VARCHAR(32) NOT NULL COMMENT '幂等操作类型',
    idempotency_key VARCHAR(128) NOT NULL COMMENT '调用方幂等键',
    request_hash CHAR(64) NOT NULL COMMENT '规范化请求体的SHA-256摘要',
    processing_status VARCHAR(16) NOT NULL COMMENT '处理状态',
    result_sign_no VARCHAR(64) DEFAULT NULL COMMENT '处理结果对应的平台签收单号',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    completed_at DATETIME(3) DEFAULT NULL COMMENT '处理完成时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_operation_idempotency (tenant_code, operation, idempotency_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='签收命令幂等记录';

CREATE TABLE t_sign_outbox (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    event_id CHAR(36) NOT NULL COMMENT '领域事件唯一标识',
    event_type VARCHAR(128) NOT NULL COMMENT '领域事件类型',
    event_version VARCHAR(16) NOT NULL COMMENT '事件契约版本',
    tenant_code VARCHAR(64) NOT NULL COMMENT '租户编码',
    aggregate_type VARCHAR(64) NOT NULL COMMENT '聚合类型',
    aggregate_id VARCHAR(64) NOT NULL COMMENT '聚合标识',
    payload JSON NOT NULL COMMENT '事件载荷',
    trace_id VARCHAR(64) DEFAULT NULL COMMENT '全链路追踪标识',
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT '投递状态',
    occurred_at DATETIME(3) NOT NULL COMMENT '事件发生时间',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_event_id (event_id),
    KEY idx_status_created_at (status, created_at),
    KEY idx_tenant_aggregate (tenant_code, aggregate_type, aggregate_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='签收事件外盒表';
