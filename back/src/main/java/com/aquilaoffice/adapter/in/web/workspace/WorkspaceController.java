package com.aquilaoffice.adapter.in.web.workspace;

import com.aquilaoffice.application.port.in.CreateWorkspaceResult;
import com.aquilaoffice.application.port.in.CreateWorkspaceUseCase;
import com.aquilaoffice.global.exception.CommonErrorCode;
import com.aquilaoffice.global.exception.ServiceException;
import com.aquilaoffice.global.response.RsData;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 워크스페이스 관련 HTTP 요청을 application use case로 연결하는 inbound web adapter다.
 */
@RestController
@RequestMapping("/api/v1/workspaces")
public class WorkspaceController {

    // 실제 인증 어댑터가 연결되기 전까지 웹 요청에서 생성자를 식별하는 임시 경계값이다.
    public static final String CREATOR_GOOGLE_SUB_HEADER = "X-Aquila-Creator-Google-Sub";

    private final CreateWorkspaceUseCase createWorkspaceUseCase;

    public WorkspaceController(CreateWorkspaceUseCase createWorkspaceUseCase) {
        this.createWorkspaceUseCase = createWorkspaceUseCase;
    }

    @PostMapping
    public ResponseEntity<RsData<WorkspaceResponse>> createWorkspace(
            @RequestHeader(name = CREATOR_GOOGLE_SUB_HEADER, required = false) String creatorGoogleSub,
            @Valid @RequestBody CreateWorkspaceRequest request) {
        String normalizedCreatorGoogleSub = requireCreatorGoogleSub(creatorGoogleSub);

        // inbound adapter는 HTTP 입력을 application command로 바꾸고, 실제 생성 규칙은 use case에 위임한다.
        CreateWorkspaceResult result = createWorkspaceUseCase.createWorkspace(
                request.toCommand(normalizedCreatorGoogleSub));

        return ResponseEntity
                .created(URI.create("/api/v1/workspaces/" + result.workspaceId()))
                .body(RsData.ok(WorkspaceResponse.from(result)));
    }

    private String requireCreatorGoogleSub(String creatorGoogleSub) {
        if (creatorGoogleSub == null || creatorGoogleSub.isBlank()) {
            throw new ServiceException(CommonErrorCode.BAD_REQUEST);
        }
        return creatorGoogleSub.trim();
    }
}
