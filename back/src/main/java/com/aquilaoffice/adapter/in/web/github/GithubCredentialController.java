package com.aquilaoffice.adapter.in.web.github;

import com.aquilaoffice.application.port.in.CreateGithubCredentialResult;
import com.aquilaoffice.application.port.in.CreateGithubCredentialUseCase;
import com.aquilaoffice.application.port.in.DeleteGithubCredentialCommand;
import com.aquilaoffice.application.port.in.DeleteGithubCredentialUseCase;
import com.aquilaoffice.application.port.in.ListGithubCredentialsQuery;
import com.aquilaoffice.application.port.in.ListGithubCredentialsResult;
import com.aquilaoffice.application.port.in.ListGithubCredentialsUseCase;
import com.aquilaoffice.application.port.in.UpdateGithubCredentialResult;
import com.aquilaoffice.application.port.in.UpdateGithubCredentialUseCase;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 워크스페이스별 GitHub 자격 증명 HTTP 요청을 application use case로 연결하는 inbound web adapter다.
 */
@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/github/credentials")
public class GithubCredentialController {

    private final CreateGithubCredentialUseCase createGithubCredentialUseCase;
    private final ListGithubCredentialsUseCase listGithubCredentialsUseCase;
    private final UpdateGithubCredentialUseCase updateGithubCredentialUseCase;
    private final DeleteGithubCredentialUseCase deleteGithubCredentialUseCase;

    public GithubCredentialController(
            CreateGithubCredentialUseCase createGithubCredentialUseCase,
            ListGithubCredentialsUseCase listGithubCredentialsUseCase,
            UpdateGithubCredentialUseCase updateGithubCredentialUseCase,
            DeleteGithubCredentialUseCase deleteGithubCredentialUseCase) {
        this.createGithubCredentialUseCase = createGithubCredentialUseCase;
        this.listGithubCredentialsUseCase = listGithubCredentialsUseCase;
        this.updateGithubCredentialUseCase = updateGithubCredentialUseCase;
        this.deleteGithubCredentialUseCase = deleteGithubCredentialUseCase;
    }

    @GetMapping
    public ResponseEntity<RsData<List<GithubCredentialResponse>>> listGithubCredentials(
            @PathVariable Long workspaceId,
            Authentication authentication) {
        String normalizedRequesterGoogleSub = requireAuthenticatedRequesterGoogleSub(authentication);

        try {
            List<ListGithubCredentialsResult> results = listGithubCredentialsUseCase.listGithubCredentials(
                    new ListGithubCredentialsQuery(workspaceId, normalizedRequesterGoogleSub));
            return ResponseEntity.ok(RsData.ok(results.stream()
                    .map(GithubCredentialResponse::from)
                    .toList()));
        } catch (IllegalArgumentException exception) {
            // 자격 증명 목록은 워크스페이스 멤버에게만 노출하므로 대상 존재와 권한 실패를 같은 404로 숨긴다.
            throw new ServiceException(CommonErrorCode.NOT_FOUND);
        }
    }

    @PostMapping
    public ResponseEntity<RsData<GithubCredentialResponse>> createGithubCredential(
            @PathVariable Long workspaceId,
            Authentication authentication,
            @Valid @RequestBody CreateGithubCredentialRequest request) {
        String normalizedRequesterGoogleSub = requireAuthenticatedRequesterGoogleSub(authentication);

        try {
            CreateGithubCredentialResult result = createGithubCredentialUseCase.createGithubCredential(
                    request.toCommand(workspaceId, normalizedRequesterGoogleSub));
            return ResponseEntity
                    .created(URI.create("/api/v1/workspaces/"
                            + result.workspaceId()
                            + "/github/credentials/"
                            + result.credentialId()))
                    .body(RsData.ok(GithubCredentialResponse.from(result)));
        } catch (IllegalStateException exception) {
            throw new ServiceException(CommonErrorCode.BAD_REQUEST);
        } catch (IllegalArgumentException exception) {
            // 등록 권한이 없거나 대상 워크스페이스를 찾을 수 없는 경우 같은 404로 응답한다.
            throw new ServiceException(CommonErrorCode.NOT_FOUND);
        }
    }

    @PatchMapping("/{credentialId}")
    public ResponseEntity<RsData<GithubCredentialResponse>> updateGithubCredential(
            @PathVariable Long workspaceId,
            @PathVariable Long credentialId,
            Authentication authentication,
            @Valid @RequestBody UpdateGithubCredentialRequest request) {
        String normalizedRequesterGoogleSub = requireAuthenticatedRequesterGoogleSub(authentication);

        try {
            UpdateGithubCredentialResult result = updateGithubCredentialUseCase.updateGithubCredential(
                    request.toCommand(workspaceId, credentialId, normalizedRequesterGoogleSub));
            return ResponseEntity.ok(RsData.ok(GithubCredentialResponse.from(result)));
        } catch (IllegalArgumentException exception) {
            // 수정 권한이 없거나 대상 자격 증명을 찾을 수 없는 경우 같은 404로 응답한다.
            throw new ServiceException(CommonErrorCode.NOT_FOUND);
        }
    }

    @DeleteMapping("/{credentialId}")
    public ResponseEntity<RsData<Void>> deleteGithubCredential(
            @PathVariable Long workspaceId,
            @PathVariable Long credentialId,
            Authentication authentication) {
        String normalizedRequesterGoogleSub = requireAuthenticatedRequesterGoogleSub(authentication);

        try {
            deleteGithubCredentialUseCase.deleteGithubCredential(
                    new DeleteGithubCredentialCommand(workspaceId, credentialId, normalizedRequesterGoogleSub));
            return ResponseEntity.ok(RsData.ok(null));
        } catch (IllegalArgumentException exception) {
            // 삭제 권한이 없거나 대상 자격 증명을 찾을 수 없는 경우 같은 404로 응답한다.
            throw new ServiceException(CommonErrorCode.NOT_FOUND);
        }
    }

    private String requireAuthenticatedRequesterGoogleSub(Authentication authentication) {
        if (authentication == null
                || !(authentication.getPrincipal() instanceof AuthenticatedMember authenticatedMember)) {
            throw new ServiceException(CommonErrorCode.UNAUTHORIZED);
        }
        return authenticatedMember.googleSub();
    }
}
