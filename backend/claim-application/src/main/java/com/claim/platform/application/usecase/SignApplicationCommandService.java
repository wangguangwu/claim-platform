package com.claim.platform.application.usecase;

import com.claim.platform.application.usecase.command.SubmitSignApplicationCommand;
import com.claim.platform.application.usecase.command.UploadMaterialsCommand;
import com.claim.platform.application.usecase.result.SignApplicationResult;
import com.claim.platform.application.domain.exception.SignApplicationNotFoundException;
import com.claim.platform.application.domain.model.SignApplication;
import com.claim.platform.application.domain.repository.InteractionLogStore;
import com.claim.platform.application.domain.repository.SignApplicationStore;
import com.claim.platform.application.domain.repository.SignMaterialStore;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 在 Application Intake 限界上下文编排签收申请和材料登记，
 * 确保业务事实与交互日志在同一事务内提交。
 *
 * @author wangguangwu
 */
@Service
public class SignApplicationCommandService {

    private final SignApplicationStore applicationStore;
    private final SignMaterialStore materialStore;
    private final InteractionLogStore interactionLogStore;

    public SignApplicationCommandService(SignApplicationStore applicationStore, SignMaterialStore materialStore,
                                         InteractionLogStore interactionLogStore) {
        this.applicationStore = applicationStore;
        this.materialStore = materialStore;
        this.interactionLogStore = interactionLogStore;
    }

    /**
     * 接收外部申请并创建待上传材料的签收事实。
     * 相同租户和外部申请编号仅创建一次，并回放既有签收编号。
     *
     * @param command 已从受信任调用链取得租户的命令
     * @return 首次创建或按外部申请编号回放的申请结果
     */
    @Transactional
    public SignApplicationResult submit(SubmitSignApplicationCommand command) {
        SignApplication existing = applicationStore.findByTenantAndExternalApplyNo(command.tenantCode(),
                        command.externalApplyNo())
                .orElse(null);
        if (existing != null) {
            return new SignApplicationResult(existing.signNo(), existing.status());
        }
        String signNo = "SIGN" + UUID.randomUUID().toString().replace("-", "").substring(0, 20).toUpperCase();
        SignApplication application = SignApplication.create(command.tenantCode(), signNo, command.externalApplyNo(),
                command.applicantName(), command.applicantInfo(), command.accidentPersonInfo(),
                command.mainInsuredInfo(), command.payeeInfo(), command.extJson());
        try {
            applicationStore.save(application);
        } catch (DataIntegrityViolationException exception) {
            SignApplication concurrent = applicationStore.findByTenantAndExternalApplyNo(command.tenantCode(),
                            command.externalApplyNo())
                    .orElseThrow(() -> exception);
            return new SignApplicationResult(concurrent.signNo(), concurrent.status());
        }
        interactionLogStore.recordSuccess(command.tenantCode(), signNo, "SIGN_RECEIVE", null, command.traceId());
        return new SignApplicationResult(signNo, application.status());
    }

    /**
     * 登记材料并将允许补材的申请切换至材料已上传状态。
     *
     * @param command 已按租户和签收编号定位的材料命令
     * @return 材料登记后的申请结果
     */
    @Transactional
    public SignApplicationResult uploadMaterials(UploadMaterialsCommand command) {
        SignApplication application = applicationStore.findByTenantAndSignNo(command.tenantCode(), command.signNo())
                .orElseThrow(SignApplicationNotFoundException::new);
        application.registerMaterials();
        materialStore.saveAll(command.tenantCode(), command.signNo(), command.materials());
        applicationStore.save(application);
        interactionLogStore.recordSuccess(command.tenantCode(), command.signNo(), "IMAGE_UPLOAD", null,
                command.traceId());
        return new SignApplicationResult(command.signNo(), application.status());
    }
}
