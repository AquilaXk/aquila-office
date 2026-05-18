"use client";

import { AlertCircle, ArrowLeft, Loader2, Save, UserRound } from "lucide-react";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { Suspense, useEffect, useState, type FormEvent } from "react";
import { ApiError } from "@/lib/api-client";
import { resolveAccessToken } from "@/lib/auth";
import { fetchMemberProfile, updateMemberProfile } from "@/lib/members";

type AvatarColorKey = "skin" | "hair" | "shirt";

type ProfileLoadState =
  | { status: "loading"; error: null }
  | { status: "ready"; error: null }
  | { status: "error"; error: string };

type ProfileSaveState = {
  status: "idle" | "submitting" | "success" | "error";
  message: string | null;
};

const COLOR_FIELDS: { key: AvatarColorKey; label: string }[] = [
  { key: "skin", label: "Skin" },
  { key: "hair", label: "Hair" },
  { key: "shirt", label: "Shirt" },
];

const DEFAULT_AVATAR_COLORS: Record<AvatarColorKey, string> = {
  skin: "#d8b48a",
  hair: "#101820",
  shirt: "#7c3aed",
};

export default function CharacterProfilePage() {
  return (
    <Suspense fallback={null}>
      <CharacterProfileForm />
    </Suspense>
  );
}

function CharacterProfileForm() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const returnTo = safeReturnTo(searchParams.get("returnTo"));
  const [loadState, setLoadState] = useState<ProfileLoadState>({
    status: "loading",
    error: null,
  });
  const [saveState, setSaveState] = useState<ProfileSaveState>({
    status: "idle",
    message: null,
  });
  const [displayName, setDisplayName] = useState("");
  const [avatarKind, setAvatarKind] = useState("operator");
  const [avatarColors, setAvatarColors] =
    useState<Record<AvatarColorKey, string>>(DEFAULT_AVATAR_COLORS);

  useEffect(() => {
    let cancelled = false;

    fetchMemberProfile(resolveAccessToken())
      .then((profile) => {
        if (cancelled) {
          return;
        }
        setDisplayName(profile.displayName);
        setAvatarKind(profile.avatarKind);
        setAvatarColors(profile.avatarColors);
        setLoadState({ status: "ready", error: null });
      })
      .catch((error) => {
        if (cancelled) {
          return;
        }
        setLoadState({
          status: "error",
          error: messageFromError(error, "회원 프로필을 불러오지 못했습니다."),
        });
      });

    return () => {
      cancelled = true;
    };
  }, []);

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    const nextDisplayName = displayName.trim();
    const nextAvatarKind = avatarKind.trim();
    const nextAvatarColors = {
      skin: avatarColors.skin.trim(),
      hair: avatarColors.hair.trim(),
      shirt: avatarColors.shirt.trim(),
    };
    if (!nextDisplayName || !nextAvatarKind || !nextAvatarColors.skin || !nextAvatarColors.hair || !nextAvatarColors.shirt) {
      setSaveState({
        status: "error",
        message: "표시 이름, avatar kind, skin/hair/shirt 색상을 입력해 주세요.",
      });
      return;
    }

    setSaveState({ status: "submitting", message: null });
    try {
      await updateMemberProfile(resolveAccessToken(), {
        displayName: nextDisplayName,
        avatarKind: nextAvatarKind,
        avatarColors: nextAvatarColors,
      });
      setSaveState({
        status: "success",
        message: "프로필이 저장되었습니다.",
      });
      router.replace(returnTo);
    } catch (error) {
      setSaveState({
        status: "error",
        message: messageFromError(error, "프로필을 저장하지 못했습니다."),
      });
    }
  };

  const isSubmitting = saveState.status === "submitting";
  const canSubmit =
    displayName.trim().length > 0 && avatarKind.trim().length > 0 && !isSubmitting;

  return (
    <main className="workspace-create-shell">
      <section className="workspace-create-panel" aria-label="캐릭터 프로필 설정">
        <div className="workspace-create-header">
          <div className="workspace-create-icon">
            <UserRound aria-hidden="true" />
          </div>
          <div className="workspace-create-title">
            <p className="eyebrow">Profile</p>
            <h1>캐릭터 프로필</h1>
            <p>aquila office에서 표시할 이름과 avatar kind를 설정합니다.</p>
          </div>
        </div>

        {loadState.status === "loading" ? (
          <div className="workspace-task-notice">
            <Loader2 className="spin" aria-hidden="true" />
            <span>프로필을 불러오고 있습니다.</span>
          </div>
        ) : null}
        {loadState.status === "error" ? (
          <div className="workspace-task-notice" role="alert">
            <AlertCircle aria-hidden="true" />
            <span>{loadState.error}</span>
          </div>
        ) : null}

        <form className="workspace-create-form" onSubmit={handleSubmit}>
          <label className="form-field">
            <span>
              <strong>표시 이름</strong>
              <small>displayName</small>
            </span>
            <input
              value={displayName}
              onChange={(event) => {
                setDisplayName(event.target.value);
                setSaveState({ status: "idle", message: null });
              }}
              maxLength={40}
              disabled={isSubmitting}
            />
          </label>

          <label className="form-field">
            <span>
              <strong>Avatar kind</strong>
              <small>avatarKind</small>
            </span>
            <input
              value={avatarKind}
              onChange={(event) => {
                setAvatarKind(event.target.value);
                setSaveState({ status: "idle", message: null });
              }}
              maxLength={40}
              disabled={isSubmitting}
            />
          </label>

          <fieldset className="workspace-create-form" aria-label="Avatar 색상 설정">
            <legend className="eyebrow">Avatar colors</legend>
            {COLOR_FIELDS.map((field) => (
              <label className="form-field" key={field.key}>
                <span>
                  <strong>{field.label}</strong>
                  <small>{`avatarColors.${field.key}`}</small>
                </span>
                <input
                  type="color"
                  value={avatarColors[field.key]}
                  onChange={(event) => {
                    setAvatarColors((current) => ({
                      ...current,
                      [field.key]: event.target.value,
                    }));
                    setSaveState({ status: "idle", message: null });
                  }}
                  disabled={isSubmitting}
                />
              </label>
            ))}
          </fieldset>

          {saveState.status === "error" && saveState.message ? (
            <div className="workspace-task-notice" role="alert">
              <AlertCircle aria-hidden="true" />
              <span>{saveState.message}</span>
            </div>
          ) : null}
          {saveState.status === "success" && saveState.message ? (
            <div className="workspace-task-notice">
              <Save aria-hidden="true" />
              <span>{saveState.message}</span>
            </div>
          ) : null}

          <div className="workspace-modal-actions">
            <Link className="secondary-action" href={returnTo}>
              <ArrowLeft aria-hidden="true" />
              <span>돌아가기</span>
            </Link>
            <button
              className="primary-action primary-action-active"
              type="submit"
              disabled={!canSubmit}
            >
              {isSubmitting ? <Loader2 className="spin" aria-hidden="true" /> : <Save aria-hidden="true" />}
              <span>{isSubmitting ? "저장 중" : "프로필 저장"}</span>
            </button>
          </div>
        </form>
      </section>
    </main>
  );
}

function safeReturnTo(value: string | null): string {
  if (!value || !value.startsWith("/") || value.startsWith("//")) {
    return "/workspaces";
  }
  return value;
}

function messageFromError(error: unknown, fallback: string): string {
  if (error instanceof ApiError) {
    return error.message;
  }
  if (error instanceof Error && error.message) {
    return error.message;
  }
  return fallback;
}
