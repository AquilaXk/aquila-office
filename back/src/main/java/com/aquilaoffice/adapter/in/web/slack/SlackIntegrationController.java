package com.aquilaoffice.adapter.in.web.slack;

import com.aquilaoffice.adapter.in.web.workspace.WorkspaceController;
import com.aquilaoffice.application.port.in.CreateSlackChannelMappingResult;
import com.aquilaoffice.application.port.in.CreateSlackChannelMappingUseCase;
import com.aquilaoffice.application.port.in.DeleteSlackIntegrationCommand;
import com.aquilaoffice.application.port.in.DeleteSlackIntegrationResult;
import com.aquilaoffice.application.port.in.DeleteSlackIntegrationUseCase;
import com.aquilaoffice.application.port.in.ListSlackChannelMappingsQuery;
import com.aquilaoffice.application.port.in.ListSlackChannelMappingsUseCase;
import com.aquilaoffice.application.port.in.UpdateSlackIntegrationResult;
import com.aquilaoffice.application.port.in.UpdateSlackIntegrationUseCase;
import com.aquilaoffice.global.exception.CommonErrorCode;
import com.aquilaoffice.global.exception.ServiceException;
import com.aquilaoffice.global.response.RsData;
import com.aquilaoffice.global.security.AuthenticatedMember;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Slack 수동 연동 HTTP 요청을 기존 channel mapping 유스케이스로 연결하는 adapter다.
 */
@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/slack/integrations")
public class SlackIntegrationController {

    private final CreateSlackChannelMappingUseCase createSlackChannelMappingUseCase;
    private final ListSlackChannelMappingsUseCase listSlackChannelMappingsUseCase;
    private final UpdateSlackIntegrationUseCase updateSlackIntegrationUseCase;
    private final DeleteSlackIntegrationUseCase deleteSlackIntegrationUseCase;

    public SlackIntegrationController(
            CreateSlackChannelMappingUseCase createSlackChannelMappingUseCase,
            ListSlackChannelMappingsUseCase listSlackChannelMappingsUseCase,
            UpdateSlackIntegrationUseCase updateSlackIntegrationUseCase,
            DeleteSlackIntegrationUseCase deleteSlackIntegrationUseCase) {
        this.createSlackChannelMappingUseCase = createSlackChannelMappingUseCase;
        this.listSlackChannelMappingsUseCase = listSlackChannelMappingsUseCase;
        this.updateSlackIntegrationUseCase = updateSlackIntegrationUseCase;
        this.deleteSlackIntegrationUseCase = deleteSlackIntegrationUseCase;
    }

    @PostMapping
    public ResponseEntity<RsData<SlackIntegrationResponse>> createSlackIntegration(
            @PathVariable Long workspaceId,
            Authentication authentication,
            @Valid @RequestBody CreateSlackIntegrationRequest request) {
        String normalizedRequesterGoogleSub = requireAuthenticatedRequesterGoogleSub(authentication);

        try {
            CreateSlackChannelMappingResult result = createSlackChannelMappingUseCase.createSlackChannelMapping(
                    request.toCommand(workspaceId, normalizedRequesterGoogleSub));
            return ResponseEntity.created(URI.create(
                            "/api/v1/workspaces/" + result.workspaceId()
                                    + "/slack/integrations/" + result.mappingId()))
                    .body(RsData.ok(SlackIntegrationResponse.from(result)));
        } catch (IllegalStateException exception) {
            throw new ServiceException(CommonErrorCode.BAD_REQUEST);
        } catch (IllegalArgumentException exception) {
            // 수동 연동 API는 워크스페이스 멤버에게만 노출하므로 대상 존재와 권한 실패를 같은 404로 숨긴다.
            throw new ServiceException(CommonErrorCode.NOT_FOUND);
        }
    }

    @GetMapping
    public ResponseEntity<RsData<List<SlackIntegrationResponse>>> listSlackIntegrations(
            @PathVariable Long workspaceId,
            Authentication authentication) {
        String normalizedRequesterGoogleSub = requireAuthenticatedRequesterGoogleSub(authentication);

        try {
            return ResponseEntity.ok(RsData.ok(listSlackChannelMappingsUseCase
                    .listSlackChannelMappings(new ListSlackChannelMappingsQuery(
                            workspaceId,
                            normalizedRequesterGoogleSub))
                    .stream()
                    .map(SlackIntegrationResponse::from)
                    .toList()));
        } catch (IllegalArgumentException exception) {
            // 수동 연동 목록도 워크스페이스 멤버에게만 노출하므로 대상 존재와 권한 실패를 같은 404로 숨긴다.
            throw new ServiceException(CommonErrorCode.NOT_FOUND);
        }
    }

    @PatchMapping("/{integrationId}")
    public ResponseEntity<RsData<SlackIntegrationResponse>> updateSlackIntegration(
            @PathVariable Long workspaceId,
            @PathVariable Long integrationId,
            Authentication authentication,
            @Valid @RequestBody UpdateSlackIntegrationRequest request) {
        String normalizedRequesterGoogleSub = requireAuthenticatedRequesterGoogleSub(authentication);

        try {
            UpdateSlackIntegrationResult result = updateSlackIntegrationUseCase.updateSlackIntegration(
                    request.toCommand(workspaceId, integrationId, normalizedRequesterGoogleSub));
            return ResponseEntity.ok(RsData.ok(SlackIntegrationResponse.from(result)));
        } catch (IllegalStateException exception) {
            throw new ServiceException(CommonErrorCode.BAD_REQUEST);
        } catch (IllegalArgumentException exception) {
            // 수정 권한이 없거나 대상 연동을 찾을 수 없는 경우 같은 404로 숨긴다.
            throw new ServiceException(CommonErrorCode.NOT_FOUND);
        }
    }

    @DeleteMapping("/{integrationId}")
    public ResponseEntity<RsData<DeleteSlackIntegrationResponse>> deleteSlackIntegration(
            @PathVariable Long workspaceId,
            @PathVariable Long integrationId,
            Authentication authentication) {
        String normalizedRequesterGoogleSub = requireAuthenticatedRequesterGoogleSub(authentication);

        try {
            DeleteSlackIntegrationResult result = deleteSlackIntegrationUseCase.deleteSlackIntegration(
                    new DeleteSlackIntegrationCommand(
                            workspaceId,
                            normalizedRequesterGoogleSub,
                            integrationId));
            return ResponseEntity.ok(RsData.ok(DeleteSlackIntegrationResponse.from(result)));
        } catch (IllegalArgumentException exception) {
            // 삭제 권한이 없거나 대상 연동을 찾을 수 없는 경우 같은 404로 숨긴다.
            throw new ServiceException(CommonErrorCode.NOT_FOUND);
        }
    }

    private String requireRequesterGoogleSub(String requesterGoogleSub) {
        if (requesterGoogleSub == null || requesterGoogleSub.isBlank()) {
            throw new ServiceException(CommonErrorCode.BAD_REQUEST);
        }
        return requesterGoogleSub.trim();
    }

    private String requireAuthenticatedRequesterGoogleSub(Authentication authentication) {
        if (authentication == null
                || !(authentication.getPrincipal() instanceof AuthenticatedMember authenticatedMember)) {
            throw new ServiceException(CommonErrorCode.UNAUTHORIZED);
        }
        return authenticatedMember.googleSub();
    }
}
