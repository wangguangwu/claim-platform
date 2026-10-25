package com.claim.platform.gateway.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

/** Tests controlled local credential mapping. @author wangguangwu */
class LocalTenantPropertiesTest {
    @Test
    void shouldKeepConfiguredCredentialMappingSeparateFromBusinessInput() {
        LocalTenantProperties properties = new LocalTenantProperties();
        properties.setEnabled(true);
        properties.setCredentials(Map.of("local-token", "tenant-a"));
        assertThat(properties.getCredentials()).containsEntry("local-token", "tenant-a");
    }
}
