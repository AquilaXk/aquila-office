package com.aquilaoffice.adapter.in.web.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aquilaoffice.adapter.in.web.workspace.WorkspaceController;
import com.aquilaoffice.application.port.in.GetMemberProfileQuery;
import com.aquilaoffice.application.port.in.GetMemberProfileUseCase;
import com.aquilaoffice.application.port.in.MemberAvatarColorsResult;
import com.aquilaoffice.application.port.in.MemberProfileResult;
import com.aquilaoffice.application.port.in.UpdateMemberProfileCommand;
import com.aquilaoffice.application.port.in.UpdateMemberProfileUseCase;
import com.aquilaoffice.global.handler.GlobalExceptionHandler;
import com.aquilaoffice.global.security.AuthenticatedMember;
import com.aquilaoffice.domain.member.MemberRole;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 멤버 프로필 web adapter가 HTTP 요청을 query/command로 변환하고 공통 응답 계약을 지키는지 검증한다.
 */
@WebMvcTest(MemberController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class MemberControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetMemberProfileUseCase getMemberProfileUseCase;

    @MockitoBean
    private UpdateMemberProfileUseCase updateMemberProfileUseCase;

    @Test
    void getMyProfileReturnsOkEnvelope() throws Exception {
        given(getMemberProfileUseCase.getMemberProfile(any(GetMemberProfileQuery.class)))
                .willReturn(profileResult());

        mockMvc.perform(get("/api/v1/members/me/profile")
                        .principal(authenticatedPrincipal("google-1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("OK"))
                .andExpect(jsonPath("$.data.memberId").value(1))
                .andExpect(jsonPath("$.data.displayName").value("User"))
                .andExpect(jsonPath("$.data.avatarKind").value("mira"))
                .andExpect(jsonPath("$.data.avatarColors.skin").value("#f8d4b0"))
                .andExpect(jsonPath("$.data.avatarColors.hair").value("#1a1a1a"))
                .andExpect(jsonPath("$.data.avatarColors.shirt").value("#2a3a4a"));

        ArgumentCaptor<GetMemberProfileQuery> queryCaptor = ArgumentCaptor.forClass(GetMemberProfileQuery.class);
        verify(getMemberProfileUseCase).getMemberProfile(queryCaptor.capture());
        assertThat(queryCaptor.getValue().memberGoogleSub()).isEqualTo("google-1");
    }

    @Test
    void updateMyProfileReturnsOkEnvelope() throws Exception {
        given(updateMemberProfileUseCase.updateMemberProfile(any(UpdateMemberProfileCommand.class)))
                .willReturn(updatedProfileResult());

        mockMvc.perform(patch("/api/v1/members/me/profile")
                        .header(WorkspaceController.CREATOR_GOOGLE_SUB_HEADER, " google-1 ")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "displayName": " Operator ",
                                  "avatarKind": " servo-skull ",
                                  "avatarColors": {
                                    "skin": "#111111",
                                    "shirt": "#333333"
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.memberId").value(1))
                .andExpect(jsonPath("$.data.displayName").value("Operator"))
                .andExpect(jsonPath("$.data.avatarKind").value("servo-skull"))
                .andExpect(jsonPath("$.data.avatarColors.skin").value("#111111"))
                .andExpect(jsonPath("$.data.avatarColors.hair").value("#1a1a1a"))
                .andExpect(jsonPath("$.data.avatarColors.shirt").value("#333333"));

        ArgumentCaptor<UpdateMemberProfileCommand> commandCaptor =
                ArgumentCaptor.forClass(UpdateMemberProfileCommand.class);
        verify(updateMemberProfileUseCase).updateMemberProfile(commandCaptor.capture());
        UpdateMemberProfileCommand command = commandCaptor.getValue();
        assertThat(command.memberGoogleSub()).isEqualTo("google-1");
        assertThat(command.displayName()).isEqualTo("Operator");
        assertThat(command.avatarKind()).isEqualTo("servo-skull");
        assertThat(command.skinColor()).isEqualTo("#111111");
        assertThat(command.hairColor()).isNull();
        assertThat(command.shirtColor()).isEqualTo("#333333");
    }

    @Test
    void missingPrincipalReturnsUnauthorizedAndDoesNotCallUseCases() throws Exception {
        mockMvc.perform(get("/api/v1/members/me/profile"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("401"));

        verifyNoInteractions(getMemberProfileUseCase, updateMemberProfileUseCase);
    }

    @Test
    void invalidAvatarColorReturnsBadRequestAndDoesNotCallUseCase() throws Exception {
        mockMvc.perform(patch("/api/v1/members/me/profile")
                        .header(WorkspaceController.CREATOR_GOOGLE_SUB_HEADER, "google-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "avatarColors": {
                                    "skin": "red"
                                  }
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("400"));

        verifyNoInteractions(updateMemberProfileUseCase);
    }

    @Test
    void useCaseIllegalArgumentReturnsNotFoundEnvelope() throws Exception {
        given(getMemberProfileUseCase.getMemberProfile(any(GetMemberProfileQuery.class)))
                .willThrow(new IllegalArgumentException("member not found"));

        mockMvc.perform(get("/api/v1/members/me/profile")
                        .principal(authenticatedPrincipal("missing")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("404"));
    }

    private static MemberProfileResult profileResult() {
        return new MemberProfileResult(
                1L,
                "User",
                "mira",
                new MemberAvatarColorsResult("#f8d4b0", "#1a1a1a", "#2a3a4a"));
    }

    private static MemberProfileResult updatedProfileResult() {
        return new MemberProfileResult(
                1L,
                "Operator",
                "servo-skull",
                new MemberAvatarColorsResult("#111111", "#1a1a1a", "#333333"));
    }

    private static UsernamePasswordAuthenticationToken authenticatedPrincipal(String googleSub) {
        AuthenticatedMember member =
                new AuthenticatedMember(1L, googleSub, googleSub + "@example.com", MemberRole.USER);
        return new UsernamePasswordAuthenticationToken(member, null, List.of());
    }
}
