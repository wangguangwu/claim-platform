package com.claim.platform.sign.application.command;

import com.claim.platform.sign.domain.interactionlog.model.InteractionDirection;
import com.claim.platform.sign.domain.interactionlog.model.InteractionLog;
import com.claim.platform.sign.domain.interactionlog.model.InteractionType;
import com.claim.platform.sign.domain.interactionlog.repository.InteractionLogRepository;
import com.claim.platform.sign.domain.material.model.Material;
import com.claim.platform.sign.domain.material.repository.MaterialRepository;
import com.claim.platform.sign.domain.sign.model.Sign;
import com.claim.platform.sign.domain.sign.repository.SignRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Commits the initial Sign fact, its material references, and submission audit as one transaction.
 *
 * @author wangguangwu
 */
@Service
public class SignCreationTransaction {
    private final SignRepository signRepository;
    private final MaterialRepository materialRepository;
    private final InteractionLogRepository interactionLogRepository;

    public SignCreationTransaction(SignRepository signRepository, MaterialRepository materialRepository,
                                   InteractionLogRepository interactionLogRepository) {
        this.signRepository = signRepository;
        this.materialRepository = materialRepository;
        this.interactionLogRepository = interactionLogRepository;
    }

    /** Atomically records the creation facts before any event intent can be expressed. */
    @Transactional
    public void create(Sign sign, List<Material> materials, String traceId) {
        signRepository.save(sign);
        materialRepository.saveAll(materials);
        interactionLogRepository.append(new InteractionLog(sign.tenantCode(), sign.signNo().value(),
                InteractionType.SIGN_SUBMIT, InteractionDirection.IN, "tenant", "claim-sign-service", null,
                traceId, "SUCCESS", null, "Sign fact accepted", 0L, Instant.now()));
    }
}
