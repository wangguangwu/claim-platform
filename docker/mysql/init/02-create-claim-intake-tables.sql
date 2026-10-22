USE claim_intake_db;

-- ============================================================
-- 1. 理赔受理表
-- ============================================================
CREATE TABLE `t_claim_intake` (
	`id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
	`tenant_code` VARCHAR ( 64 ) NOT NULL COMMENT '租户编码',
	`sign_no` VARCHAR ( 64 ) NOT NULL COMMENT '平台签收编号',
	`external_apply_no` VARCHAR ( 64 ) NOT NULL COMMENT '外部申请编号',
	`applicant_name` VARCHAR ( 64 ) DEFAULT NULL COMMENT '申请人姓名',
	`applicant_info` JSON DEFAULT NULL COMMENT '申请人信息',
	`accident_person_info` JSON DEFAULT NULL COMMENT '出险人信息',
	`main_insured_info` JSON DEFAULT NULL COMMENT '主被保险人信息',
	`payee_info` JSON DEFAULT NULL COMMENT '领款人信息',
	`intake_status` VARCHAR ( 32 ) NOT NULL DEFAULT 'IMAGE_NOT_UPLOAD' COMMENT '受理状态：IMAGE_NOT_UPLOAD-材料未上传，IMAGE_UPLOADED-材料已上传，NO_INSURED_INFO-未发现承保信息，SYNC_AUDIT-同步审核系统，SYNC_AUDIT_SUCCESS-同步审核系统成功，ISSUE_CASE-问题件，CASE_REVOKE-案件撤销',
	`ext_json` JSON DEFAULT NULL COMMENT '扩展信息',
	`version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
	`created_at` DATETIME ( 3 ) NOT NULL DEFAULT CURRENT_TIMESTAMP ( 3 ) COMMENT '创建时间',
	`updated_at` DATETIME ( 3 ) NOT NULL DEFAULT CURRENT_TIMESTAMP ( 3 ) ON UPDATE CURRENT_TIMESTAMP ( 3 ) COMMENT '更新时间',
	`is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除：0-否，1-是',
	PRIMARY KEY ( `id` ),
	UNIQUE KEY `uk_tenant_sign_no` ( `tenant_code`, `sign_no` ),
	UNIQUE KEY `uk_tenant_external_apply_no` ( `tenant_code`, `external_apply_no` ),
	KEY `idx_tenant_status_created_at` ( `tenant_code`, `intake_status`, `created_at` ),
	KEY `idx_tenant_applicant_name` ( `tenant_code`, `applicant_name` ),
	KEY `idx_tenant_created_at` ( `tenant_code`, `created_at` )
) ENGINE = INNODB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '理赔受理表';

-- ============================================================
-- 2. 理赔受理材料表
-- ============================================================
CREATE TABLE `t_claim_intake_material` (
	`id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
	`tenant_code` VARCHAR ( 64 ) NOT NULL COMMENT '租户编码',
	`sign_no` VARCHAR ( 64 ) NOT NULL COMMENT '平台签收编号',
	`batch_no` VARCHAR ( 64 ) NOT NULL COMMENT '材料上传批次号',
	`seq` INT DEFAULT NULL COMMENT '材料顺序',
	`type` VARCHAR ( 32 ) DEFAULT NULL COMMENT '材料类型',
	`name` VARCHAR ( 255 ) NOT NULL COMMENT '材料名称',
	`path` VARCHAR ( 1000 ) DEFAULT NULL COMMENT '平台材料地址',
	`original_path` VARCHAR ( 1000 ) DEFAULT NULL COMMENT '原始材料地址',
	`is_downloaded` TINYINT ( 1 ) NOT NULL DEFAULT 0 COMMENT '是否已下载处理：0-否，1-是',
	`created_at` DATETIME ( 3 ) NOT NULL DEFAULT CURRENT_TIMESTAMP ( 3 ) COMMENT '创建时间',
	`updated_at` DATETIME ( 3 ) NOT NULL DEFAULT CURRENT_TIMESTAMP ( 3 ) ON UPDATE CURRENT_TIMESTAMP ( 3 ) COMMENT '更新时间',
	`is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除：0-否，1-是',
	PRIMARY KEY ( `id` ),
	KEY `idx_tenant_sign_no` ( `tenant_code`, `sign_no` ),
	KEY `idx_tenant_sign_batch` ( `tenant_code`, `sign_no`, `batch_no` )
) ENGINE = INNODB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '理赔受理材料表';

-- ============================================================
-- 3. 理赔受理问题件表
-- ============================================================
CREATE TABLE `t_claim_intake_issue` (
	`id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
	`tenant_code` VARCHAR ( 64 ) NOT NULL COMMENT '租户编码',
	`sign_no` VARCHAR ( 64 ) NOT NULL COMMENT '平台签收编号',
	`auditor` VARCHAR ( 64 ) DEFAULT NULL COMMENT '审核人',
	`reason` VARCHAR ( 1000 ) NOT NULL COMMENT '问题件原因',
	`is_sync_tenant` TINYINT ( 1 ) NOT NULL DEFAULT 0 COMMENT '是否已同步租户：0-否，1-是',
	`is_supplement` TINYINT ( 1 ) NOT NULL DEFAULT 0 COMMENT '是否需要补充材料：0-否，1-是',
	`created_at` DATETIME ( 3 ) NOT NULL DEFAULT CURRENT_TIMESTAMP ( 3 ) COMMENT '创建时间',
	`updated_at` DATETIME ( 3 ) NOT NULL DEFAULT CURRENT_TIMESTAMP ( 3 ) ON UPDATE CURRENT_TIMESTAMP ( 3 ) COMMENT '更新时间',
	`is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除：0-否，1-是',
	PRIMARY KEY ( `id` ),
	KEY `idx_tenant_sign_no` ( `tenant_code`, `sign_no` )
) ENGINE = INNODB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '理赔受理问题件表';

-- ============================================================
-- 4. 理赔受理交互日志表
-- ============================================================
CREATE TABLE `t_claim_intake_interaction_log` (
	`id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
	`tenant_code` VARCHAR ( 64 ) NOT NULL COMMENT '租户编码',
	`sign_no` VARCHAR ( 64 ) NOT NULL COMMENT '平台签收编号',
	`interaction_type` VARCHAR ( 32 ) NOT NULL COMMENT '交互类型：SIGN_RECEIVE-签收申请，IMAGE_UPLOAD-材料上传，INSURED_QUERY-承保信息查询，SYNC_AUDIT-同步审核系统，PROBLEM_FEEDBACK-问题件反馈，SUPPLEMENT_SUBMIT-补材重新提交，CASE_REVOKE-案件撤销',
	`direction` VARCHAR ( 8 ) NOT NULL COMMENT '交互方向：IN-接收，OUT-发送',
	`target_system` VARCHAR ( 64 ) DEFAULT NULL COMMENT '对端系统',
	`request_id` VARCHAR ( 64 ) DEFAULT NULL COMMENT '单次请求ID',
	`trace_id` VARCHAR ( 64 ) DEFAULT NULL COMMENT '链路TraceId',
	`result` VARCHAR ( 16 ) NOT NULL COMMENT '处理结果：SUCCESS-成功，FAILED-失败',
	`message` VARCHAR ( 500 ) DEFAULT NULL COMMENT '处理结果说明',
	`created_at` DATETIME ( 3 ) NOT NULL DEFAULT CURRENT_TIMESTAMP ( 3 ) COMMENT '创建时间',
	PRIMARY KEY ( `id` ),
	KEY `idx_tenant_sign_time` ( `tenant_code`, `sign_no`, `created_at` ),
	KEY `idx_tenant_trace` ( `tenant_code`, `trace_id` )
) ENGINE = INNODB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '理赔受理交互日志表';

-- ============================================================
-- 5. 理赔受理与案件关系表
-- ============================================================
CREATE TABLE `t_claim_intake_case_rel` (
	`id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
	`tenant_code` VARCHAR ( 64 ) NOT NULL COMMENT '租户编码',
	`sign_no` VARCHAR ( 64 ) NOT NULL COMMENT '平台签收编号',
	`case_no` VARCHAR ( 64 ) NOT NULL COMMENT '平台案件编号',
	`external_case_no` VARCHAR ( 64 ) DEFAULT NULL COMMENT '外部案件编号',
	`created_at` DATETIME ( 3 ) NOT NULL DEFAULT CURRENT_TIMESTAMP ( 3 ) COMMENT '创建时间',
	`updated_at` DATETIME ( 3 ) NOT NULL DEFAULT CURRENT_TIMESTAMP ( 3 ) ON UPDATE CURRENT_TIMESTAMP ( 3 ) COMMENT '更新时间',
	`is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除：0-否，1-是',
	PRIMARY KEY ( `id` ),
	UNIQUE KEY `uk_tenant_case_no` ( `tenant_code`, `case_no` ),
	UNIQUE KEY `uk_tenant_sign_case` ( `tenant_code`, `sign_no`, `case_no` ),
	KEY `idx_tenant_sign_no` ( `tenant_code`, `sign_no` ),
	KEY `idx_tenant_external_case_no` ( `tenant_code`, `external_case_no` )
) ENGINE = INNODB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '理赔受理与案件关系表';
