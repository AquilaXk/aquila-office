package com.aquilaoffice.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class DatabasePublicApiExposurePolicyTest {

    private static final List<String> FORBIDDEN_FRONTEND_DB_TOKENS = List.of(
            "service_role",
            "service-role",
            "service role",
            "database_url",
            "spring_datasource",
            "postgres_password",
            "postgres_user",
            "postgres_db");

    @Test
    void readmeDocumentsServerOnlyDatabaseAccessAndRlsGate() throws IOException {
        String readme = Files.readString(repositoryRoot().resolve("README.md"));

        assertThat(readme)
                .contains("### DB 접근 정책")
                .contains("Frontend는 backend API만 호출하고 DB credential을 보관하지 않습니다.")
                .contains("공개 DB API를 추가할 때는 노출 대상 테이블의 RLS 정책과 anon 권한을 먼저 정의합니다.");
    }

    @Test
    void frontendDoesNotExposeDatabaseCredentials() throws IOException {
        Path root = repositoryRoot();
        List<String> violations;
        try (Stream<Path> files = Files.walk(root.resolve("front"))) {
            violations = files
                    .filter(Files::isRegularFile)
                    .filter(DatabasePublicApiExposurePolicyTest::isTextFile)
                    .filter(path -> !path.toString().contains("/node_modules/"))
                    .filter(path -> !path.toString().contains("/.next/"))
                    .filter(path -> containsForbiddenToken(path))
                    .map(root::relativize)
                    .map(Path::toString)
                    .toList();
        }

        assertThat(violations).isEmpty();
    }

    private static boolean isTextFile(Path path) {
        String fileName = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return !fileName.endsWith(".png")
                && !fileName.endsWith(".jpg")
                && !fileName.endsWith(".jpeg")
                && !fileName.endsWith(".webp")
                && !fileName.endsWith(".ico");
    }

    private static boolean containsForbiddenToken(Path path) {
        try {
            String source = Files.readString(path).toLowerCase(Locale.ROOT);
            return FORBIDDEN_FRONTEND_DB_TOKENS.stream().anyMatch(source::contains);
        } catch (IOException exception) {
            throw new IllegalStateException("frontend DB exposure scan failed: " + path, exception);
        }
    }

    private static Path repositoryRoot() {
        return Path.of(System.getProperty("user.dir")).getParent();
    }
}
