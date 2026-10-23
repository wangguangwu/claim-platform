package com.claim.platform.sign.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.claim.platform.sign.domain.exception.ConcurrentIntakeModificationException;
import com.claim.platform.sign.domain.model.ClaimIntake;
import com.claim.platform.sign.domain.model.ClaimIntakeDetails;
import com.claim.platform.sign.domain.model.IntakeMaterial;
import com.claim.platform.sign.domain.repository.ClaimIntakeStore;
import com.claim.platform.sign.domain.repository.IntakeMaterialStore;
import com.claim.platform.sign.domain.repository.InteractionLogStore;
import com.claim.platform.sign.infrastructure.persistence.converter.ClaimIntakePersistenceConverter;
import com.claim.platform.sign.infrastructure.persistence.dataobject.ClaimIntakeDO;
import com.claim.platform.sign.infrastructure.persistence.dataobject.IntakeMaterialDO;
import com.claim.platform.sign.infrastructure.persistence.dataobject.InteractionLogDO;
import com.claim.platform.sign.infrastructure.persistence.mapper.ClaimIntakeMapper;
import com.claim.platform.sign.infrastructure.persistence.mapper.IntakeMaterialMapper;
import com.claim.platform.sign.infrastructure.persistence.mapper.InteractionLogMapper;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

/**
 * 将 Claim Intake 领域持久化端口映射为本上下文拥有的 MySQL/MyBatis-Plus 表。
 * 所有写方法参加应用服务声明的同一个事务。
 *
 * @author wangguangwu
 */
@Repository
public class IntakePersistenceRepositoryImpl implements ClaimIntakeStore, IntakeMaterialStore, InteractionLogStore {

    private static final Logger log = LoggerFactory.getLogger(IntakePersistenceRepositoryImpl.class);

    private final ClaimIntakeMapper intakeMapper;
    private final IntakeMaterialMapper materialMapper;
    private final InteractionLogMapper interactionLogMapper;
    private final ClaimIntakePersistenceConverter persistenceConverter;

    public IntakePersistenceRepositoryImpl(ClaimIntakeMapper intakeMapper, IntakeMaterialMapper materialMapper,
                                           InteractionLogMapper interactionLogMapper,
                                           ClaimIntakePersistenceConverter persistenceConverter) {
        this.intakeMapper = intakeMapper;
        this.materialMapper = materialMapper;
        this.interactionLogMapper = interactionLogMapper;
        this.persistenceConverter = persistenceConverter;
    }

    @Override
    public Optional<ClaimIntake> findByTenantAndSignNo(String tenantCode, String signNo) {
        return Optional.ofNullable(findDO(tenantCode, signNo)).map(persistenceConverter::toDomain);
    }

    @Override
    public Optional<ClaimIntake> findByTenantAndExternalApplyNo(String tenantCode, String externalApplyNo) {
        return Optional.ofNullable(intakeMapper.selectOne(new LambdaQueryWrapper<ClaimIntakeDO>()
                        .eq(ClaimIntakeDO::getTenantCode, tenantCode)
                        .eq(ClaimIntakeDO::getExternalApplyNo, externalApplyNo)
                        .eq(ClaimIntakeDO::isDeleted, false)))
                .map(persistenceConverter::toDomain);
    }

    @Override
    public Optional<ClaimIntakeDetails> findDetailsByTenantAndSignNo(String tenantCode, String signNo) {
        return Optional.ofNullable(findDO(tenantCode, signNo)).map(dataObject -> new ClaimIntakeDetails(
                persistenceConverter.toDomain(dataObject),
                materialMapper.selectList(new LambdaQueryWrapper<IntakeMaterialDO>()
                                .eq(IntakeMaterialDO::getTenantCode, tenantCode)
                                .eq(IntakeMaterialDO::getSignNo, signNo)
                                .eq(IntakeMaterialDO::isDeleted, false)
                                .orderByAsc(IntakeMaterialDO::getId))
                        .stream().map(persistenceConverter::toDomain).toList()));
    }

    @Override
    public boolean existsByTenantAndExternalApplyNo(String tenantCode, String externalApplyNo) {
        return intakeMapper.exists(new LambdaQueryWrapper<ClaimIntakeDO>()
                .eq(ClaimIntakeDO::getTenantCode, tenantCode)
                .eq(ClaimIntakeDO::getExternalApplyNo, externalApplyNo)
                .eq(ClaimIntakeDO::isDeleted, false));
    }

    @Override
    public void insert(ClaimIntake intake) {
        intakeMapper.insert(persistenceConverter.toDO(intake));
        log.debug("inserted new claim intake tenant={} signNo={}", intake.tenantCode(), intake.signNo());
    }

    @Override
    public void save(ClaimIntake intake) {
        ClaimIntakeDO existing = findDO(intake.tenantCode(), intake.signNo());
        if (existing == null) {
            intakeMapper.insert(persistenceConverter.toDO(intake));
            log.debug("inserted new claim intake tenant={} signNo={}", intake.tenantCode(), intake.signNo());
            return;
        }
        existing.setStatus(intake.status());
        if (intakeMapper.updateById(existing) == 0) {
            throw new ConcurrentIntakeModificationException();
        }
        log.debug("updated claim intake tenant={} signNo={}", intake.tenantCode(), intake.signNo());
    }

    @Override
    public void saveAll(String tenantCode, String signNo, List<IntakeMaterial> materials) {
        for (IntakeMaterial material : materials) {
            materialMapper.insert(new IntakeMaterialDO(tenantCode, signNo, material.batchNo(), material.sequence(),
                    material.type(), material.name(), material.path(), material.originalPath()));
        }
    }

    @Override
    public void recordSuccess(String tenantCode, String signNo, String interactionType, String requestId,
                              String traceId) {
        interactionLogMapper.insert(new InteractionLogDO(tenantCode, signNo, interactionType, requestId, traceId));
    }

    private ClaimIntakeDO findDO(String tenantCode, String signNo) {
        return intakeMapper.selectOne(new LambdaQueryWrapper<ClaimIntakeDO>()
                .eq(ClaimIntakeDO::getTenantCode, tenantCode)
                .eq(ClaimIntakeDO::getSignNo, signNo)
                .eq(ClaimIntakeDO::isDeleted, false));
    }
}
