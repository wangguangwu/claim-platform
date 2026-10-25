package com.claim.platform.sign.domain.sign.repository;

import com.claim.platform.sign.domain.sign.model.Sign;
import com.claim.platform.sign.domain.sign.model.SignNo;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Persists and reads Sign facts; tenant-scoped lookups are mandatory for business access.
 *
 * @author wangguangwu
 */
public interface SignRepository {
    Optional<Sign> findByTenantAndExternalApplyNo(String tenantCode, String externalApplyNo);

    Optional<Sign> findByTenantAndSignNo(String tenantCode, SignNo signNo);

    Optional<SignNo> findHighestSignNo();

    List<Sign> findStalledBefore(Instant cutoff, int limit);

    void save(Sign sign);
}
