package com.claim.platform.application.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.claim.platform.application.domain.exception.ConcurrentSignApplicationModificationException;
import com.claim.platform.application.domain.model.SignApplication;
import com.claim.platform.application.domain.model.SignApplicationDetails;
import com.claim.platform.application.domain.model.SignMaterial;
import com.claim.platform.application.domain.repository.InteractionLogStore;
import com.claim.platform.application.domain.repository.SignApplicationStore;
import com.claim.platform.application.domain.repository.SignMaterialStore;
import com.claim.platform.application.infrastructure.persistence.mapper.SignApplicationPersistenceMapper;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * 将 Application Intake 领域持久化端口映射为本上下文拥有的 MySQL/MyBatis-Plus 表。
 * 所有写方法参加应用服务声明的同一个事务。
 *
 * @author wangguangwu
 */
@Repository
public class SignIntakePersistenceAdapter implements SignApplicationStore, SignMaterialStore, InteractionLogStore {

    private final SignApplicationMapper applicationMapper;
    private final SignMaterialMapper materialMapper;
    private final InteractionLogMapper interactionLogMapper;
    private final SignApplicationPersistenceMapper persistenceMapper;

    public SignIntakePersistenceAdapter(SignApplicationMapper applicationMapper, SignMaterialMapper materialMapper,
                                        InteractionLogMapper interactionLogMapper,
                                        SignApplicationPersistenceMapper persistenceMapper) {
        this.applicationMapper = applicationMapper;
        this.materialMapper = materialMapper;
        this.interactionLogMapper = interactionLogMapper;
        this.persistenceMapper = persistenceMapper;
    }

    @Override
    public Optional<SignApplication> findByTenantAndSignNo(String tenantCode, String signNo) {
        return Optional.ofNullable(findDO(tenantCode, signNo)).map(persistenceMapper::toDomain);
    }

    @Override
    public Optional<SignApplication> findByTenantAndExternalApplyNo(String tenantCode, String externalApplyNo) {
        return Optional.ofNullable(applicationMapper.selectOne(new LambdaQueryWrapper<SignApplicationDO>()
                        .eq(SignApplicationDO::getTenantCode, tenantCode)
                        .eq(SignApplicationDO::getExternalApplyNo, externalApplyNo)
                        .eq(SignApplicationDO::isDeleted, false)))
                .map(persistenceMapper::toDomain);
    }

    @Override
    public Optional<SignApplicationDetails> findDetailsByTenantAndSignNo(String tenantCode, String signNo) {
        return Optional.ofNullable(findDO(tenantCode, signNo)).map(dataObject -> new SignApplicationDetails(
                persistenceMapper.toDomain(dataObject),
                materialMapper.selectList(new LambdaQueryWrapper<SignMaterialDO>()
                        .eq(SignMaterialDO::getTenantCode, tenantCode)
                        .eq(SignMaterialDO::getSignNo, signNo)
                        .eq(SignMaterialDO::isDeleted, false)
                        .orderByAsc(SignMaterialDO::getId)).stream().map(persistenceMapper::toDomain).toList()));
    }

    @Override
    public boolean existsByTenantAndExternalApplyNo(String tenantCode, String externalApplyNo) {
        return applicationMapper.exists(new LambdaQueryWrapper<SignApplicationDO>()
                .eq(SignApplicationDO::getTenantCode, tenantCode)
                .eq(SignApplicationDO::getExternalApplyNo, externalApplyNo)
                .eq(SignApplicationDO::isDeleted, false));
    }

    @Override
    public void save(SignApplication application) {
        SignApplicationDO existing = findDO(application.tenantCode(), application.signNo());
        if (existing == null) {
            applicationMapper.insert(persistenceMapper.toDO(application));
            return;
        }
        existing.setStatus(application.status());
        if (applicationMapper.updateById(existing) == 0) {
            throw new ConcurrentSignApplicationModificationException();
        }
    }

    @Override
    public void saveAll(String tenantCode, String signNo, List<SignMaterial> materials) {
        for (SignMaterial material : materials) {
            materialMapper.insert(new SignMaterialDO(tenantCode, signNo, material.batchNo(), material.sequence(),
                    material.type(), material.name(), material.path(), material.originalPath()));
        }
    }

    @Override
    public void recordSuccess(String tenantCode, String signNo, String interactionType, String requestId, String traceId) {
        interactionLogMapper.insert(new InteractionLogDO(tenantCode, signNo, interactionType, requestId, traceId));
    }

    private SignApplicationDO findDO(String tenantCode, String signNo) {
        return applicationMapper.selectOne(new LambdaQueryWrapper<SignApplicationDO>()
                .eq(SignApplicationDO::getTenantCode, tenantCode)
                .eq(SignApplicationDO::getSignNo, signNo)
                .eq(SignApplicationDO::isDeleted, false));
    }
}
