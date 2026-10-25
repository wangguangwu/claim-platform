package com.claim.platform.sign.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.claim.platform.sign.domain.material.model.Material;
import com.claim.platform.sign.domain.material.repository.MaterialRepository;
import com.claim.platform.sign.infrastructure.persistence.dataobject.MaterialDO;
import com.claim.platform.sign.infrastructure.persistence.mapper.MaterialMapper;
import java.util.List;
import org.springframework.stereotype.Repository;

/** Stores only material metadata and object-storage references. @author wangguangwu */
@Repository public class MaterialRepositoryImpl implements MaterialRepository {
    private final MaterialMapper mapper;
    public MaterialRepositoryImpl(MaterialMapper mapper){this.mapper=mapper;}
    @Override public void saveAll(List<Material> materials){ for(Material value:materials){ MaterialDO data=new MaterialDO(); data.setTenantCode(value.tenantCode()); data.setSignNo(value.signNo()); data.setBatchNo(value.batchNo()); data.setType(value.type()); data.setName(value.name()); data.setPath(value.path()); data.setOriginalPath(value.originalPath()); data.setIsDownloaded(value.downloaded()?1:0); data.setIsDeleted(0); mapper.insert(data); } }
    @Override public List<Material> listByTenantAndSignNo(String tenantCode, String signNo) {
        return mapper.selectList(Wrappers.<MaterialDO>lambdaQuery().eq(MaterialDO::getTenantCode, tenantCode)
                .eq(MaterialDO::getSignNo, signNo).eq(MaterialDO::getIsDeleted, 0)).stream()
                .map(value -> new Material(value.getTenantCode(), value.getSignNo(), value.getBatchNo(),
                        value.getType(), value.getName(), value.getPath(), value.getOriginalPath(),
                        value.getIsDownloaded() == 1)).toList();
    }
}
