package com.claim.platform.sign.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * 验证签收号发号相关代码保持分层边界：
 * 应用层和领域层不直接依赖 Redis、Redisson、MyBatis、JDBC 或基础设施实现，领域层不依赖 Spring。
 *
 * @author wangguangwu
 */
class SignNoArchitectureTest {

    private static final Path MODULE_MAIN_SOURCES = Path.of("src/main/java/com/claim/platform/sign");
    private static final Path REPOSITORY_MAIN_SOURCES =
            Path.of("claim-platform-backend/claim-sign-service/src/main/java/com/claim/platform/sign");
    private static final List<String> STORAGE_DEPENDENCIES = List.of(
            "org.springframework.data.redis",
            "org.redisson",
            "org.apache.ibatis",
            "com.baomidou",
            "org.springframework.jdbc",
            "java.sql",
            "javax.sql");

    @Test
    void applicationShouldNotDependOnStorageOrInfrastructure() {
        List<Path> sources = layerSources("application");

        assertThat(sources).isNotEmpty();
        assertThat(sources).allSatisfy(path -> {
            String source = read(path);
            assertThat(source).as(path.toString()).doesNotContain("com.claim.platform.sign.infrastructure");
            assertThat(STORAGE_DEPENDENCIES).allSatisfy(dependency ->
                    assertThat(source).as(path.toString()).doesNotContain(dependency));
        });
    }

    @Test
    void domainShouldNotDependOnSpringStorageOrInfrastructure() {
        List<Path> sources = layerSources("domain");

        assertThat(sources).isNotEmpty();
        assertThat(sources).allSatisfy(path -> {
            String source = read(path);
            assertThat(source).as(path.toString()).doesNotContain("org.springframework");
            assertThat(source).as(path.toString()).doesNotContain("com.claim.platform.sign.infrastructure");
            assertThat(STORAGE_DEPENDENCIES).allSatisfy(dependency ->
                    assertThat(source).as(path.toString()).doesNotContain(dependency));
        });
    }

    private List<Path> layerSources(String layer) {
        Path mainSources = Files.isDirectory(MODULE_MAIN_SOURCES) ? MODULE_MAIN_SOURCES : REPOSITORY_MAIN_SOURCES;
        assertThat(mainSources).isDirectory();
        List<Path> sources = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(mainSources.resolve(layer))) {
            paths.filter(path -> path.toString().endsWith(".java")).forEach(sources::add);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
        return sources;
    }

    private String read(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }
}
