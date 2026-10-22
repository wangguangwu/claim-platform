package com.claim.platform.application.usecase;

import com.claim.platform.application.usecase.command.SubmitSignApplicationCommand;
import com.claim.platform.application.usecase.command.UploadMaterialsCommand;
import com.claim.platform.application.usecase.result.SignApplicationResult;
import com.claim.platform.application.domain.event.OutboxEvent;
import com.claim.platform.application.domain.exception.IdempotencyKeyReusedException;
import com.claim.platform.application.domain.exception.IdempotencyRequestInProgressException;
import com.claim.platform.application.domain.exception.SignApplicationAlreadyExistsException;
import com.claim.platform.application.domain.exception.SignApplicationNotFoundException;
import com.claim.platform.application.domain.model.SignApplication;
import com.claim.platform.application.domain.repository.IdempotencyStore;
import com.claim.platform.application.domain.repository.InteractionLogStore;
import com.claim.platform.application.domain.repository.OutboxStore;
import com.claim.platform.application.domain.repository.SignApplicationStore;
import com.claim.platform.application.domain.repository.SignMaterialStore;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 在 Application Intake 限界上下文编排签收申请和材料登记，
 * 确保业务事实、交互日志、Outbox 与幂等结果在同一事务内提交。
 *
 * @author wangguangwu
 */
@Service
public class SignApplicationCommandService {

    private static final String SUBMIT_OPERATION = "SUBMIT_SIGN";
    private static final String UPLOAD_OPERATION = "UPLOAD_MATERIALS";
    private final SignApplicationStore applicationStore;
    private final SignMaterialStore materialStore;
    private final IdempotencyStore idempotencyStore;
    private final InteractionLogStore interactionLogStore;
    private final OutboxStore outboxStore;

    public SignApplicationCommandService(SignApplicationStore applicationStore, SignMaterialStore materialStore,
                                         IdempotencyStore idempotencyStore, InteractionLogStore interactionLogStore,
                                         OutboxStore outboxStore) {
        this.applicationStore = applicationStore;
        this.materialStore = materialStore;
        this.idempotencyStore = idempotencyStore;
        this.interactionLogStore = interactionLogStore;
        this.outboxStore = outboxStore;
    }

    /**
     * 接收外部申请并创建待上传材料的签收事实。
     * 相同租户和幂等键仅创建一次，外部申请编号重复则拒绝。
     *
     * @param command 已从受信任调用链取得租户的命令
     * @return 首次创建或幂等回放的申请结果
     */
    @Transactional
    public SignApplicationResult submit(SubmitSignApplicationCommand command) {
        String requestHash = hash(command.externalApplyNo() + "|" + nullable(command.applicantName()) + "|"
                + nullable(command.applicantInfo()) + "|" + nullable(command.extJson()));
        SignApplicationResult replay = replayIfPresent(command.tenantCode(), SUBMIT_OPERATION,
                command.idempotencyKey(), requestHash);
        if (replay != null) {
            return replay;
        }
        if (applicationStore.existsByTenantAndExternalApplyNo(command.tenantCode(), command.externalApplyNo())) {
            throw new SignApplicationAlreadyExistsException();
        }
        String signNo = "SIGN" + UUID.randomUUID().toString().replace("-", "").substring(0, 20).toUpperCase();
        SignApplication application = SignApplication.create(command.tenantCode(), signNo, command.externalApplyNo(),
                command.applicantName(), command.applicantInfo(), command.accidentPersonInfo(),
                command.mainInsuredInfo(), command.payeeInfo(), command.extJson());
        applicationStore.save(application);
        interactionLogStore.recordSuccess(command.tenantCode(), signNo, "SIGN_RECEIVE", command.idempotencyKey(), command.traceId());
        outboxStore.append(event("SignApplicationReceived.v1", application, command.traceId()));
        idempotencyStore.complete(command.tenantCode(), SUBMIT_OPERATION, command.idempotencyKey(), signNo);
        return new SignApplicationResult(signNo, application.status());
    }

    /**
     * 登记材料并将允许补材的申请切换至材料已上传状态。
     *
     * @param command 已按租户和签收编号定位的材料命令
     * @return 首次写入或幂等回放的申请结果
     */
    @Transactional
    public SignApplicationResult uploadMaterials(UploadMaterialsCommand command) {
        String requestHash = hash(command.signNo() + "|" + command.materials());
        SignApplicationResult replay = replayIfPresent(command.tenantCode(), UPLOAD_OPERATION,
                command.idempotencyKey(), requestHash);
        if (replay != null) {
            return replay;
        }
        SignApplication application = applicationStore.findByTenantAndSignNo(command.tenantCode(), command.signNo())
                .orElseThrow(SignApplicationNotFoundException::new);
        application.registerMaterials();
        materialStore.saveAll(command.tenantCode(), command.signNo(), command.materials());
        applicationStore.save(application);
        interactionLogStore.recordSuccess(command.tenantCode(), command.signNo(), "IMAGE_UPLOAD",
                command.idempotencyKey(), command.traceId());
        outboxStore.append(event("SignMaterialsRegistered.v1", application, command.traceId()));
        idempotencyStore.complete(command.tenantCode(), UPLOAD_OPERATION, command.idempotencyKey(), command.signNo());
        return new SignApplicationResult(command.signNo(), application.status());
    }

    private SignApplicationResult replayIfPresent(String tenantCode, String operation, String idempotencyKey,
                                                  String requestHash) {
        if (idempotencyStore.tryAcquire(tenantCode, operation, idempotencyKey, requestHash)) {
            return null;
        }
        IdempotencyStore.IdempotencyRecord record = idempotencyStore.find(tenantCode, operation, idempotencyKey)
                .orElseThrow(IdempotencyRequestInProgressException::new);
        if (!record.requestHash().equals(requestHash)) {
            throw new IdempotencyKeyReusedException();
        }
        if (!"COMPLETED".equals(record.processingStatus()) || record.resultSignNo() == null) {
            throw new IdempotencyRequestInProgressException();
        }
        SignApplication application = applicationStore.findByTenantAndSignNo(tenantCode, record.resultSignNo())
                .orElseThrow(SignApplicationNotFoundException::new);
        return new SignApplicationResult(application.signNo(), application.status());
    }

    private OutboxEvent event(String eventType, SignApplication application, String traceId) {
        String eventId = UUID.randomUUID().toString();
        String payload = "{\"eventId\":\"" + eventId + "\",\"tenantCode\":\"" + application.tenantCode()
                + "\",\"signNo\":\"" + application.signNo() + "\",\"status\":\"" + application.status() + "\"}";
        return new OutboxEvent(eventId, eventType, "v1", application.tenantCode(), "SignApplication",
                application.signNo(), payload, traceId, Instant.now());
    }

    private String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 不可用", exception);
        }
    }

    private String nullable(String value) {
        return value == null ? "<null>" : value;
    }
}
