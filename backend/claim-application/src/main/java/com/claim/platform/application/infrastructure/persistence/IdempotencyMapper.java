package com.claim.platform.application.infrastructure.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 通过 MySQL 唯一键和原生原子语句实现幂等占位，不依赖 Redis 锁。
 *
 * @author wangguangwu
 */
@Mapper
public interface IdempotencyMapper extends BaseMapper<IdempotencyDO> {

    /**
     * 尝试写入处理中占位；重复键返回 0 而非抛出异常。
     */
    @Insert("INSERT IGNORE INTO t_sign_idempotency (tenant_code, operation, idempotency_key, request_hash, processing_status) "
            + "VALUES (#{tenantCode}, #{operation}, #{idempotencyKey}, #{requestHash}, 'PROCESSING')")
    int insertIgnore(@Param("tenantCode") String tenantCode, @Param("operation") String operation,
                     @Param("idempotencyKey") String idempotencyKey, @Param("requestHash") String requestHash);

    /**
     * 查询同一租户、操作和幂等键的首次处理结果。
     */
    @Select("SELECT * FROM t_sign_idempotency WHERE tenant_code = #{tenantCode} AND operation = #{operation} "
            + "AND idempotency_key = #{idempotencyKey}")
    IdempotencyDO find(@Param("tenantCode") String tenantCode, @Param("operation") String operation,
                           @Param("idempotencyKey") String idempotencyKey);

    /**
     * 保存命令完成状态和可回放的签收编号。
     */
    @Update("UPDATE t_sign_idempotency SET processing_status = 'COMPLETED', result_sign_no = #{signNo}, "
            + "completed_at = CURRENT_TIMESTAMP(3) WHERE tenant_code = #{tenantCode} AND operation = #{operation} "
            + "AND idempotency_key = #{idempotencyKey}")
    int markCompleted(@Param("tenantCode") String tenantCode, @Param("operation") String operation,
                      @Param("idempotencyKey") String idempotencyKey, @Param("signNo") String signNo);
}
