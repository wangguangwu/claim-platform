package com.claim.platform.sign.infrastructure.persistence.repository;

import com.claim.platform.sign.domain.interactionlog.model.InteractionLog;
import com.claim.platform.sign.domain.interactionlog.repository.InteractionLogRepository;
import com.claim.platform.sign.infrastructure.persistence.dataobject.InteractionLogDO;
import com.claim.platform.sign.infrastructure.persistence.mapper.InteractionLogMapper;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.springframework.stereotype.Repository;

/** Persists a safe interaction summary and deliberately excludes request and response bodies.
 *
 * @author wangguangwu
 */
@Repository
public class InteractionLogRepositoryImpl implements InteractionLogRepository {
    private final InteractionLogMapper mapper;

    public InteractionLogRepositoryImpl(InteractionLogMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void append(InteractionLog log) {
        InteractionLogDO data = new InteractionLogDO();
        data.setTenantCode(log.tenantCode());
        data.setSignNo(log.signNo());
        data.setInteractionType(log.interactionType().name());
        data.setDirection(log.direction().name());
        data.setSourceSystem(log.sourceSystem());
        data.setTargetSystem(log.targetSystem());
        data.setRequestId(log.requestId());
        data.setTraceId(log.traceId());
        data.setResult(log.result());
        data.setErrorCode(log.errorCode());
        data.setMessage(log.message());
        data.setCostMs(log.costMs());
        data.setCreatedAt(LocalDateTime.ofInstant(log.occurredAt(), ZoneOffset.UTC));
        mapper.insert(data);
    }
}
