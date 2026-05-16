package com.aquilaoffice.adapter.in.web.member;

import com.aquilaoffice.adapter.in.web.workspace.WorkspaceController;
import com.aquilaoffice.application.port.in.GetMemberProfileQuery;
import com.aquilaoffice.application.port.in.GetMemberProfileUseCase;
import com.aquilaoffice.application.port.in.MemberProfileResult;
import com.aquilaoffice.application.port.in.UpdateMemberProfileUseCase;
import com.aquilaoffice.global.exception.CommonErrorCode;
import com.aquilaoffice.global.exception.ServiceException;
import com.aquilaoffice.global.response.RsData;
import com.aquilaoffice.global.security.AuthenticatedMember;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 멤버 프로필 관련 HTTP 요청을 application use case로 연결하는 inbound web adapter다.
 */
@RestController
@RequestMapping("/api/v1/members")
public class MemberController {

    private final GetMemberProfileUseCase getMemberProfileUseCase;
    private final UpdateMemberProfileUseCase updateMemberProfileUseCase;

    public MemberController(
            GetMemberProfileUseCase getMemberProfileUseCase,
            UpdateMemberProfileUseCase updateMemberProfileUseCase) {
        this.getMemberProfileUseCase = getMemberProfileUseCase;
        this.updateMemberProfileUseCase = updateMemberProfileUseCase;
    }

    @GetMapping("/me/profile")
    public ResponseEntity<RsData<MemberProfileResponse>> getMyProfile(
            Authentication authentication) {
        String normalizedMemberGoogleSub = requireAuthenticatedMemberGoogleSub(authentication);

        try {
            MemberProfileResult result =
                    getMemberProfileUseCase.getMemberProfile(new GetMemberProfileQuery(normalizedMemberGoogleSub));
            return ResponseEntity.ok(RsData.ok(MemberProfileResponse.from(result)));
        } catch (IllegalArgumentException exception) {
            // 회원 존재 여부를 직접 노출하지 않기 위해 없는 회원은 동일한 404로 응답한다.
            throw new ServiceException(CommonErrorCode.NOT_FOUND);
        }
    }

    @PatchMapping("/me/profile")
    public ResponseEntity<RsData<MemberProfileResponse>> updateMyProfile(
            @RequestHeader(name = WorkspaceController.CREATOR_GOOGLE_SUB_HEADER, required = false)
                    String memberGoogleSub,
            @RequestBody(required = false) MemberProfileUpdateRequest request) {
        String normalizedMemberGoogleSub = requireMemberGoogleSub(memberGoogleSub);
        if (request == null) {
            throw new ServiceException(CommonErrorCode.BAD_REQUEST);
        }

        try {
            MemberProfileResult result = updateMemberProfileUseCase.updateMemberProfile(
                    request.toCommand(normalizedMemberGoogleSub));
            return ResponseEntity.ok(RsData.ok(MemberProfileResponse.from(result)));
        } catch (IllegalStateException exception) {
            throw new ServiceException(CommonErrorCode.BAD_REQUEST);
        } catch (IllegalArgumentException exception) {
            // 없는 회원과 저장소 조회 실패는 프로필 정보를 노출하지 않고 같은 404로 응답한다.
            throw new ServiceException(CommonErrorCode.NOT_FOUND);
        }
    }

    private String requireMemberGoogleSub(String memberGoogleSub) {
        if (memberGoogleSub == null || memberGoogleSub.isBlank()) {
            throw new ServiceException(CommonErrorCode.BAD_REQUEST);
        }
        return memberGoogleSub.trim();
    }

    private String requireAuthenticatedMemberGoogleSub(Authentication authentication) {
        if (authentication == null
                || !(authentication.getPrincipal() instanceof AuthenticatedMember authenticatedMember)) {
            throw new ServiceException(CommonErrorCode.UNAUTHORIZED);
        }
        return authenticatedMember.googleSub();
    }
}
