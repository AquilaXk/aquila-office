package com.aquilaoffice.domain.agent;

import java.util.List;

/**
 * Agent 역할별 기본 bootstrap skill 파일을 제공한다.
 */
public class WorkspaceAgentTemplateCatalog {

    private static final WorkspaceAgentTemplate SHARED_AGENTS_MD = new WorkspaceAgentTemplate(
            "AGENTS.md",
            """
            # aquila office Agent

            ## 작업 범위
            - 지정된 workspace와 task 범위 안에서만 작업한다.
            - 민감정보, token, key 파일 내용은 출력하거나 commit하지 않는다.
            - 변경 전후 검증 명령을 기록하고 실패 원인을 먼저 확인한다.

            ## 검증
            - 코드 변경은 관련 자동화 테스트를 먼저 실행한다.
            - 실패한 검증을 숨기지 않고 결과와 다음 조치를 보고한다.
            """);

    public List<WorkspaceAgentTemplate> defaultsFor(WorkspaceAgentCategory category) {
        WorkspaceAgentCategory normalizedCategory =
                category == null ? WorkspaceAgentCategory.CUSTOM : category;
        return switch (normalizedCategory) {
            case ORCHESTRATOR -> List.of(SHARED_AGENTS_MD, roleTemplate(
                    "orchestrator-guide.md",
                    "계획 수립, 작업 분배, 결과 검증을 조율하는 역할입니다."));
            case BACKEND -> List.of(SHARED_AGENTS_MD, roleTemplate(
                    "backend-guide.md",
                    "Java 21, Spring Boot, JPA, 보안 흐름을 구현하고 검증하는 역할입니다."));
            case FRONTEND -> List.of(SHARED_AGENTS_MD, roleTemplate(
                    "frontend-guide.md",
                    "화면 구조, API client, UI 상태를 일관되게 구현하고 검증하는 역할입니다."));
            case QA -> List.of(SHARED_AGENTS_MD, roleTemplate(
                    "qa-guide.md",
                    "통합 시나리오, 회귀 테스트, 실패 재현을 설계하고 검증하는 역할입니다."));
            case CUSTOM -> List.of(SHARED_AGENTS_MD);
        };
    }

    private WorkspaceAgentTemplate roleTemplate(String fileName, String roleDescription) {
        return new WorkspaceAgentTemplate(
                fileName,
                """
                # 역할 지침

                ## 역할
                %s

                ## 검증
                - 작업 범위에 맞는 가장 작은 테스트부터 실행한다.
                - 실패가 나오면 원인을 확인한 뒤 수정한다.
                """.formatted(roleDescription));
    }
}
