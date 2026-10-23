package com.claim.platform.intake.interfaces.tenant;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * 验证租户协议转换层的依赖边界和禁止分支规则。
 *
 * @author wangguangwu
 */
class TenantAdapterArchitectureTest {

    private static final Path MODULE_MAIN_SOURCES = Path.of("src/main/java/com/claim/platform/intake");
    private static final Path REPOSITORY_MAIN_SOURCES =
            Path.of("backend/claim-intake-service/src/main/java/com/claim/platform/intake");
    private static final Pattern CONDITIONAL_ROUTING = Pattern.compile(
            "\\b(if|else\\s+if)\\s*\\([^)]*\\b(tenantCode|protocolVersion)\\b[^)]*\\)");
    private static final Pattern SWITCH = Pattern.compile("\\bswitch\\s*\\(");

    @Test
    void applicationAndDomainShouldNotDependOnTenantAdapter() throws IOException {
        Path mainSources = mainSources();
        assertThat(mainSources).isDirectory();
        List<Path> sources = new ArrayList<>();
        for (String layer : List.of("application", "domain")) {
            try (Stream<Path> paths = Files.walk(mainSources.resolve(layer))) {
                paths.filter(path -> path.toString().endsWith(".java")).forEach(sources::add);
            }
        }

        assertThat(sources).isNotEmpty();
        assertThat(sources).allSatisfy(path ->
                assertThat(read(path)).as(path.toString()).doesNotContain("interfaces.tenant"));
    }

    @Test
    void routingCodeShouldNotBranchOnTenantOrProtocol() {
        Path mainSources = mainSources();
        List<Path> routingSources = List.of(
                mainSources.resolve("interfaces/tenant/core/pipeline/DefaultIntakeConvertPipeline.java"),
                mainSources.resolve("interfaces/tenant/core/registry/TenantProtocolHandlerRegistry.java"));

        assertThat(routingSources).allSatisfy(path -> assertThat(path).isRegularFile());
        for (Path path : routingSources) {
            String source = read(path);
            assertThat(source).as(path.toString()).doesNotContainPattern(SWITCH);
            assertThat(source).as(path.toString()).doesNotContainPattern(CONDITIONAL_ROUTING);
        }
    }

    private Path mainSources() {
        return Files.isDirectory(MODULE_MAIN_SOURCES) ? MODULE_MAIN_SOURCES : REPOSITORY_MAIN_SOURCES;
    }

    private String read(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }
}
