"use client";

import { useRouter, useSearchParams } from "next/navigation";
import { Suspense, useEffect, useState } from "react";
import { ArrowRight, Check, Loader2, Palette, Shuffle, Sparkles } from "lucide-react";
import {
  PA_PALETTES,
  PixelAvatar,
  type PixelAvatarDirection,
  type PixelAvatarKind,
  type PixelAvatarPaletteOverride,
} from "@/components/arcade";
import {
  CHARACTER_GEAR_OPTIONS,
  CHARACTER_HAIR_OPTIONS,
  CHARACTER_OUTFIT_OPTIONS,
  type PixelActivity,
  type PixelAvatarGearChoice,
} from "@/lib/pixel-office/character-renderer";
import { AppTopChrome } from "@/components/desktop/AppTopChrome";
import { Button, Input } from "@/components/ui";
import {
  getMyMemberProfile,
  updateMyMemberProfile,
  type MemberProfile,
} from "@/lib/api/members";
import { hydrateDesktopAccessToken, isAuthenticated } from "@/lib/auth";

interface AvatarOption {
  kind: PixelAvatarKind;
  role: string;
  sub: string;
}

const AVATARS: AvatarOption[] = [
  { kind: "alex", role: "Frontend", sub: "화면 구성과 사용 흐름을 다듬습니다." },
  { kind: "mira", role: "Design", sub: "팀이 보는 화면과 인터랙션을 정리합니다." },
  { kind: "kenji", role: "Backend", sub: "API와 데이터 흐름을 안정적으로 연결합니다." },
  { kind: "yuna", role: "Product", sub: "작업 우선순위와 요구사항을 정리합니다." },
  { kind: "diego", role: "DevOps", sub: "실행 환경과 배포 파이프라인을 관리합니다." },
  { kind: "iris", role: "AI", sub: "에이전트와 협업해 작업을 수행합니다." },
];

type ColorTarget = "skin" | "hair" | "shirt";
type ColorMode = "original" | "custom";

interface ColorOption {
  label: string;
  value: string;
  shade: string;
}

type AvatarColors = Record<ColorTarget, ColorOption>;

const COLOR_OPTIONS: Record<ColorTarget, ColorOption[]> = {
  skin: [
    { label: "01", value: "#f8d4b0", shade: "#c8a47a" },
    { label: "02", value: "#f0c098", shade: "#c08868" },
    { label: "03", value: "#d8a878", shade: "#a87848" },
    { label: "04", value: "#9a6a48", shade: "#6f472f" },
  ],
  hair: [
    { label: "01", value: "#1a1a1a", shade: "#0a0a0a" },
    { label: "02", value: "#3a2a1a", shade: "#1f1410" },
    { label: "03", value: "#a83a3a", shade: "#6a1f1f" },
    { label: "04", value: "#5a3a8a", shade: "#2a1a4a" },
    { label: "05", value: "#d8a838", shade: "#a87808" },
  ],
  shirt: [
    { label: "01", value: "#2a3a4a", shade: "#15202c" },
    { label: "02", value: "#3a5a3a", shade: "#1a3a1a" },
    { label: "03", value: "#5a3a8a", shade: "#2a1a4a" },
    { label: "04", value: "#a83a6a", shade: "#681a3a" },
    { label: "05", value: "#d8c89a", shade: "#a89868" },
  ],
};

const DEFAULT_COLORS: AvatarColors = {
  skin: COLOR_OPTIONS.skin[0],
  hair: COLOR_OPTIONS.hair[0],
  shirt: COLOR_OPTIONS.shirt[0],
};

const CHARACTER_STORAGE_KEY = "ai-office.character";
const AVATAR_KINDS = AVATARS.map((avatar) => avatar.kind);

function originalColorsFor(kind: PixelAvatarKind): AvatarColors {
  const palette = PA_PALETTES[kind] ?? PA_PALETTES.mira;
  return {
    skin: { label: "ORIGINAL", value: palette.skin, shade: palette.skin2 },
    hair: { label: "ORIGINAL", value: palette.hair, shade: palette.hair2 },
    shirt: { label: "ORIGINAL", value: palette.shirt, shade: palette.shirt2 },
  };
}

function isOriginalColorSet(
  kind: PixelAvatarKind,
  colors?: Partial<Record<ColorTarget, string>>,
) {
  const original = originalColorsFor(kind);
  return (
    colors?.skin === original.skin.value &&
    colors?.hair === original.hair.value &&
    colors?.shirt === original.shirt.value
  );
}

function avatarColorsToPalette(colors: AvatarColors): PixelAvatarPaletteOverride {
  return {
    skin: colors.skin.value,
    skin2: colors.skin.shade,
    hair: colors.hair.value,
    hair2: colors.hair.shade,
    shirt: colors.shirt.value,
    shirt2: colors.shirt.shade,
  };
}

function buildPaletteOverride(
  colorMode: ColorMode,
  colors: AvatarColors,
  wardrobe: { hairStyle: string; outfitStyle: string; gear: PixelAvatarGearChoice },
): PixelAvatarPaletteOverride | undefined {
  const override: PixelAvatarPaletteOverride =
    colorMode === "custom" ? avatarColorsToPalette(colors) : {};
  if (wardrobe.hairStyle) {
    override.hairStyle = wardrobe.hairStyle;
  }
  if (wardrobe.outfitStyle) {
    override.outfitStyle = wardrobe.outfitStyle;
  }
  if (wardrobe.gear && wardrobe.gear !== "none") {
    override.gear = wardrobe.gear;
  }
  return Object.keys(override).length > 0 ? override : undefined;
}

function findColorOption(target: ColorTarget, value?: string): ColorOption {
  return COLOR_OPTIONS[target].find((option) => option.value === value) ?? DEFAULT_COLORS[target];
}

function loadSavedCharacter(): {
  kind: PixelAvatarKind;
  name: string;
  colors: AvatarColors;
  colorMode: ColorMode;
  hairStyle: string;
  outfitStyle: string;
  gear: PixelAvatarGearChoice;
} {
  if (typeof window === "undefined") {
    return {
      kind: "mira",
      name: "",
      colors: DEFAULT_COLORS,
      colorMode: "original",
      hairStyle: "",
      outfitStyle: "",
      gear: "none",
    };
  }
  try {
    const raw = window.localStorage.getItem(CHARACTER_STORAGE_KEY);
    if (!raw) {
      return {
        kind: "mira",
        name: "",
        colors: DEFAULT_COLORS,
        colorMode: "original",
        hairStyle: "",
        outfitStyle: "",
        gear: "none",
      };
    }
    const saved = JSON.parse(raw) as {
      kind?: string;
      name?: string;
      colors?: Partial<Record<ColorTarget, string>>;
      colorMode?: ColorMode;
      paletteOverride?: PixelAvatarPaletteOverride;
    };
    const kind =
      saved.kind && AVATAR_KINDS.includes(saved.kind as PixelAvatarKind)
        ? (saved.kind as PixelAvatarKind)
        : "mira";
    const name = typeof saved.name === "string" ? saved.name : "";
    const colorMode =
      saved.colorMode === "original" || isOriginalColorSet(kind, saved.colors)
        ? "original"
        : "custom";
    const hairStyle =
      typeof saved.paletteOverride?.hairStyle === "string" ? saved.paletteOverride.hairStyle : "";
    const outfitStyle =
      typeof saved.paletteOverride?.outfitStyle === "string"
        ? saved.paletteOverride.outfitStyle
        : "";
    const gear =
      typeof saved.paletteOverride?.gear === "string"
        ? (saved.paletteOverride.gear as PixelAvatarGearChoice)
        : "none";
    return {
      kind,
      name,
      colorMode,
      hairStyle,
      outfitStyle,
      gear,
      colors: {
        skin: findColorOption("skin", saved.colors?.skin),
        hair: findColorOption("hair", saved.colors?.hair),
        shirt: findColorOption("shirt", saved.colors?.shirt),
      },
    };
  } catch {
    return {
      kind: "mira",
      name: "",
      colors: DEFAULT_COLORS,
      colorMode: "original",
      hairStyle: "",
      outfitStyle: "",
      gear: "none",
    };
  }
}

function characterFromProfile(profile: MemberProfile): {
  kind: PixelAvatarKind;
  name: string;
  colors: AvatarColors;
  colorMode: ColorMode;
} {
  const kind = AVATAR_KINDS.includes(profile.avatarKind as PixelAvatarKind)
    ? (profile.avatarKind as PixelAvatarKind)
    : "mira";
  return {
    kind,
    name: profile.displayName,
    colorMode: isOriginalColorSet(kind, profile.avatarColors) ? "original" : "custom",
    colors: {
      skin: findColorOption("skin", profile.avatarColors?.skin),
      hair: findColorOption("hair", profile.avatarColors?.hair),
      shirt: findColorOption("shirt", profile.avatarColors?.shirt),
    },
  };
}

function writeSavedCharacter(
  kind: PixelAvatarKind,
  name: string,
  colors: AvatarColors,
  colorMode: ColorMode,
  paletteOverride?: PixelAvatarPaletteOverride,
) {
  if (typeof window === "undefined") return;
  window.localStorage.setItem(
    CHARACTER_STORAGE_KEY,
    JSON.stringify({
      kind,
      name,
      colors: {
        skin: colors.skin.value,
        hair: colors.hair.value,
        shirt: colors.shirt.value,
      },
      colorMode,
      paletteOverride,
    }),
  );
}

export default function CharacterPage() {
  return (
    <Suspense fallback={null}>
      <CharacterInner />
    </Suspense>
  );
}

function CharacterInner() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const returnTo = safeReturnTo(searchParams.get("returnTo"));
  const [kind, setKind] = useState<PixelAvatarKind>("mira");
  const [name, setName] = useState("");
  const [colors, setColors] = useState<AvatarColors>(DEFAULT_COLORS);
  const [colorMode, setColorMode] = useState<ColorMode>("original");
  const [hairStyle, setHairStyle] = useState<string>("");
  const [outfitStyle, setOutfitStyle] = useState<string>("");
  const [gear, setGear] = useState<PixelAvatarGearChoice>("none");
  const [saving, setSaving] = useState(false);
  const [previewDirection, setPreviewDirection] = useState<PixelAvatarDirection>("S");
  const [previewActivity, setPreviewActivity] = useState<PixelActivity>("idle");

  useEffect(() => {
    let cancelled = false;
    void hydrateDesktopAccessToken().then(() => {
      if (cancelled) return;
      if (!isAuthenticated()) {
        router.replace("/auth");
        return;
      }
      const saved = loadSavedCharacter();
      setKind(saved.kind);
      setName(saved.name);
      setColors(saved.colors);
      setColorMode(saved.colorMode);
      setHairStyle(saved.hairStyle);
      setOutfitStyle(saved.outfitStyle);
      setGear(saved.gear);
      getMyMemberProfile()
        .then((profile) => {
          if (cancelled) return;
          const next = characterFromProfile(profile);
          setKind(next.kind);
          setName(next.name);
          setColors(next.colors);
          setColorMode(next.colorMode);
        })
        .catch(() => {
          // 백엔드 프로필이 없을 때는 로컬 저장 프로필 유지
        });
    });
    return () => {
      cancelled = true;
    };
  }, [router]);

  const trimmed = name.trim();
  const canSubmit = trimmed.length > 0;
  const selected = AVATARS.find((avatar) => avatar.kind === kind) ?? AVATARS[1];
  const activeColors = colorMode === "original" ? originalColorsFor(kind) : colors;
  const paletteOverride = buildPaletteOverride(colorMode, colors, {
    hairStyle,
    outfitStyle,
    gear,
  });

  function randomizeWardrobeStyle() {
    const pick = <T,>(items: readonly T[]): T => items[Math.floor(Math.random() * items.length)];
    setKind(pick(AVATAR_KINDS));
    setColorMode("custom");
    setColors({
      skin: pick(COLOR_OPTIONS.skin),
      hair: pick(COLOR_OPTIONS.hair),
      shirt: pick(COLOR_OPTIONS.shirt),
    });
    setHairStyle(pick(CHARACTER_HAIR_OPTIONS).id);
    setOutfitStyle(pick(CHARACTER_OUTFIT_OPTIONS).id);
    const nonEmptyGear = CHARACTER_GEAR_OPTIONS.filter((opt) => opt.id !== "none");
    setGear(pick(nonEmptyGear).id);
  }

  async function enterOffice() {
    if (!canSubmit || saving) return;
    writeSavedCharacter(kind, trimmed, activeColors, colorMode, paletteOverride);
    setSaving(true);
    try {
      const profile = await updateMyMemberProfile({
        displayName: trimmed,
        avatarKind: kind,
        avatarColors: {
          skin: activeColors.skin.value,
          hair: activeColors.hair.value,
          shirt: activeColors.shirt.value,
        },
      });
      const next = characterFromProfile(profile);
      const nextColors = next.colorMode === "original" ? originalColorsFor(next.kind) : next.colors;
      const mergedOverride = buildPaletteOverride(next.colorMode, next.colors, {
        hairStyle,
        outfitStyle,
        gear,
      });
      writeSavedCharacter(next.kind, next.name, nextColors, next.colorMode, mergedOverride);
    } catch {
      // 브라우저 로컬 프로필 보존
    } finally {
      setSaving(false);
      router.replace(returnTo);
    }
  }

  return (
    <AppTopChrome title="프로필 및 아바타 설정" backHref="/workspaces" backLabel="워크스페이스 목록">
      <div className="ao-page-container">
        <header className="ao-page-header">
          <div>
            <p className="ao-page-eyebrow">프로필 설정</p>
            <h1 className="ao-page-title">오피스 아바타 및 표시 이름</h1>
            <p className="ao-page-subtitle">
              2D 오피스 스테이지에서 사용할 캐릭터와 색상을 선택하세요.
            </p>
          </div>
        </header>

        <div className="chrome-split-pane">
          <section className="ao-surface-panel flex flex-col items-center gap-4" aria-label="아바타 미리보기">
            <div className="p-6 rounded-[var(--radius-lg)] border border-[var(--ui-border)] bg-[var(--ui-surface-raised)] flex flex-col items-center gap-3 w-full">
              <div className="flex items-end gap-4">
                <PixelAvatar
                  kind={kind}
                  size={6}
                  direction={previewDirection}
                  activity={previewActivity}
                  paletteOverride={paletteOverride}
                />
                <div className="flex flex-col items-center gap-1">
                  <div className="ao-crew-mugshot-chip">
                    <PixelAvatar
                      kind={kind}
                      variant="mugshot"
                      size={2.2}
                      paletteOverride={paletteOverride}
                    />
                  </div>
                  <span className="text-[12px] text-[var(--ui-text-muted)]">포트레이트</span>
                </div>
              </div>
              <div className="text-center">
                <strong className="block text-[16px] text-[var(--ui-text-strong)]">
                  {trimmed || selected.kind}
                </strong>
                <span className="text-[12px] text-[var(--ui-text-muted)]">{selected.role}</span>
              </div>
              <p className="m-0 text-center text-[13px] text-[var(--ui-text-secondary)]">
                {selected.sub}
              </p>

              <div className="flex flex-col gap-2 w-full pt-2 border-t border-[var(--ui-border-subtle)]">
                <div className="flex flex-wrap items-center justify-center gap-1.5" role="group" aria-label="미리보기 방향">
                  {(
                    [
                      ["S", "정면"],
                      ["E", "우측"],
                      ["N", "후면"],
                      ["W", "좌측"],
                    ] as const
                  ).map(([dir, label]) => (
                    <button
                      key={dir}
                      type="button"
                      className={`tb-btn${previewDirection === dir ? " tb-btn-primary" : ""}`}
                      onClick={() => setPreviewDirection(dir)}
                    >
                      {label}
                    </button>
                  ))}
                </div>
                <div className="flex flex-wrap items-center justify-center gap-1.5" role="group" aria-label="미리보기 동작">
                  {(
                    [
                      ["idle", "대기"],
                      ["walk", "걷기"],
                      ["typing", "타이핑"],
                      ["reading", "독서"],
                    ] as const
                  ).map(([act, label]) => (
                    <button
                      key={act}
                      type="button"
                      className={`tb-btn${previewActivity === act ? " tb-btn-primary" : ""}`}
                      onClick={() => setPreviewActivity(act)}
                    >
                      {label}
                    </button>
                  ))}
                </div>
              </div>
            </div>

            <form
              className="w-full flex flex-col gap-3"
              onSubmit={(event) => {
                event.preventDefault();
                void enterOffice();
              }}
            >
              <label className="form-field">
                <span>
                  <strong>표시 이름</strong>
                  <small>필수</small>
                </span>
                <Input
                  value={name}
                  maxLength={40}
                  placeholder="예: 민수"
                  onChange={(event) => setName(event.target.value)}
                  disabled={saving}
                />
              </label>

              <Button type="submit" loading={saving} disabled={!canSubmit} className="w-full">
                {saving ? (
                  <Loader2 size={14} className="spin" aria-hidden="true" />
                ) : (
                  <ArrowRight size={14} aria-hidden="true" />
                )}
                <span>{saving ? "저장 중" : "저장 후 계속하기"}</span>
              </Button>
            </form>
          </section>

          <div className="flex flex-col gap-4">
            <section className="ao-surface-panel" aria-label="아바타 프리셋 선택">
              <div className="ao-surface-panel-header">
                <h2>아바타 선택</h2>
                <Sparkles size={15} aria-hidden="true" />
              </div>
              <div className="grid grid-cols-2 sm:grid-cols-3 gap-2.5">
                {AVATARS.map((option) => {
                  const active = option.kind === kind;
                  return (
                    <button
                      key={option.kind}
                      type="button"
                      onClick={() => setKind(option.kind)}
                      aria-pressed={active}
                      className={`p-3 rounded-[var(--radius-md)] border text-left flex items-center gap-3 cursor-pointer transition-colors ${
                        active
                          ? "border-[var(--accent-primary)] bg-[var(--ui-selected)]"
                          : "border-[var(--ui-border-subtle)] bg-[var(--ui-surface-raised)]"
                      }`}
                    >
                      <PixelAvatar kind={option.kind} size={3} />
                      <div className="min-w-0">
                        <strong className="block text-[13px] text-[var(--ui-text-strong)] capitalize">
                          {option.kind}
                        </strong>
                        <span className="block text-[12px] text-[var(--ui-text-muted)]">
                          {option.role}
                        </span>
                      </div>
                      {active ? (
                        <Check size={14} className="ml-auto text-[var(--accent-primary)]" aria-hidden="true" />
                      ) : null}
                    </button>
                  );
                })}
              </div>
            </section>

            <section className="ao-surface-panel flex flex-col gap-4" aria-label="색상 사용자화">
              <div className="ao-surface-panel-header">
                <h2>색상 설정</h2>
                <Palette size={15} aria-hidden="true" />
              </div>

              <div className="flex items-center gap-2">
                <button
                  type="button"
                  className={`tb-btn${colorMode === "original" ? " tb-btn-primary" : ""}`}
                  onClick={() => setColorMode("original")}
                >
                  기본 팔레트
                </button>
                <button
                  type="button"
                  className={`tb-btn${colorMode === "custom" ? " tb-btn-primary" : ""}`}
                  onClick={() => setColorMode("custom")}
                >
                  사용자 지정 색상
                </button>
              </div>

              {colorMode === "custom" ? (
                <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                  <ColorRow
                    label="피부색"
                    target="skin"
                    value={colors.skin.value}
                    onChange={(option) => setColors((prev) => ({ ...prev, skin: option }))}
                  />
                  <ColorRow
                    label="머리색"
                    target="hair"
                    value={colors.hair.value}
                    onChange={(option) => setColors((prev) => ({ ...prev, hair: option }))}
                  />
                  <ColorRow
                    label="상의 색상"
                    target="shirt"
                    value={colors.shirt.value}
                    onChange={(option) => setColors((prev) => ({ ...prev, shirt: option }))}
                  />
                </div>
              ) : (
                <p className="m-0 text-[13px] text-[var(--ui-text-secondary)]">
                  선택한 아바타({selected.kind})의 기본 색상 구성을 사용합니다.
                </p>
              )}
            </section>

            <section className="ao-surface-panel flex flex-col gap-4" aria-label="스타일 및 장비 커스터마이저">
              <div className="ao-surface-panel-header">
                <h2>스타일 · 장비 커스터마이저</h2>
                <button
                  type="button"
                  className="tb-btn"
                  onClick={randomizeWardrobeStyle}
                >
                  <Shuffle size={13} aria-hidden="true" />
                  <span>랜덤 스타일</span>
                </button>
              </div>

              <div className="flex flex-col gap-3">
                <div>
                  <div className="text-[12px] font-medium text-[var(--ui-text-secondary)] mb-1.5">
                    헤어스타일
                  </div>
                  <div className="ao-wardrobe-pill-grid" role="group" aria-label="헤어스타일 선택">
                    <button
                      type="button"
                      className="ao-wardrobe-pill"
                      aria-pressed={hairStyle === ""}
                      onClick={() => setHairStyle("")}
                    >
                      기본 헤어
                    </button>
                    {CHARACTER_HAIR_OPTIONS.map((opt) => (
                      <button
                        key={opt.id}
                        type="button"
                        className="ao-wardrobe-pill"
                        aria-pressed={hairStyle === opt.id}
                        onClick={() => setHairStyle(opt.id)}
                      >
                        {opt.label}
                      </button>
                    ))}
                  </div>
                </div>

                <div>
                  <div className="text-[12px] font-medium text-[var(--ui-text-secondary)] mb-1.5">
                    의상 실루엣
                  </div>
                  <div className="ao-wardrobe-pill-grid" role="group" aria-label="의상 실루엣 선택">
                    <button
                      type="button"
                      className="ao-wardrobe-pill"
                      aria-pressed={outfitStyle === ""}
                      onClick={() => setOutfitStyle("")}
                    >
                      기본 의상
                    </button>
                    {CHARACTER_OUTFIT_OPTIONS.map((opt) => (
                      <button
                        key={opt.id}
                        type="button"
                        className="ao-wardrobe-pill"
                        aria-pressed={outfitStyle === opt.id}
                        onClick={() => setOutfitStyle(opt.id)}
                      >
                        {opt.label}
                      </button>
                    ))}
                  </div>
                </div>

                <div>
                  <div className="text-[12px] font-medium text-[var(--ui-text-secondary)] mb-1.5">
                    액세서리 · 장비
                  </div>
                  <div className="ao-wardrobe-pill-grid" role="group" aria-label="액세서리 및 장비 선택">
                    {CHARACTER_GEAR_OPTIONS.map((opt) => (
                      <button
                        key={opt.id}
                        type="button"
                        className="ao-wardrobe-pill"
                        aria-pressed={gear === opt.id}
                        onClick={() => setGear(opt.id)}
                      >
                        {opt.label}
                      </button>
                    ))}
                  </div>
                </div>
              </div>
            </section>
          </div>
        </div>
      </div>
    </AppTopChrome>
  );
}

function ColorRow({
  label,
  target,
  value,
  onChange,
}: {
  label: string;
  target: ColorTarget;
  value: string;
  onChange: (option: ColorOption) => void;
}) {
  return (
    <div>
      <div className="text-[12px] font-medium text-[var(--ui-text-secondary)] mb-1.5">{label}</div>
      <div className="flex flex-wrap gap-2">
        {COLOR_OPTIONS[target].map((option) => {
          const selected = option.value === value;
          return (
            <button
              key={option.value}
              type="button"
              aria-label={`${label} ${option.label}`}
              aria-pressed={selected}
              onClick={() => onChange(option)}
              style={{
                width: 24,
                height: 24,
                borderRadius: 6,
                border: selected
                  ? "2px solid var(--accent-primary)"
                  : "1px solid var(--ui-border-strong)",
                background: option.value,
                cursor: "pointer",
              }}
            />
          );
        })}
      </div>
    </div>
  );
}

function safeReturnTo(raw: string | null): string {
  if (!raw) return "/workspaces";
  if (!raw.startsWith("/") || raw.startsWith("//")) return "/workspaces";
  return raw;
}
