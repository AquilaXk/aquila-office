package com.aquilaoffice.application.service;

import com.aquilaoffice.application.port.in.GoogleLoginCommand;
import com.aquilaoffice.application.port.in.GoogleLoginResult;
import com.aquilaoffice.application.port.in.GoogleLoginUseCase;
import com.aquilaoffice.application.port.out.IssueAuthTokenPort;
import com.aquilaoffice.application.port.out.IssuedAuthTokens;
import com.aquilaoffice.application.port.out.SaveGoogleLoginMemberPort;
import com.aquilaoffice.application.port.out.SaveRefreshTokenPort;
import com.aquilaoffice.application.port.out.SavedGoogleLoginMember;
import com.aquilaoffice.application.port.out.VerifiedGoogleUser;
import com.aquilaoffice.application.port.out.VerifyGoogleIdTokenPort;
import com.aquilaoffice.domain.member.MemberRole;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;

/**
 * Google 로그인 검증, 회원 upsert, token 발급을 조율하는 application service다.
 */
public class GoogleLoginService implements GoogleLoginUseCase {

    private final VerifyGoogleIdTokenPort verifyGoogleIdTokenPort;
    private final SaveGoogleLoginMemberPort saveGoogleLoginMemberPort;
    private final IssueAuthTokenPort issueAuthTokenPort;
    private final SaveRefreshTokenPort saveRefreshTokenPort;
    private final String adminAllowlistEmails;

    public GoogleLoginService(
            VerifyGoogleIdTokenPort verifyGoogleIdTokenPort,
            SaveGoogleLoginMemberPort saveGoogleLoginMemberPort,
            IssueAuthTokenPort issueAuthTokenPort,
            SaveRefreshTokenPort saveRefreshTokenPort,
            String adminAllowlistEmails) {
        this.verifyGoogleIdTokenPort =
                Objects.requireNonNull(verifyGoogleIdTokenPort, "verifyGoogleIdTokenPort must not be null");
        this.saveGoogleLoginMemberPort =
                Objects.requireNonNull(saveGoogleLoginMemberPort, "saveGoogleLoginMemberPort must not be null");
        this.issueAuthTokenPort = Objects.requireNonNull(issueAuthTokenPort, "issueAuthTokenPort must not be null");
        this.saveRefreshTokenPort =
                Objects.requireNonNull(saveRefreshTokenPort, "saveRefreshTokenPort must not be null");
        this.adminAllowlistEmails = adminAllowlistEmails == null ? "" : adminAllowlistEmails;
    }

    @Override
    public GoogleLoginResult login(GoogleLoginCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        VerifiedGoogleUser googleUser = verifyGoogleIdTokenPort.verify(command.idToken());
        MemberRole requestedRole = isAdminAllowlistEmail(googleUser.email()) ? MemberRole.ADMIN : MemberRole.USER;
        SavedGoogleLoginMember member = saveGoogleLoginMemberPort.saveOrUpdate(
                googleUser.googleSub(),
                googleUser.email(),
                googleUser.name(),
                requestedRole);
        IssuedAuthTokens tokens = issueAuthTokenPort.issue(member.memberId(), member.email(), member.role());
        saveRefreshTokenPort.saveOrRotate(member.memberId(), tokens.refreshToken());

        return new GoogleLoginResult(
                member.memberId(),
                member.name(),
                member.email(),
                member.role().name(),
                tokens.accessToken(),
                tokens.refreshToken());
    }

    private boolean isAdminAllowlistEmail(String email) {
        if (adminAllowlistEmails.isBlank()) {
            return false;
        }
        String normalizedEmail = normalizeEmail(email);
        return Arrays.stream(adminAllowlistEmails.split(","))
                .map(String::trim)
                .filter(candidate -> !candidate.isBlank())
                .map(this::normalizeEmail)
                .anyMatch(normalizedEmail::equals);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
