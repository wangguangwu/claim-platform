package com.claim.platform.sign.application.service;

import com.claim.platform.sign.application.command.SubmitIntakeCommand;
import com.claim.platform.sign.application.result.ClaimIntakeResult;
import com.claim.platform.sign.domain.model.ClaimIntake;
import com.claim.platform.sign.domain.model.InteractionType;
import com.claim.platform.sign.domain.repository.ClaimIntakeStore;
import com.claim.platform.sign.domain.repository.IntakeMaterialStore;
import com.claim.platform.sign.domain.repository.InteractionLogStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 在独立数据库事务内创建签收申请、命令携带的材料和成功交互日志。
 * 独立事务保证唯一键冲突可以整体回滚，外层编排能在回滚后重新查询并发赢家。
 *
 * @author wangguangwu
 */
@Service
@RequiredArgsConstructor
public class ClaimIntakeCreationService {

    private final ClaimIntakeStore intakeStore;
    private final IntakeMaterialStore materialStore;
    private final InteractionLogStore interactionLogStore;

    /**
     * 创建一条签收事实；材料非空时通过聚合行为迁移为材料已上传状态。
     *
     * @param command 标准签收命令
     * @param signNo  平台生成的签收编号
     * @return 新建申请的结果
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ClaimIntakeResult create(SubmitIntakeCommand command, String signNo) {
        boolean hasMaterials = !command.materials().isEmpty();
        ClaimIntake intake = ClaimIntake.create(command.tenantCode(), signNo, command.externalApplyNo(),
                command.applicantName(), command.applicant(), command.accidentPerson(),
                command.mainInsured(), command.payee(), command.extJson());
        if (hasMaterials) {
            intake.registerMaterials();
        }
        intakeStore.insert(intake);
        if (hasMaterials) {
            materialStore.saveAll(command.tenantCode(), signNo, command.materials());
        }
        interactionLogStore.recordSuccess(command.tenantCode(), signNo,
                InteractionType.SIGN_RECEIVE.code(), null, command.traceId());
        return new ClaimIntakeResult(signNo, intake.status());
    }
}
