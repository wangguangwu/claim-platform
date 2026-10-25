package com.claim.platform.common.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

/** Verifies that trusted tenant context cannot be constructed from an empty identity. @author wangguangwu */
class TenantContextTest {
    @Test
    void shouldRejectBlankTenantCode() {
        assertThatIllegalArgumentException().isThrownBy(() -> new TenantContext(" "));
    }

    @Test
    void shouldRetainTrustedTenantCode() {
        assertThat(new TenantContext("tenant-a").tenantCode()).isEqualTo("tenant-a");
    }
}
