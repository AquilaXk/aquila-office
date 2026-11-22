/*
 * 데스크톱·브라우저 플랫폼 인지 단축키 및 인스펙터 너비 클램프 유틸리티.
 */

export type ShortcutStyle = "desktop-mac" | "desktop" | "browser-mac" | "browser";

export const MIN_INSPECTOR_WIDTH = 280;
export const MAX_INSPECTOR_WIDTH = 520;
export const DEFAULT_INSPECTOR_WIDTH = 320;
const RESERVED_MAIN_WIDTH = 520;

export function isEditableEventTarget(target: EventTarget | null): boolean {
  if (!target || typeof target !== "object") {
    return false;
  }
  const el = target as {
    tagName?: string;
    isContentEditable?: boolean;
    closest?: (selector: string) => unknown;
  };
  if (el.isContentEditable) {
    return true;
  }
  const tag = typeof el.tagName === "string" ? el.tagName.toUpperCase() : "";
  if (tag === "INPUT" || tag === "TEXTAREA" || tag === "SELECT") {
    return true;
  }
  if (typeof el.closest === "function") {
    return el.closest('input, textarea, select, [contenteditable], [role="dialog"]') !== null;
  }
  return false;
}

export function resolveShortcutStyle(
  desktopShell: boolean,
  userAgent = typeof navigator === "undefined" ? "" : navigator.userAgent,
): ShortcutStyle {
  const mac = /Mac OS X|Macintosh/i.test(userAgent);
  if (desktopShell) {
    return mac ? "desktop-mac" : "desktop";
  }
  return mac ? "browser-mac" : "browser";
}

export function formatCommandShortcut(style: ShortcutStyle, key: string): string {
  const upper = key.toUpperCase();
  return style === "desktop-mac" || style === "browser-mac" ? `⌘${upper}` : `Ctrl+${upper}`;
}

export function formatViewIndexShortcut(style: ShortcutStyle, index1Based: number): string {
  switch (style) {
    case "desktop-mac":
      return `⌘${index1Based}`;
    case "desktop":
      return `Ctrl+${index1Based}`;
    case "browser-mac":
      return `⌥${index1Based}`;
    default:
      return `Alt+${index1Based}`;
  }
}

export function matchViewSwitchShortcut(
  style: ShortcutStyle,
  event: Pick<KeyboardEvent, "key" | "metaKey" | "ctrlKey" | "altKey" | "shiftKey">,
  viewCount: number,
): number | null {
  if (event.shiftKey) {
    return null;
  }
  const modifierMatch =
    style === "desktop-mac"
      ? event.metaKey && !event.ctrlKey && !event.altKey
      : style === "desktop"
        ? event.ctrlKey && !event.metaKey && !event.altKey
        : event.altKey && !event.metaKey && !event.ctrlKey;

  if (!modifierMatch) {
    return null;
  }
  if (!/^[1-9]$/.test(event.key)) {
    return null;
  }
  const index = Number.parseInt(event.key, 10) - 1;
  return index >= 0 && index < viewCount ? index : null;
}

export function clampInspectorWidth(width: number, viewportWidth?: number): number {
  if (!Number.isFinite(width)) {
    return DEFAULT_INSPECTOR_WIDTH;
  }
  const raw = Math.round(width);
  const maxAllowed =
    typeof viewportWidth === "number" && Number.isFinite(viewportWidth) && viewportWidth >= 600
      ? Math.min(MAX_INSPECTOR_WIDTH, Math.max(MIN_INSPECTOR_WIDTH, viewportWidth - RESERVED_MAIN_WIDTH))
      : MAX_INSPECTOR_WIDTH;
  return Math.min(maxAllowed, Math.max(MIN_INSPECTOR_WIDTH, raw));
}
