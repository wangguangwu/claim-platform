package com.claim.platform.intake.application.service;

import com.claim.platform.intake.application.command.SubmitIntakeCommand;
import com.claim.platform.intake.application.command.UploadMaterialsCommand;
import com.claim.platform.intake.application.result.ClaimIntakeResult;
import com.claim.platform.intake.domain.exception.ClaimIntakeNotFoundException;
import com.claim.platform.intake.domain.model.ClaimIntake;
import com.claim.platform.intake.domain.repository.ClaimIntakeStore;
import com.claim.platform.intake.domain.repository.IntakeMaterialStore;
import com.claim.platform.intake.domain.repository.InteractionLogStore;

import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 在 Claim Intake 限界上下文编排签收申请和材料登记，
 * 确保业务事实与交互日志在同一事务内提交。
 *
 * @author wangguangwu
 */
@Service
public class ClaimIntakeCommandService {

    private final ClaimIntakeStore intakeStore;
    private final IntakeMaterialStore materialStore;
    private final InteractionLogStore interactionLogStore;

    public ClaimIntakeCommandService(ClaimIntakeStore intakeStore, IntakeMaterialStore materialStore,
                                     InteractionLogStore interactionLogStore) {
        this.intakeStore = intakeStore;
        this.materialStore = materialStore;
        this.interactionLogStore = interactionLogStore;
    }

    /**
     * 接收外部申请并创建待上传材料的受理事实。
     * 幂等键为 {@code tenantCode + externalApplyNo}（见聚合不变量），重复提交回放既有签收编号。
     *
     * @param command 已从受信任调用链取得租户的命令
     * @return 首次创建或按外部申请编号回放的受理结果
     */
    @Transactional
    public ClaimIntakeResult submit(SubmitIntakeCommand command) {
        ClaimIntake existing = intakeStore.findByTenantAndExternalApplyNo(command.tenantCode(),
                        command.externalApplyNo())
                .orElse(null);
        if (existing != null) {
            return new ClaimIntakeResult(existing.signNo(), existing.status());
        }
        String signNo = "SIGN" + UUID.randomUUID().toString().replace("-", "").substring(0, 20).toUpperCase();
        ClaimIntake intake = ClaimIntake.create(command.tenantCode(), signNo, command.externalApplyNo(),
                command.applicantName(), command.applicant(), command.accidentPerson(),
                command.mainInsured(), command.payee(), command.extJson());
        try {
            intakeStore.save(intake);
        } catch (DataIntegrityViolationException exception) {
            // 并发下唯一键先赢：回读既有受理事实回放，保证幂等键语义不被绕过
            ClaimIntake concurrent = intakeStore.findByTenantAndExternalApplyNo(command.tenantCode(),
                            command.externalApplyNo())
                    .orElseThrow(() -> exception);
            return new ClaimIntakeResult(concurrent.signNo(), concurrent.status());
        }
        interactionLogStore.recordSuccess(command.tenantCode(), signNo, "SIGN_RECEIVE", null, command.traceId());
        return new ClaimIntakeResult(signNo, intake.status());
    }

    /**
     * 登记材料并将允许补材的受理切换至材料已上传状态。
     *
     * @param command 已按租户和签收编号定位的材料命令
     * @return 材料登记后的受理结果
     */
    @Transactional
    public ClaimIntakeResult uploadMaterials(UploadMaterialsCommand command) {
        ClaimIntake intake = intakeStore.findByTenantAndSignNo(command.tenantCode(), command.signNo())
                .orElseThrow(ClaimIntakeNotFoundException::new);
        intake.registerMaterials();
        materialStore.saveAll(command.tenantCode(), command.signNo(), command.materials());
        intakeStore.save(intake);
        interactionLogStore.recordSuccess(command.tenantCode(), command.signNo(), "IMAGE_UPLOAD", null,
                command.traceId());
        return new ClaimIntakeResult(command.signNo(), intake.status());
    }
}
