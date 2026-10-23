package com.claim.platform.sign.domain.model;

/**
 * 表示签收申请关联的影像元数据，不承载或读取影像二进制内容。
 *
 * @author wangguangwu
 */
public record IntakeMaterial(String batchNo, Integer sequence, String type, String name, String path,
                             String originalPath) {
}
