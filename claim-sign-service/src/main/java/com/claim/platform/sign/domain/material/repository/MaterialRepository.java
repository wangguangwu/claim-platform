package com.claim.platform.sign.domain.material.repository;

import com.claim.platform.sign.domain.material.model.Material;
import java.util.List;

/** Persists material references that belong to a Sign creation transaction.
 * @author wangguangwu
 */
public interface MaterialRepository {
    void saveAll(List<Material> materials);

    List<Material> listByTenantAndSignNo(String tenantCode, String signNo);
}
