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
