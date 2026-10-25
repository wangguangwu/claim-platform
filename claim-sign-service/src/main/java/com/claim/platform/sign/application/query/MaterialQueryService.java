package com.claim.platform.sign.application.query;

import com.claim.platform.common.tenant.TenantContext;
import com.claim.platform.sign.application.result.MaterialView;
import com.claim.platform.sign.domain.material.repository.MaterialRepository;
import java.util.List;
import org.springframework.stereotype.Service;

/** Reads material metadata within a trusted tenant boundary.
 *
 * @author wangguangwu
 */
@Service
public class MaterialQueryService {
    private final MaterialRepository repository;
    public MaterialQueryService(MaterialRepository repository) { this.repository = repository; }
    /** Lists material references for the given tenant Sign without leaking other tenant rows. */
    public List<MaterialView> listBySignNo(TenantContext context, String signNo) {
        return repository.listByTenantAndSignNo(context.tenantCode(), signNo).stream()
                .map(value -> new MaterialView(value.batchNo(), value.type(), value.name(), value.path(),
                        value.originalPath(), value.downloaded())).toList();
    }
}
