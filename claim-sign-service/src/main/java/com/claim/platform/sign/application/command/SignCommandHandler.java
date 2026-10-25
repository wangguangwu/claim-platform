package com.claim.platform.sign.application.command;

import com.claim.platform.common.trace.TraceContext;
import com.claim.platform.sign.application.port.BusinessLock;
import com.claim.platform.sign.application.port.EventPublisher;
import com.claim.platform.sign.application.port.SignNumberGenerator;
import com.claim.platform.sign.application.result.SignResult;
import com.claim.platform.sign.application.exception.ConcurrentSignModificationException;
import com.claim.platform.sign.application.port.SignAcceptedEvent;
import com.claim.platform.sign.domain.material.model.Material;
import com.claim.platform.sign.domain.sign.model.Sign;
import com.claim.platform.sign.domain.sign.repository.SignRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Implements the fact-store fast path and lock-protected Sign creation path.
 *
 * @author wangguangwu
 */
@Service
public class SignCommandHandler {
    private static final Logger LOG = LoggerFactory.getLogger(SignCommandHandler.class);
    private final SignRepository repository;
    private final BusinessLock businessLock;
    private final SignNumberGenerator numberGenerator;
    private final EventPublisher eventPublisher;
    private final SignCreationTransaction creationTransaction;
    public SignCommandHandler(SignRepository repository, BusinessLock businessLock, SignNumberGenerator numberGenerator,
                              EventPublisher eventPublisher, SignCreationTransaction creationTransaction) {
        this.repository = repository; this.businessLock = businessLock; this.numberGenerator = numberGenerator;
        this.eventPublisher = eventPublisher;
        this.creationTransaction = creationTransaction;
    }
    /** Creates once per tenant business key and publishes only for the winning creation. */
    public SignResult create(CreateSignCommand command) {
        String tenantCode = command.tenantContext().tenantCode();
        return repository.findByTenantAndExternalApplyNo(tenantCode, command.externalApplyNo())
                .map(sign -> result(sign, true))
                .orElseGet(() -> businessLock.execute(tenantCode + ":" + command.externalApplyNo(), () -> createLocked(command)));
    }
    private SignResult createLocked(CreateSignCommand command) {
        String tenantCode = command.tenantContext().tenantCode();
        return repository.findByTenantAndExternalApplyNo(tenantCode, command.externalApplyNo()).map(sign -> result(sign, true))
                .orElseGet(() -> createNew(command));
    }
    private SignResult createNew(CreateSignCommand command) {
        Sign sign = new Sign(command.tenantContext().tenantCode(), numberGenerator.next(), command.externalApplyNo(),
                command.applicantName(), command.protocolVersion());
        String traceId = TraceContext.currentOrCreate();
        try { creationTransaction.create(sign, bindMaterials(command.initialMaterials(), sign), traceId); } catch (DuplicateKeyException exception) {
            return repository.findByTenantAndExternalApplyNo(sign.tenantCode(), sign.externalApplyNo())
                    .map(found -> result(found, true)).orElseThrow(() -> new ConcurrentSignModificationException(
                            sign.tenantCode(), sign.externalApplyNo(), exception));
        }
        try {
            eventPublisher.publishSignAccepted(new SignAcceptedEvent(UUID.randomUUID().toString(), 1,
                    sign.tenantCode(), "Sign", sign.signNo().value(), Instant.now(), traceId));
        } catch (RuntimeException exception) {
            LOG.error("eventIntent=SIGN_ACCEPT_PUBLISH_FAILED tenantCode={} signNo={} traceId={} recoveryAction=COMPENSATE_PENDING_ACCEPT",
                    sign.tenantCode(), sign.signNo().value(), traceId, exception);
        }
        return result(sign, false);
    }
    private List<Material> bindMaterials(List<InitialMaterial> materials, Sign sign) {
        return materials.stream().map(material -> new Material(sign.tenantCode(), sign.signNo().value(),
                material.batchNo(), material.type(), material.name(), material.path(), material.originalPath(),
                false)).toList();
    }
    private SignResult result(Sign sign, boolean replayed) { return new SignResult(sign.signNo().value(), sign.status().name(), replayed); }
}
