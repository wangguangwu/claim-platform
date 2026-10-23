package com.claim.platform.intake.interfaces.tenant.core.pipeline;

import com.claim.platform.intake.application.command.SubmitIntakeCommand;

/**
 * 租户报文转换层的固定入口，按 Parser → Validator → Converter 顺序执行。
 * 主流程不包含租户判断、字段映射、编码转换或 HTTP 异常映射。
 *
 * @author wangguangwu
 */
public interface IntakeConvertPipeline {

    /**
     * 将已完成验签和解密的 Raw Payload 转换为平台标准签收命令。
     *
     * @param payload 原始报文
     * @return 完整的签收命令
     */
    SubmitIntakeCommand execute(String payload);
}
