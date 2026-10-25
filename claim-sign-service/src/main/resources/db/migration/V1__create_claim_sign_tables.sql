CREATE TABLE t_claim_sign (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tenant_code VARCHAR(64) NOT NULL,
    sign_no VARCHAR(64) NOT NULL,
    external_apply_no VARCHAR(128) NOT NULL,
    applicant_name VARCHAR(128) NULL,
    sign_status VARCHAR(32) NOT NULL,
    applicant_info JSON NULL,
    accident_person_info JSON NULL,
    main_insured_info JSON NULL,
    payee_info JSON NULL,
    ext_json JSON NULL,
    protocol_version INT NOT NULL,
    version INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sign_no (sign_no),
    UNIQUE KEY uk_tenant_external_apply_no (tenant_code, external_apply_no),
    KEY idx_tenant_status_created (tenant_code, sign_status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE t_claim_sign_material (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tenant_code VARCHAR(64) NOT NULL,
    sign_no VARCHAR(64) NOT NULL,
    batch_no VARCHAR(64) NOT NULL,
    type VARCHAR(64) NOT NULL,
    name VARCHAR(256) NOT NULL,
    path VARCHAR(512) NOT NULL,
    original_path VARCHAR(512) NULL,
    is_downloaded TINYINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_material_tenant_sign (tenant_code, sign_no),
    KEY idx_material_tenant_batch (tenant_code, batch_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE t_claim_sign_issue (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tenant_code VARCHAR(64) NOT NULL,
    sign_no VARCHAR(64) NOT NULL,
    issue_no VARCHAR(64) NOT NULL,
    auditor VARCHAR(128) NULL,
    reason VARCHAR(1024) NOT NULL,
    ext_json JSON NULL,
    is_sync_tenant TINYINT NOT NULL DEFAULT 0,
    is_supplement TINYINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_issue_tenant_sign_no (tenant_code, sign_no, issue_no),
    KEY idx_issue_tenant_sign (tenant_code, sign_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE t_claim_sign_case_rel (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tenant_code VARCHAR(64) NOT NULL,
    sign_no VARCHAR(64) NOT NULL,
    case_no VARCHAR(64) NOT NULL,
    external_case_no VARCHAR(128) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_case_relation (tenant_code, sign_no, case_no),
    KEY idx_case_relation_tenant_case (tenant_code, case_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE t_claim_sign_interaction_log (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tenant_code VARCHAR(64) NOT NULL,
    sign_no VARCHAR(64) NOT NULL,
    interaction_type VARCHAR(64) NOT NULL,
    direction VARCHAR(8) NOT NULL,
    source_system VARCHAR(64) NULL,
    target_system VARCHAR(64) NULL,
    request_id VARCHAR(64) NULL,
    trace_id VARCHAR(64) NULL,
    result VARCHAR(32) NOT NULL,
    error_code VARCHAR(64) NULL,
    message VARCHAR(512) NULL,
    cost_ms BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_interaction_tenant_sign_created (tenant_code, sign_no, created_at),
    KEY idx_interaction_trace_id (trace_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
