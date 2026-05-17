package com.aquilaoffice.adapter.in.web.slack;

import com.aquilaoffice.adapter.in.web.workspace.WorkspaceController;
import com.aquilaoffice.global.exception.CommonErrorCode;
import com.aquilaoffice.global.exception.ServiceException;
import com.aquilaoffice.global.response.RsData;
import com.aquilaoffice.global.security.AuthenticatedMember;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Slack 설치 URL 요청을 처리하는 inbound web adapter다.
 */
@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/slack/install")
public class SlackInstallController {

    private final SlackInstallUrlBuilder slackInstallUrlBuilder;

    public SlackInstallController(SlackInstallUrlBuilder slackInstallUrlBuilder) {
        this.slackInstallUrlBuilder = slackInstallUrlBuilder;
    }

    @GetMapping
    public ResponseEntity<RsData<SlackInstallUrlResponse>> getSlackInstallUrl(
            @PathVariable Long workspaceId,
            Authentication authentication) {
        String normalizedRequesterGoogleSub = requireAuthenticatedRequesterGoogleSub(authentication);
        String installUrl = slackInstallUrlBuilder.build(workspaceId, normalizedRequesterGoogleSub);
        return ResponseEntity.ok(RsData.ok(new SlackInstallUrlResponse(installUrl)));
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
