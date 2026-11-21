"use client";

import { Building2, LogOut, Moon, Network, Sun } from "lucide-react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useState, type ReactNode } from "react";
import { isAuthenticated, logout } from "@/lib/auth";
import {
  normalizeThemeMode,
  readStoredTheme,
  THEME_CHANGE_EVENT,
  THEME_STORAGE_KEY,
  toggleTheme,
  type ThemeMode,
} from "@/lib/theme";

export type AppTopChromeProps = {
  title: string;
  backHref?: string;
  backLabel?: string;
  actions?: ReactNode;
  showWorkspaceLinks?: boolean;
  children?: ReactNode;
};

export function AppTopChrome({
  title,
  backHref,
  backLabel,
  actions,
  showWorkspaceLinks = true,
  children,
}: AppTopChromeProps) {
  const router = useRouter();
  const [themeMode, setThemeMode] = useState<ThemeMode>("dark");
  const [authed, setAuthed] = useState(false);

  useEffect(() => {
    queueMicrotask(() => {
      setThemeMode(readStoredTheme());
      setAuthed(isAuthenticated());
    });

    const handleThemeEvent = (event: Event) => {
      const custom = event as CustomEvent<ThemeMode>;
      setThemeMode(normalizeThemeMode(custom.detail));
    };
    const handleStorage = (event: StorageEvent) => {
      if (event.key === null || event.key === THEME_STORAGE_KEY) {
        setThemeMode(readStoredTheme());
      }
      setAuthed(isAuthenticated());
    };

    window.addEventListener(THEME_CHANGE_EVENT, handleThemeEvent);
    window.addEventListener("storage", handleStorage);
    return () => {
      window.removeEventListener(THEME_CHANGE_EVENT, handleThemeEvent);
      window.removeEventListener("storage", handleStorage);
    };
  }, []);

  async function handleLogout() {
    await logout();
    setAuthed(false);
    router.replace("/");
  }

  const header = (
    <header className="app-top-chrome" aria-label="애플리케이션 상단 바">
      <div className="app-top-chrome-left">
        <Link className="app-top-chrome-brand" href={authed ? "/workspaces" : "/"}>
          <Building2 size={14} aria-hidden="true" />
          <span>Aquila Office</span>
        </Link>
        <span className="app-top-chrome-sep" aria-hidden="true">
          /
        </span>
        <span className="app-top-chrome-crumb">{title}</span>
        {backHref && backLabel ? (
          <>
            <span className="app-top-chrome-sep" aria-hidden="true">
              ·
            </span>
            <Link className="app-top-chrome-link" href={backHref}>
              {backLabel}
            </Link>
          </>
        ) : null}
      </div>

      <div className="app-top-chrome-right">
        {actions}
        {authed && showWorkspaceLinks ? (
          <>
            <Link className="app-top-chrome-link" href="/workspaces">
              <span>워크스페이스</span>
            </Link>
            <Link className="app-top-chrome-link" href="/workspaces/gateway">
              <Network size={13} aria-hidden="true" />
              <span>게이트웨이</span>
            </Link>
          </>
        ) : null}
        <button
          type="button"
          className="app-top-chrome-btn"
          onClick={() => setThemeMode(toggleTheme(themeMode))}
          aria-label={themeMode === "light" ? "다크 모드로 전환" : "라이트 모드로 전환"}
          title={themeMode === "light" ? "다크 모드로 전환" : "라이트 모드로 전환"}
        >
          {themeMode === "light" ? (
            <Moon size={13} aria-hidden="true" />
          ) : (
            <Sun size={13} aria-hidden="true" />
          )}
          <span>{themeMode === "light" ? "다크" : "라이트"}</span>
        </button>
        {authed ? (
          <button
            type="button"
            className="app-top-chrome-btn"
            onClick={() => void handleLogout()}
            aria-label="로그아웃"
            title="로그아웃"
          >
            <LogOut size={13} aria-hidden="true" />
            <span>로그아웃</span>
          </button>
        ) : null}
      </div>
    </header>
  );

  if (children === undefined) {
    return header;
  }

  return (
    <div className="app-chrome-shell">
      {header}
      <main className="app-top-chrome-main">{children}</main>
    </div>
  );
}

export default AppTopChrome;
