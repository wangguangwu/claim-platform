package com.claim.platform.gateway.config;

import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Controlled development-only credential-to-tenant mapping; credentials never originate in business payloads.
 *
 * @author wangguangwu
 */
@ConfigurationProperties(prefix = "claim.gateway.local-tenant")
public class LocalTenantProperties {
    private boolean enabled;
    private Map<String, String> credentials = Map.of();

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public Map<String, String> getCredentials() { return credentials; }
    public void setCredentials(Map<String, String> credentials) { this.credentials = credentials; }
}
