"use client";

import {
  AlertCircle,
  ArrowLeft,
  Check,
  Loader2,
  RotateCcw,
  Save,
  UserRound,
} from "lucide-react";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { Suspense, useCallback, useEffect, useRef, useState, type FormEvent } from "react";
import { ApiError } from "@/lib/api";
import { resolveAccessToken } from "@/lib/auth";
import { fetchMemberProfile, updateMemberProfile } from "@/lib/members";

type AvatarColorKey = "skin" | "hair" | "shirt";
type ColorMode = "original" | "custom";

type CharacterDraft = {
  displayName: string;
  avatarKind: string;
  colorMode: ColorMode;
  avatarColors: Record<AvatarColorKey, string>;
};

type ProfileLoadState =
  | { status: "loading"; error: null }
  | { status: "ready"; error: null }
  | { status: "error"; error: string };

type ProfileSaveState = {
  status: "idle" | "submitting" | "success" | "error";
  message: string | null;
};

const CHARACTER_STORAGE_KEY = "ai-office.character";

const CHARACTER_ARCHETYPES = [
  {
    value: "operator",
    label: "Operator",
    description: "운영 중심",
    detail: "운영 프로필",
    sigil: "O",
  },
  {
    value: "analyst",
    label: "Analyst",
    description: "분석 중심",
    detail: "분석 프로필",
    sigil: "A",
  },
  {
    value: "builder",
    label: "Builder",
    description: "구축 중심",
    detail: "구축 프로필",
    sigil: "B",
  },
  {
    value: "designer",
    label: "Designer",
    description: "설계 중심",
    detail: "설계 프로필",
    sigil: "D",
  },
] as const;

const AVATAR_OPTIONS = CHARACTER_ARCHETYPES;

const COLOR_MODE_OPTIONS: { value: ColorMode; label: string; description: string }[] = [
  { value: "original", label: "Original", description: "서비스 기본 색상" },
  { value: "custom", label: "Custom", description: "직접 색상 선택" },
];

const COLOR_FIELDS: { key: AvatarColorKey; label: string }[] = [
  { key: "skin", label: "피부" },
  { key: "hair", label: "머리" },
  { key: "shirt", label: "상의" },
];

const ORIGINAL_AVATAR_COLORS: Record<AvatarColorKey, string> = {
  skin: "#c8916b",
  hair: "#2b1f1a",
  shirt: "#27364a",
};

const CUSTOM_AVATAR_COLORS: Record<AvatarColorKey, string> = {
  skin: "#d8b48a",
  hair: "#101820",
  shirt: "#1f7a5f",
};

const DEFAULT_CHARACTER: CharacterDraft = {
  displayName: "",
  avatarKind: "operator",
  colorMode: "original",
  avatarColors: ORIGINAL_AVATAR_COLORS,
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
  const [draft, setDraft] = useState<CharacterDraft>(DEFAULT_CHARACTER);
  const draftRef = useRef<CharacterDraft>(DEFAULT_CHARACTER);
  const draftDirtyRef = useRef(false);
  const loadRequestIdRef = useRef(0);
  const [loadState, setLoadState] = useState<ProfileLoadState>({
    status: "loading",
    error: null,
  });
  const [saveState, setSaveState] = useState<ProfileSaveState>({
    status: "idle",
    message: null,
  });

  const setCharacterDraft = useCallback((nextDraft: CharacterDraft) => {
    const normalizedDraft = normalizeCharacterDraft(nextDraft);
    draftRef.current = normalizedDraft;
    setDraft(normalizedDraft);
  }, []);

  const loadProfile = useCallback(async () => {
    const requestId = loadRequestIdRef.current + 1;
    loadRequestIdRef.current = requestId;
    const storedCharacter = loadStoredCharacter();
    if (storedCharacter) {
      setCharacterDraft(storedCharacter);
    }

    setLoadState({ status: "loading", error: null });
    try {
      const profile = await fetchMemberProfile(resolveAccessToken());
      if (requestId !== loadRequestIdRef.current || draftDirtyRef.current) {
        setLoadState({ status: "ready", error: null });
        return;
      }
      const nextDraft = normalizeCharacterDraft({
        displayName: profile.displayName,
        avatarKind: profile.avatarKind,
        colorMode: colorsMatchOriginal(profile.avatarColors) ? "original" : "custom",
        avatarColors: profile.avatarColors,
      });
      setCharacterDraft(nextDraft);
      saveStoredCharacter(nextDraft);
      setLoadState({ status: "ready", error: null });
    } catch (error) {
      setLoadState({
        status: "error",
        error: messageFromError(error, "회원 프로필을 불러오지 못했습니다."),
      });
    }
  }, [setCharacterDraft]);

  useEffect(() => {
    queueMicrotask(() => {
      void loadProfile();
    });
  }, [loadProfile]);

  const updateDraft = (updater: (current: CharacterDraft) => CharacterDraft) => {
    draftDirtyRef.current = true;
    setCharacterDraft(updater(draftRef.current));
    setSaveState({ status: "idle", message: null });
  };

  const selectAvatarKind = (avatarKind: string) => {
    updateDraft((current) => ({ ...current, avatarKind }));
  };

  const selectColorMode = (colorMode: ColorMode) => {
    const currentDraft = draftRef.current;
    const avatarColors =
      colorMode === "original"
        ? ORIGINAL_AVATAR_COLORS
        : currentDraft.colorMode === "original"
          ? CUSTOM_AVATAR_COLORS
          : currentDraft.avatarColors;
    updateDraft((current) => ({ ...current, colorMode, avatarColors }));
  };

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    loadRequestIdRef.current += 1;
    draftDirtyRef.current = true;

    const nextDraft = normalizeCharacterDraft({
      ...draftRef.current,
      displayName: draftRef.current.displayName.trim(),
      avatarKind: draftRef.current.avatarKind.trim(),
      avatarColors: {
        skin: draftRef.current.avatarColors.skin.trim(),
        hair: draftRef.current.avatarColors.hair.trim(),
        shirt: draftRef.current.avatarColors.shirt.trim(),
      },
    });

    if (
      !nextDraft.displayName ||
      !nextDraft.avatarKind ||
      !nextDraft.avatarColors.skin ||
      !nextDraft.avatarColors.hair ||
      !nextDraft.avatarColors.shirt
    ) {
      setSaveState({
        status: "error",
        message: "표시 이름, avatar kind, skin/hair/shirt 색상을 입력해 주세요.",
      });
      return;
    }

    setSaveState({ status: "submitting", message: null });
    try {
      await updateMemberProfile(resolveAccessToken(), {
        displayName: nextDraft.displayName,
        avatarKind: nextDraft.avatarKind,
        avatarColors: nextDraft.avatarColors,
      });
      saveStoredCharacter(nextDraft);
      setCharacterDraft(nextDraft);
      draftDirtyRef.current = false;
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
    draft.displayName.trim().length > 0 && draft.avatarKind.trim().length > 0 && !isSubmitting;
  const selectedArchetype =
    AVATAR_OPTIONS.find((option) => option.value === draft.avatarKind) ?? AVATAR_OPTIONS[0];

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
            <p>표시 이름, avatar 종류, 색상 모드를 설정합니다.</p>
          </div>
        </div>

        {loadState.status === "loading" ? (
          <div className="workspace-task-notice">
            <Loader2 className="spin" aria-hidden="true" />
            <span>프로필을 불러오고 있습니다.</span>
          </div>
        ) : null}
        {loadState.status === "error" ? (
          <div className="workspace-task-notice character-notice-row" role="alert">
            <AlertCircle aria-hidden="true" />
            <span>{loadState.error}</span>
            <button
              className="secondary-action workspace-task-refresh"
              type="button"
              onClick={() => void loadProfile()}
            >
              <RotateCcw aria-hidden="true" />
              <span>다시 시도</span>
            </button>
          </div>
        ) : null}

        <form className="workspace-create-form" onSubmit={handleSubmit}>
          <label className="form-field">
            <span>
              <strong>표시 이름</strong>
              <small>displayName</small>
            </span>
            <input
              aria-label="표시 이름"
              value={draft.displayName}
              onChange={(event) => {
                updateDraft((current) => ({ ...current, displayName: event.target.value }));
              }}
              maxLength={40}
              disabled={isSubmitting}
            />
          </label>

          <fieldset className="character-option-group" aria-label="Avatar 종류 선택">
            <legend className="eyebrow">Avatar kind</legend>
            <div className="character-option-grid" role="radiogroup" aria-label="Avatar 종류 선택">
              {AVATAR_OPTIONS.map((option) => {
                const isActive = draft.avatarKind === option.value;
                return (
                  <button
                    className={`character-option ${isActive ? "character-option-active" : ""}`}
                    type="button"
                    role="radio"
                    aria-checked={isActive}
                    key={option.value}
                    onClick={() => selectAvatarKind(option.value)}
                    disabled={isSubmitting}
                  >
                    <span className="character-option-check">
                      {isActive ? <Check aria-hidden="true" /> : null}
                    </span>
                    <span className="character-option-sigil" aria-hidden="true">
                      {option.sigil}
                    </span>
                    <strong>{option.label}</strong>
                    <small>{option.description}</small>
                    <span className="character-option-detail">{option.detail}</span>
                  </button>
                );
              })}
            </div>
          </fieldset>

          <fieldset className="character-option-group" aria-label="색상 모드 선택">
            <legend className="eyebrow">Color mode</legend>
            <div className="character-segmented" role="radiogroup" aria-label="색상 모드 선택">
              {COLOR_MODE_OPTIONS.map((option) => {
                const isActive = draft.colorMode === option.value;
                return (
                  <button
                    className={`character-segment ${isActive ? "character-segment-active" : ""}`}
                    type="button"
                    role="radio"
                    aria-checked={isActive}
                    key={option.value}
                    onClick={() => selectColorMode(option.value)}
                    disabled={isSubmitting}
                  >
                    <strong>{option.label}</strong>
                    <small>{option.description}</small>
                  </button>
                );
              })}
            </div>
          </fieldset>

          <fieldset className="character-option-group" aria-label="Avatar 색상 설정">
            <legend className="eyebrow">Avatar colors</legend>
            <div className="character-color-grid">
              {COLOR_FIELDS.map((field) => (
                <label className="form-field character-color-field" key={field.key}>
                  <span>
                    <strong>{field.label}</strong>
                    <small>{`avatarColors.${field.key}`}</small>
                  </span>
                  <span className="character-color-input">
                    <span
                      className="character-color-swatch"
                      style={{ background: draft.avatarColors[field.key] }}
                      aria-hidden="true"
                    />
                    <input
                      aria-label={`${field.label} 색상`}
                      type="color"
                      value={draft.avatarColors[field.key]}
                      onChange={(event) => {
                        updateDraft((current) => ({
                          ...current,
                          colorMode: "custom",
                          avatarColors: {
                            ...current.avatarColors,
                            [field.key]: event.target.value,
                          },
                        }));
                      }}
                      disabled={isSubmitting || draft.colorMode === "original"}
                    />
                  </span>
                </label>
              ))}
            </div>
          </fieldset>

          <div className="character-preview character-preview-gothic" aria-label="캐릭터 미리보기">
            <div
              className={`character-preview-avatar character-preview-gothic-avatar character-preview-${draft.avatarKind}`}
            >
              <span className="character-avatar-halo" aria-hidden="true" />
              <span
                className="character-avatar-mantle"
                style={{ background: draft.avatarColors.shirt }}
                aria-hidden="true"
              />
              <span
                className="character-avatar-armor"
                style={{ background: draft.avatarColors.shirt }}
                aria-hidden="true"
              />
              <span
                className="character-avatar-face"
                style={{ background: draft.avatarColors.skin }}
                aria-hidden="true"
              />
              <span
                className="character-avatar-helm"
                style={{ background: draft.avatarColors.hair }}
                aria-hidden="true"
              />
              <span className="character-avatar-visor" aria-hidden="true" />
              <span className="character-avatar-sigil" aria-hidden="true">
                {selectedArchetype.sigil}
              </span>
            </div>
            <div className="character-preview-copy">
              <span className="character-preview-kicker">Profile preview</span>
              <strong>{draft.displayName || "표시 이름"}</strong>
              <p>{`${selectedArchetype.label} / ${draft.colorMode}`}</p>
              <div className="character-preview-status" aria-label="캐릭터 상태">
                <span>{selectedArchetype.detail}</span>
                <span>{draft.colorMode === "original" ? "기본 색상" : "사용자 색상"}</span>
              </div>
            </div>
          </div>

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

function normalizeCharacterDraft(value: CharacterDraft): CharacterDraft {
  const colorMode: ColorMode = value.colorMode === "custom" ? "custom" : "original";
  return {
    displayName: typeof value.displayName === "string" ? value.displayName : "",
    avatarKind: typeof value.avatarKind === "string" ? value.avatarKind : "operator",
    colorMode,
    avatarColors:
      colorMode === "original"
        ? ORIGINAL_AVATAR_COLORS
        : normalizeAvatarColors(value.avatarColors, CUSTOM_AVATAR_COLORS),
  };
}

function normalizeAvatarColors(
  colors: Partial<Record<AvatarColorKey, string>> | null | undefined,
  fallback: Record<AvatarColorKey, string>,
): Record<AvatarColorKey, string> {
  return {
    skin: validColor(colors?.skin) ? colors.skin : fallback.skin,
    hair: validColor(colors?.hair) ? colors.hair : fallback.hair,
    shirt: validColor(colors?.shirt) ? colors.shirt : fallback.shirt,
  };
}

function loadStoredCharacter(): CharacterDraft | null {
  if (typeof window === "undefined") {
    return null;
  }

  try {
    const rawCharacter = window.localStorage.getItem(CHARACTER_STORAGE_KEY);
    if (!rawCharacter) {
      return null;
    }
    const parsedCharacter = JSON.parse(rawCharacter) as Partial<CharacterDraft>;
    return normalizeCharacterDraft({
      displayName: typeof parsedCharacter.displayName === "string" ? parsedCharacter.displayName : "",
      avatarKind: typeof parsedCharacter.avatarKind === "string" ? parsedCharacter.avatarKind : "operator",
      colorMode: parsedCharacter.colorMode === "custom" ? "custom" : "original",
      avatarColors: normalizeAvatarColors(parsedCharacter.avatarColors, ORIGINAL_AVATAR_COLORS),
    });
  } catch {
    return null;
  }
}

function saveStoredCharacter(character: CharacterDraft) {
  if (typeof window === "undefined") {
    return;
  }

  window.localStorage.setItem(CHARACTER_STORAGE_KEY, JSON.stringify(normalizeCharacterDraft(character)));
}

function colorsMatchOriginal(colors: Partial<Record<AvatarColorKey, string>>): boolean {
  return COLOR_FIELDS.every((field) => colors[field.key]?.toLowerCase() === ORIGINAL_AVATAR_COLORS[field.key]);
}

function validColor(value: string | undefined): value is string {
  return Boolean(value && /^#[0-9a-f]{6}$/i.test(value));
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
