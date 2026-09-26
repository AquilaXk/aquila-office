/**
 * Detects whether the current runtime environment is inside the Tauri desktop shell.
 */
export function isDesktop(): boolean {
  if (typeof window === "undefined") {
    return false;
  }

  const win = window as unknown as {
    __TAURI_INTERNALS__?: unknown;
    __TAURI__?: unknown;
  };

  return Boolean(win.__TAURI_INTERNALS__ || win.__TAURI__);
}
