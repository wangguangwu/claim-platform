package com.claim.platform.sign.interfaces.web.request;

import com.claim.platform.sign.interfaces.web.SignWebContract;
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
public record UploadMaterialRequest(
        @NotBlank @Size(max = SignWebContract.Limits.BATCH_NO_LENGTH) String batchNo,
        @NotEmpty List<@Valid MaterialRequest> materials) {

    /**
     * 定义单条材料的可验证元数据。
     *
     * @author wangguangwu
     */
    public record MaterialRequest(
            Integer sequence,
            @Size(max = SignWebContract.Limits.MATERIAL_TYPE_LENGTH) String type,
            @NotBlank @Size(max = SignWebContract.Limits.MATERIAL_NAME_LENGTH) String name,
            @Size(max = SignWebContract.Limits.MATERIAL_PATH_LENGTH) String path,
            @Size(max = SignWebContract.Limits.MATERIAL_PATH_LENGTH) String originalPath) {
    }
}
