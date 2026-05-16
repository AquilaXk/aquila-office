package com.aquilaoffice.application.service;

import com.aquilaoffice.application.port.in.GetMemberProfileQuery;
import com.aquilaoffice.application.port.in.GetMemberProfileUseCase;
import com.aquilaoffice.application.port.in.MemberProfileResult;
import com.aquilaoffice.application.port.in.UpdateMemberProfileCommand;
import com.aquilaoffice.application.port.in.UpdateMemberProfileUseCase;
import com.aquilaoffice.application.port.out.LoadMemberPort;
import com.aquilaoffice.application.port.out.LoadMemberProfilePort;
import com.aquilaoffice.application.port.out.MemberProfileUpdate;
import com.aquilaoffice.application.port.out.UpdateMemberProfilePort;
import java.util.Objects;

/**
 * 멤버 프로필 조회/수정 유스케이스를 수행하는 application service다.
 */
public class MemberProfileService implements GetMemberProfileUseCase, UpdateMemberProfileUseCase {

    private final LoadMemberPort loadMemberPort;
    private final LoadMemberProfilePort loadMemberProfilePort;
    private final UpdateMemberProfilePort updateMemberProfilePort;

    public MemberProfileService(
            LoadMemberPort loadMemberPort,
            LoadMemberProfilePort loadMemberProfilePort,
            UpdateMemberProfilePort updateMemberProfilePort) {
        this.loadMemberPort = Objects.requireNonNull(loadMemberPort, "loadMemberPort must not be null");
        this.loadMemberProfilePort =
                Objects.requireNonNull(loadMemberProfilePort, "loadMemberProfilePort must not be null");
        this.updateMemberProfilePort =
                Objects.requireNonNull(updateMemberProfilePort, "updateMemberProfilePort must not be null");
    }

    @Override
    public MemberProfileResult getMemberProfile(GetMemberProfileQuery query) {
        Objects.requireNonNull(query, "query must not be null");
        String googleSub = requireExistingMember(query.memberGoogleSub());
        return loadMemberProfilePort
                .findOrCreateDefaultByGoogleSub(googleSub)
                .orElseThrow(() -> new IllegalArgumentException("member profile not found"));
    }

    @Override
    public MemberProfileResult updateMemberProfile(UpdateMemberProfileCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        String googleSub = requireExistingMember(command.memberGoogleSub());

        return updateMemberProfilePort
                .update(new MemberProfileUpdate(
                        googleSub,
                        command.displayName(),
                        command.avatarKind(),
                        command.skinColor(),
                        command.hairColor(),
                        command.shirtColor()))
                .orElseThrow(() -> new IllegalArgumentException("member profile not found"));
    }

    private String requireExistingMember(String googleSub) {
        return loadMemberPort
                .findByGoogleSub(googleSub)
                .orElseThrow(() -> new IllegalArgumentException("member not found"))
                .getGoogleSub();
    }
}

