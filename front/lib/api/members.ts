import { resolveAccessToken } from "../auth.ts";
import {
  fetchMemberProfile,
  updateMemberProfile,
  type MemberAvatarColors,
  type MemberProfile,
} from "../members.ts";

export type { MemberAvatarColors, MemberProfile };

export interface UpdateMemberProfileReq {
  displayName: string;
  avatarKind: string;
  avatarColors: MemberAvatarColors;
}

export function getMyMemberProfile(): Promise<MemberProfile> {
  return fetchMemberProfile(resolveAccessToken());
}

export function updateMyMemberProfile(input: UpdateMemberProfileReq): Promise<MemberProfile> {
  return updateMemberProfile(resolveAccessToken(), input);
}
