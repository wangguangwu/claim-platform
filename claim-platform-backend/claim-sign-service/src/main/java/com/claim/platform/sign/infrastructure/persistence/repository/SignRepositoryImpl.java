package com.claim.platform.sign.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.claim.platform.sign.domain.exception.ConcurrentSignModificationException;
import com.claim.platform.sign.domain.model.Sign;
import com.claim.platform.sign.domain.model.SignDetails;
import com.claim.platform.sign.domain.model.Material;
import com.claim.platform.sign.domain.repository.SignRepository;
import com.claim.platform.sign.domain.repository.MaterialRepository;
import com.claim.platform.sign.domain.repository.InteractionLogRepository;
import com.claim.platform.sign.infrastructure.persistence.converter.SignPersistenceConverter;
import com.claim.platform.sign.infrastructure.persistence.dataobject.SignDO;
import com.claim.platform.sign.infrastructure.persistence.dataobject.MaterialDO;
import com.claim.platform.sign.infrastructure.persistence.dataobject.InteractionLogDO;
import com.claim.platform.sign.infrastructure.persistence.mapper.SignMapper;
import com.claim.platform.sign.infrastructure.persistence.mapper.MaterialMapper;
import com.claim.platform.sign.infrastructure.persistence.mapper.InteractionLogMapper;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

/**
 * 将理赔签收领域持久化端口映射为本上下文拥有的 MySQL/MyBatis-Plus 表。
 * 所有写方法参加应用服务声明的同一个事务。
 *
 * @author wangguangwu
 */
@Repository
public class SignRepositoryImpl implements SignRepository, MaterialRepository, InteractionLogRepository {

    private static final Logger log = LoggerFactory.getLogger(SignRepositoryImpl.class);

    private final SignMapper signMapper;
    private final MaterialMapper materialMapper;
    private final InteractionLogMapper interactionLogMapper;
    private final SignPersistenceConverter persistenceConverter;

    public SignRepositoryImpl(SignMapper signMapper, MaterialMapper materialMapper,
                                           InteractionLogMapper interactionLogMapper,
                                           SignPersistenceConverter persistenceConverter) {
        this.signMapper = signMapper;
        this.materialMapper = materialMapper;
        this.interactionLogMapper = interactionLogMapper;
        this.persistenceConverter = persistenceConverter;
    }

    @Override
    public Optional<Sign> findByTenantAndSignNo(String tenantCode, String signNo) {
        return Optional.ofNullable(findDO(tenantCode, signNo)).map(persistenceConverter::toDomain);
    }

    @Override
    public Optional<Sign> findByTenantAndExternalApplyNo(String tenantCode, String externalApplyNo) {
        return Optional.ofNullable(signMapper.selectOne(new LambdaQueryWrapper<SignDO>()
                        .eq(SignDO::getTenantCode, tenantCode)
                        .eq(SignDO::getExternalApplyNo, externalApplyNo)
                        .eq(SignDO::isDeleted, false)))
                .map(persistenceConverter::toDomain);
    }

    @Override
    public Optional<SignDetails> findDetailsByTenantAndSignNo(String tenantCode, String signNo) {
        return Optional.ofNullable(findDO(tenantCode, signNo)).map(dataObject -> new SignDetails(
                persistenceConverter.toDomain(dataObject),
                materialMapper.selectList(new LambdaQueryWrapper<MaterialDO>()
                                .eq(MaterialDO::getTenantCode, tenantCode)
                                .eq(MaterialDO::getSignNo, signNo)
                                .eq(MaterialDO::isDeleted, false)
                                .orderByAsc(MaterialDO::getId))
                        .stream().map(persistenceConverter::toDomain).toList()));
    }

    @Override
    public boolean existsByTenantAndExternalApplyNo(String tenantCode, String externalApplyNo) {
        return signMapper.exists(new LambdaQueryWrapper<SignDO>()
                .eq(SignDO::getTenantCode, tenantCode)
                .eq(SignDO::getExternalApplyNo, externalApplyNo)
                .eq(SignDO::isDeleted, false));
    }

    @Override
    public void insert(Sign sign) {
        signMapper.insert(persistenceConverter.toDO(sign));
        log.debug("inserted new claim sign tenant={} signNo={}", sign.tenantCode(), sign.signNo());
    }

    @Override
    public void save(Sign sign) {
        SignDO existing = findDO(sign.tenantCode(), sign.signNo());
        if (existing == null) {
            signMapper.insert(persistenceConverter.toDO(sign));
            log.debug("inserted new claim sign tenant={} signNo={}", sign.tenantCode(), sign.signNo());
            return;
        }
        existing.setStatus(sign.status());
        if (signMapper.updateById(existing) == 0) {
            throw new ConcurrentSignModificationException();
        }
        log.debug("updated claim sign tenant={} signNo={}", sign.tenantCode(), sign.signNo());
    }

    @Override
    public void saveAll(String tenantCode, String signNo, List<Material> materials) {
        for (Material material : materials) {
            materialMapper.insert(new MaterialDO(tenantCode, signNo, material.batchNo(), material.sequence(),
                    material.type(), material.name(), material.path(), material.originalPath()));
        }
    }

    @Override
    public void recordSuccess(String tenantCode, String signNo, String interactionType, String requestId,
                              String traceId) {
        interactionLogMapper.insert(new InteractionLogDO(tenantCode, signNo, interactionType, requestId, traceId));
    }

    private SignDO findDO(String tenantCode, String signNo) {
        return signMapper.selectOne(new LambdaQueryWrapper<SignDO>()
                .eq(SignDO::getTenantCode, tenantCode)
                .eq(SignDO::getSignNo, signNo)
                .eq(SignDO::isDeleted, false));
    }
}
