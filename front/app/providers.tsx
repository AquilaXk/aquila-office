"use client";

import { GoogleOAuthProvider } from "@react-oauth/google";
import { useEffect, type ReactNode } from "react";
import {
  applyTheme,
  normalizeThemeMode,
  readStoredTheme,
  THEME_CHANGE_EVENT,
  THEME_STORAGE_KEY,
  type ThemeMode,
} from "@/lib/theme";

const GOOGLE_CLIENT_ID = process.env.NEXT_PUBLIC_GOOGLE_CLIENT_ID ?? "";

export default function Providers({ children }: { children: ReactNode }) {
  useEffect(() => {
    applyTheme(readStoredTheme(), false);

    const handleStorage = (event: StorageEvent) => {
      if (event.key === null || event.key === THEME_STORAGE_KEY) {
        applyTheme(readStoredTheme(), false);
      }
    };

    const handleThemeEvent = (event: Event) => {
      const customEvent = event as CustomEvent<ThemeMode>;
      applyTheme(normalizeThemeMode(customEvent.detail), false);
    };

    window.addEventListener("storage", handleStorage);
    window.addEventListener(THEME_CHANGE_EVENT, handleThemeEvent);
    return () => {
      window.removeEventListener("storage", handleStorage);
      window.removeEventListener(THEME_CHANGE_EVENT, handleThemeEvent);
    };
  }, []);

  return (
    <GoogleOAuthProvider clientId={GOOGLE_CLIENT_ID}>
      {children}
    </GoogleOAuthProvider>
  );
}
