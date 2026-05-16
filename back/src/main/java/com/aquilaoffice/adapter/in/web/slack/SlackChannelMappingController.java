package com.aquilaoffice.adapter.in.web.slack;

import com.aquilaoffice.adapter.in.web.workspace.WorkspaceController;
import com.aquilaoffice.application.port.in.CreateSlackChannelMappingResult;
import com.aquilaoffice.application.port.in.CreateSlackChannelMappingUseCase;
import com.aquilaoffice.application.port.in.ListSlackChannelMappingsQuery;
import com.aquilaoffice.application.port.in.ListSlackChannelMappingsUseCase;
import com.aquilaoffice.global.exception.CommonErrorCode;
import com.aquilaoffice.global.exception.ServiceException;
import com.aquilaoffice.global.response.RsData;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Slack channel mapping HTTP 요청을 application use case로 연결하는 inbound web adapter다.
 */
@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/slack/channel-mappings")
public class SlackChannelMappingController {

    private final CreateSlackChannelMappingUseCase createSlackChannelMappingUseCase;
    private final ListSlackChannelMappingsUseCase listSlackChannelMappingsUseCase;

    public SlackChannelMappingController(
            CreateSlackChannelMappingUseCase createSlackChannelMappingUseCase,
            ListSlackChannelMappingsUseCase listSlackChannelMappingsUseCase) {
        this.createSlackChannelMappingUseCase = createSlackChannelMappingUseCase;
        this.listSlackChannelMappingsUseCase = listSlackChannelMappingsUseCase;
    }

    @PostMapping
    public ResponseEntity<RsData<SlackChannelMappingResponse>> createSlackChannelMapping(
            @PathVariable Long workspaceId,
            @RequestHeader(name = WorkspaceController.CREATOR_GOOGLE_SUB_HEADER, required = false)
                    String requesterGoogleSub,
            @Valid @RequestBody CreateSlackChannelMappingRequest request) {
        String normalizedRequesterGoogleSub = requireRequesterGoogleSub(requesterGoogleSub);

        try {
            CreateSlackChannelMappingResult result = createSlackChannelMappingUseCase.createSlackChannelMapping(
                    request.toCommand(workspaceId, normalizedRequesterGoogleSub));
            return ResponseEntity.created(URI.create(
                            "/api/v1/workspaces/" + result.workspaceId()
                                    + "/slack/channel-mappings/" + result.mappingId()))
                    .body(RsData.ok(SlackChannelMappingResponse.from(result)));
        } catch (IllegalStateException exception) {
            throw new ServiceException(CommonErrorCode.BAD_REQUEST);
        } catch (IllegalArgumentException exception) {
            // mapping API는 워크스페이스 멤버에게만 노출하므로 대상 존재와 권한 실패를 같은 404로 숨긴다.
            throw new ServiceException(CommonErrorCode.NOT_FOUND);
        }
    }

    @GetMapping
    public ResponseEntity<RsData<List<SlackChannelMappingResponse>>> listSlackChannelMappings(
            @PathVariable Long workspaceId,
            @RequestHeader(name = WorkspaceController.CREATOR_GOOGLE_SUB_HEADER, required = false)
                    String requesterGoogleSub) {
        String normalizedRequesterGoogleSub = requireRequesterGoogleSub(requesterGoogleSub);

        try {
            return ResponseEntity.ok(RsData.ok(listSlackChannelMappingsUseCase
                    .listSlackChannelMappings(new ListSlackChannelMappingsQuery(
                            workspaceId,
                            normalizedRequesterGoogleSub))
                    .stream()
                    .map(SlackChannelMappingResponse::from)
                    .toList()));
        } catch (IllegalArgumentException exception) {
            // mapping 목록도 워크스페이스 멤버에게만 노출하므로 대상 존재와 권한 실패를 같은 404로 숨긴다.
            throw new ServiceException(CommonErrorCode.NOT_FOUND);
        }
    }

    private String requireRequesterGoogleSub(String requesterGoogleSub) {
        if (requesterGoogleSub == null || requesterGoogleSub.isBlank()) {
            throw new ServiceException(CommonErrorCode.BAD_REQUEST);
        }
        return requesterGoogleSub.trim();
    }
}
