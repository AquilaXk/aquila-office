import { apiFetch } from "@/lib/api-client";

export type MemberAvatarColors = {
  skin: string;
  hair: string;
  shirt: string;
};

export type MemberProfile = {
  memberId: number;
  displayName: string;
  avatarKind: string;
  avatarColors: MemberAvatarColors;
};

export type UpdateMemberProfileInput = {
  displayName?: string;
  avatarKind?: string;
  avatarColors?: MemberAvatarColors;
};

export function memberProfilePath(): string {
  return "/api/v1/members/me/profile";
}

export async function fetchMemberProfile(accessToken: string): Promise<MemberProfile> {
  return apiFetch<MemberProfile>(memberProfilePath(), {
    accessToken,
  });
}

export async function updateMemberProfile(
  accessToken: string,
  input: UpdateMemberProfileInput,
): Promise<MemberProfile> {
  return apiFetch<MemberProfile>(memberProfilePath(), {
    method: "PATCH",
    accessToken,
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(input),
  });
}
