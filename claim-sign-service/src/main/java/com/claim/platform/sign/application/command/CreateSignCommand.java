package com.claim.platform.sign.application.command;

import com.claim.platform.common.tenant.TenantContext;
import java.util.List;

/** Carries a normalized Sign creation request without introducing external protocol DTOs.
 *
 * @author wangguangwu
 */
public record CreateSignCommand(TenantContext tenantContext, String externalApplyNo, String applicantName,
                                int protocolVersion, List<InitialMaterial> initialMaterials) {
    public CreateSignCommand {
        if (tenantContext == null || externalApplyNo == null || externalApplyNo.isBlank()) {
            throw new IllegalArgumentException("tenantContext and externalApplyNo are required");
        }
        initialMaterials = initialMaterials == null ? List.of() : List.copyOf(initialMaterials);
    }
}
