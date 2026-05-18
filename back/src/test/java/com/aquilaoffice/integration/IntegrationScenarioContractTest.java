package com.aquilaoffice.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class IntegrationScenarioContractTest {

    @Test
    void scenarioCoversLoginWorkspaceAgentTaskAndReportRoutes() {
        Object catalog = loadCatalog();
        List<?> steps = invokeList(catalog, "steps");

        assertThat(steps)
                .map(step -> List.of(
                        invokeString(step, "id"),
                        invokeString(step, "method"),
                        invokeString(step, "path")))
                .containsExactly(
                        List.of("login", "POST", "/api/v1/auth/google/login"),
                        List.of("workspace", "POST", "/api/v1/workspaces"),
                        List.of("agent", "POST", "/api/v1/workspaces/{workspaceId}/agents"),
                        List.of("task", "POST", "/api/v1/workspaces/{workspaceId}/tasks/run"),
                        List.of("report", "GET", "/api/v1/workspaces/{workspaceId}/tasks/{taskId}/reports"));
    }

    @Test
    void scenarioRouteCatalogMatchesControllerSourceAnnotations() throws Exception {
        Path repositoryRoot = repositoryRoot();
        Object catalog = loadCatalog();

        for (Object step : invokeList(catalog, "steps")) {
            Path sourcePath = repositoryRoot.resolve(invokeString(step, "controllerSourcePath"));
            assertThat(sourcePath).exists();

            String source = Files.readString(sourcePath);
            for (String fragment : invokeStringList(step, "sourceRouteFragments")) {
                assertThat(source)
                        .as("%s should contain %s", sourcePath.getFileName(), fragment)
                        .contains(fragment);
            }
        }
    }

    private Object loadCatalog() {
        try {
            Class<?> catalogType = Class.forName("com.aquilaoffice.integration.IntegrationScenarioRouteCatalog");
            return catalogType.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException ex) {
            fail("integration scenario route catalog is not implemented", ex);
            return null;
        }
    }

    private List<?> invokeList(Object target, String methodName) {
        return castList(invoke(target, methodName));
    }

    private List<String> invokeStringList(Object target, String methodName) {
        return castStringList(invoke(target, methodName));
    }

    private String invokeString(Object target, String methodName) {
        return (String) invoke(target, methodName);
    }

    private Object invoke(Object target, String methodName) {
        try {
            Method method = target.getClass().getMethod(methodName);
            return method.invoke(target);
        } catch (NoSuchMethodException | IllegalAccessException ex) {
            throw new AssertionError("scenario contract method is not accessible: " + methodName, ex);
        } catch (InvocationTargetException ex) {
            throw new AssertionError("scenario contract method failed: " + methodName, ex.getCause());
        }
    }

    private List<?> castList(Object value) {
        assertThat(value).isInstanceOf(List.class);
        return (List<?>) value;
    }

    @SuppressWarnings("unchecked")
    private List<String> castStringList(Object value) {
        assertThat(value).isInstanceOf(List.class);
        return (List<String>) value;
    }

    private Path repositoryRoot() {
        Path workingDirectory = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        if (Files.exists(workingDirectory.resolve("back/src/main/java"))) {
            return workingDirectory;
        }
        if (Files.exists(workingDirectory.resolve("src/main/java"))) {
            return workingDirectory.getParent();
        }
        return workingDirectory;
    }
}
