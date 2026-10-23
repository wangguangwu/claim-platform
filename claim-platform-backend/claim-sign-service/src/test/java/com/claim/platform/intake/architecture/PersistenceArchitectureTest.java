package com.claim.platform.intake.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * 验证持久化层只通过 MyBatis-Plus 访问数据库：
 * Mapper 必须继承 {@code BaseMapper}，不得声明 SQL 注解，主代码不得内嵌 SQL 语句或 Mapper XML。
 *
 * @author wangguangwu
 */
class PersistenceArchitectureTest {

    private static final Path MODULE_MAIN_SOURCES = Path.of("src/main/java/com/claim/platform/intake");
    private static final Path REPOSITORY_MAIN_SOURCES =
            Path.of("backend/claim-intake-service/src/main/java/com/claim/platform/intake");
    private static final String MAPPER_DIRECTORY = "infrastructure/persistence/mapper";
    private static final List<String> SQL_ANNOTATIONS = List.of(
            "@Select", "@Insert", "@Update", "@Delete",
            "@SelectProvider", "@InsertProvider", "@UpdateProvider", "@DeleteProvider");
    private static final Pattern INLINE_SQL_STATEMENT = Pattern.compile(
            "(?i)\"[^\"]*\\b(select\\s+.+?\\s+from|insert\\s+into|update\\s+\\w+\\s+set|delete\\s+from)\\b[^\"]*\"");
    private static final Pattern MAPPER_XML = Pattern.compile("<mapper\\b");

    @Test
    void mappersShouldExtendBaseMapper() {
        List<Path> mappers = mapperSources();

        assertThat(mappers).isNotEmpty();
        assertThat(mappers).allSatisfy(path ->
                assertThat(read(path)).as(path.toString()).contains("extends BaseMapper<"));
    }

    @Test
    void mappersShouldNotDeclareSqlAnnotations() {
        assertThat(mapperSources()).allSatisfy(path -> {
            String source = read(path);
            assertThat(SQL_ANNOTATIONS).allSatisfy(annotation ->
                    assertThat(source).as(path.toString()).doesNotContain(annotation));
        });
    }

    @Test
    void mainSourcesShouldNotContainInlineSqlStatements() {
        List<Path> sources = javaSources(mainSources());

        assertThat(sources).isNotEmpty();
        assertThat(sources).allSatisfy(path ->
                assertThat(INLINE_SQL_STATEMENT.matcher(read(path)).find())
                        .as(path.toString())
                        .isFalse());
    }

    @Test
    void mapperXmlShouldNotExist() {
        Path resources = mainSources().getParent().getParent().resolve("resources");
        if (!Files.isDirectory(resources)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(resources)) {
            List<Path> mapperXml = paths
                    .filter(path -> path.toString().endsWith(".xml"))
                    .filter(path -> MAPPER_XML.matcher(read(path)).find())
                    .toList();
            assertThat(mapperXml).isEmpty();
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private List<Path> mapperSources() {
        Path mapperDirectory = mainSources().resolve(MAPPER_DIRECTORY);
        assertThat(mapperDirectory).isDirectory();
        return javaSources(mapperDirectory);
    }

    private Path mainSources() {
        return Files.isDirectory(MODULE_MAIN_SOURCES) ? MODULE_MAIN_SOURCES : REPOSITORY_MAIN_SOURCES;
    }

    private List<Path> javaSources(Path root) {
        assertThat(root).isDirectory();
        List<Path> sources = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(root)) {
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
