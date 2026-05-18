package com.aquilaoffice.domain.agent;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * 역할별 Agent bootstrap 템플릿이 기본 AGENTS.md와 역할 skill 파일을 제공하는지 검증한다.
 */
class WorkspaceAgentTemplateCatalogTest {

    @Test
    void providesAgentsMdAndRoleSkillFilesForCoreCategories() {
        WorkspaceAgentTemplateCatalog catalog = new WorkspaceAgentTemplateCatalog();

        assertRoleTemplates(catalog.defaultsFor(WorkspaceAgentCategory.ORCHESTRATOR), "orchestrator-guide.md");
        assertRoleTemplates(catalog.defaultsFor(WorkspaceAgentCategory.BACKEND), "backend-guide.md");
        assertRoleTemplates(catalog.defaultsFor(WorkspaceAgentCategory.FRONTEND), "frontend-guide.md");
        assertRoleTemplates(catalog.defaultsFor(WorkspaceAgentCategory.QA), "qa-guide.md");
    }

    @Test
    void customCategoryProvidesOnlySharedAgentsMd() {
        WorkspaceAgentTemplateCatalog catalog = new WorkspaceAgentTemplateCatalog();

        List<WorkspaceAgentTemplate> templates = catalog.defaultsFor(WorkspaceAgentCategory.CUSTOM);

        assertThat(templates)
                .extracting(WorkspaceAgentTemplate::fileName)
                .containsExactly("AGENTS.md");
        assertThat(templates.getFirst().content())
                .contains("aquila office")
                .contains("작업 범위")
                .contains("검증");
    }

    private void assertRoleTemplates(List<WorkspaceAgentTemplate> templates, String roleFileName) {
        assertThat(templates)
                .extracting(WorkspaceAgentTemplate::fileName)
                .containsExactly("AGENTS.md", roleFileName);
        assertThat(templates.getFirst().content()).contains("aquila office");
        assertThat(templates.get(1).content()).contains("역할", "검증");
    }
}
