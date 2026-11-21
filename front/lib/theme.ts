export const gothicThemeTokens = {
  page: "var(--gothic-page)",
  surface: "var(--gothic-surface)",
  surfaceRaised: "var(--gothic-surface-raised)",
  iron: "var(--gothic-iron)",
  line: "var(--gothic-line)",
  lineStrong: "var(--gothic-line-strong)",
  brass: "var(--gothic-brass)",
  parchment: "var(--gothic-parchment)",
  muted: "var(--gothic-muted)",
  red: "var(--gothic-red)",
  radiusControl: "8px",
  radiusPanel: "8px",
} as const;

export type ThemeTokenName = keyof typeof gothicThemeTokens;

export function themeToken(name: ThemeTokenName): string {
  return gothicThemeTokens[name];
}

export type ThemeMode = "dark" | "light";

export const THEME_STORAGE_KEY = "aquila.theme";
export const THEME_CHANGE_EVENT = "aquila:theme-change";

export function normalizeThemeMode(value: unknown): ThemeMode {
  return value === "light" ? "light" : "dark";
}

export function readStoredTheme(storage?: Pick<Storage, "getItem"> | null): ThemeMode {
  const targetStorage =
    storage !== undefined
      ? storage
      : typeof window !== "undefined"
        ? safeBrowserStorage()
        : null;

  if (!targetStorage) {
    return "dark";
  }

  try {
    return normalizeThemeMode(targetStorage.getItem(THEME_STORAGE_KEY));
  } catch {
    return "dark";
  }
}

export function applyTheme(mode: ThemeMode, persist = true): ThemeMode {
  const normalized = normalizeThemeMode(mode);

  if (typeof document !== "undefined") {
    document.documentElement.dataset.theme = normalized;
  }

  if (persist && typeof window !== "undefined") {
    const targetStorage = safeBrowserStorage();
    try {
      targetStorage?.setItem(THEME_STORAGE_KEY, normalized);
    } catch {
      // 저장소 접근 제한 환경에서는 DOM 속성만 갱신한다.
    }
    window.dispatchEvent(new CustomEvent<ThemeMode>(THEME_CHANGE_EVENT, { detail: normalized }));
  }

  return normalized;
}

export function toggleTheme(current?: ThemeMode): ThemeMode {
  const baseline =
    current ??
    (typeof document !== "undefined" && document.documentElement.dataset.theme === "light"
      ? "light"
      : readStoredTheme());
  const next: ThemeMode = baseline === "light" ? "dark" : "light";
  return applyTheme(next, true);
}

function safeBrowserStorage(): Storage | null {
  try {
    return window.localStorage;
  } catch {
    return null;
  }
}
