package com.claim.platform.application.adapter.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 定义批量登记影像元数据的 HTTP 请求体，不接收影像二进制内容。
 *
 * @author wangguangwu
 */
public record UploadMaterialsRequest(@NotBlank @Size(max = 64) String batchNo,
                                     @NotEmpty List<@Valid MaterialRequest> materials) {

    /**
     * 定义单条材料的可验证元数据。
     *
     * @author wangguangwu
     */
    public record MaterialRequest(Integer sequence, @Size(max = 32) String type,
                                  @NotBlank @Size(max = 255) String name,
                                  @Size(max = 1000) String path,
                                  @Size(max = 1000) String originalPath) {
    }
}
