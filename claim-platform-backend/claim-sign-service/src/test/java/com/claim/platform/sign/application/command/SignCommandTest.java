package com.claim.platform.sign.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.claim.platform.sign.domain.model.Material;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * 验证标准命令对材料列表的不可变语义。
 *
 * @author wangguangwu
 */
class SignCommandTest {

    private static final Material MATERIAL = new Material("BATCH-1", 1, "ID_CARD", "身份证.jpg",
            "/tmp/a", "/tmp/b");

    @Test
    void shouldNormalizeNullMaterialsToEmptyList() {
        SignCommand command = command(null);

        assertThat(command.materials()).isEmpty();
    }

    @Test
    void shouldCopyMaterialsIntoImmutableList() {
        List<Material> source = new ArrayList<>();
        source.add(MATERIAL);

        SignCommand command = command(source);
        source.clear();

        assertThat(command.materials()).containsExactly(MATERIAL);
        assertThatThrownBy(() -> command.materials().add(MATERIAL))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    private SignCommand command(List<Material> materials) {
        return new SignCommand("tenant-a", "trace-001", "EXT001", "张三", null, null, null, null,
                materials, "{}");
    }
}
