package com.claim.platform.sign.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.claim.platform.sign.domain.sign.model.Sign;
import com.claim.platform.sign.domain.sign.model.SignNo;
import com.claim.platform.sign.domain.sign.repository.SignRepository;
import com.claim.platform.sign.infrastructure.persistence.converter.SignPersistenceConverter;
import com.claim.platform.sign.infrastructure.persistence.dataobject.SignDO;
import com.claim.platform.sign.infrastructure.persistence.mapper.SignMapper;
import java.util.Optional;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.stereotype.Repository;

/** Implements tenant-scoped Sign fact access with MyBatis-Plus.
 *
 * @author wangguangwu
 */
@Repository
public class SignRepositoryImpl implements SignRepository {
    private final SignMapper mapper;
    private final SignPersistenceConverter converter;
    public SignRepositoryImpl(SignMapper mapper, SignPersistenceConverter converter) {
        this.mapper = mapper;
        this.converter = converter;
    }
    @Override public Optional<Sign> findByTenantAndExternalApplyNo(String tenantCode, String externalApplyNo) {
        return Optional.ofNullable(mapper.selectOne(Wrappers.<SignDO>lambdaQuery()
                .eq(SignDO::getTenantCode, tenantCode).eq(SignDO::getExternalApplyNo, externalApplyNo)
                .eq(SignDO::getIsDeleted, 0))).map(converter::toDomain);
    }
    @Override public Optional<Sign> findByTenantAndSignNo(String tenantCode, SignNo signNo) {
        return Optional.ofNullable(mapper.selectOne(Wrappers.<SignDO>lambdaQuery()
                .eq(SignDO::getTenantCode, tenantCode).eq(SignDO::getSignNo, signNo.value())
                .eq(SignDO::getIsDeleted, 0))).map(converter::toDomain);
    }
    @Override public Optional<SignNo> findHighestSignNo() {
        return mapper.selectList(Wrappers.<SignDO>lambdaQuery().eq(SignDO::getIsDeleted, 0)
                        .orderByDesc(SignDO::getSignNo).last("LIMIT 1"))
                .stream().findFirst().map(value -> new SignNo(value.getSignNo()));
    }
    @Override public List<Sign> findStalledBefore(Instant cutoff, int limit) {
        return mapper.selectList(Wrappers.<SignDO>lambdaQuery().eq(SignDO::getIsDeleted, 0)
                        .lt(SignDO::getUpdatedAt, LocalDateTime.ofInstant(cutoff, ZoneOffset.UTC))
                        .orderByAsc(SignDO::getUpdatedAt).last("LIMIT " + Math.max(1, limit)))
                .stream().map(converter::toDomain).toList();
    }
    @Override public void save(Sign sign) { mapper.insert(converter.toDataObject(sign)); }
}
