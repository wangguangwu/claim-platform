-- 存量库迁移：为 t_claim_intake 增加全局唯一索引 uk_sign_no(sign_no)。
-- 执行前必须备份；脚本可重复执行，目标索引已存在时跳过。
--
-- 用法：
--   mysql -h <host> -u <user> -p < database < 20261023-add-uk-sign-no.sql

USE claim_intake_db;

DELIMITER $$

DROP PROCEDURE IF EXISTS migrate_add_uk_sign_no$$

CREATE PROCEDURE migrate_add_uk_sign_no()
BEGIN
    DECLARE valid_index_count INT DEFAULT 0;
    DECLARE any_index_count INT DEFAULT 0;
    DECLARE duplicate_count INT DEFAULT 0;

    SELECT COUNT(*) INTO valid_index_count
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 't_claim_intake'
      AND index_name = 'uk_sign_no'
      AND non_unique = 0
      AND seq_in_index = 1
      AND column_name = 'sign_no';

    IF valid_index_count > 0 THEN
        SELECT 'uk_sign_no already exists, migration skipped' AS migration_result;
    ELSE
        SELECT COUNT(*) INTO any_index_count
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 't_claim_intake'
          AND index_name = 'uk_sign_no';

        IF any_index_count > 0 THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'uk_sign_no exists but is not a unique index on sign_no';
        END IF;

        SELECT COUNT(*) INTO duplicate_count
        FROM (
            SELECT sign_no
            FROM t_claim_intake
            GROUP BY sign_no
            HAVING COUNT(*) > 1
        ) duplicates;

        IF duplicate_count > 0 THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'uk_sign_no migration aborted: duplicate sign_no values exist';
        END IF;

        ALTER TABLE t_claim_intake
            ADD UNIQUE KEY uk_sign_no (sign_no),
            ALGORITHM = INPLACE,
            LOCK = NONE;

        SELECT 'uk_sign_no added' AS migration_result;
    END IF;
END$$

DELIMITER ;

CALL migrate_add_uk_sign_no();

DROP PROCEDURE IF EXISTS migrate_add_uk_sign_no;
