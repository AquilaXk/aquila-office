"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import {
  clampInspectorWidth,
  DEFAULT_INSPECTOR_WIDTH,
  isEditableEventTarget,
  resolveShortcutStyle,
  type ShortcutStyle,
} from "@/lib/workspace-shortcuts";

const SIDEBAR_COLLAPSED_KEY = "ao.sidebar.collapsed";
const INSPECTOR_WIDTH_KEY = "ao.inspector.width";

function readStoredSidebarCollapsed(): boolean {
  if (typeof window === "undefined") {
    return false;
  }
  try {
    return window.localStorage.getItem(SIDEBAR_COLLAPSED_KEY) === "1";
  } catch {
    return false;
  }
}

function readStoredInspectorWidth(): number {
  if (typeof window === "undefined") {
    return DEFAULT_INSPECTOR_WIDTH;
  }
  try {
    const raw = window.localStorage.getItem(INSPECTOR_WIDTH_KEY);
    if (!raw) {
      return DEFAULT_INSPECTOR_WIDTH;
    }
    return clampInspectorWidth(Number(raw), window.innerWidth);
  } catch {
    return DEFAULT_INSPECTOR_WIDTH;
  }
}

export function useWorkspaceLayout({ desktopShell }: { desktopShell: boolean }): {
  shortcutStyle: ShortcutStyle;
  sidebarCollapsed: boolean;
  toggleSidebar: () => void;
  inspectorWidth: number;
  setInspectorWidth: (nextWidth: number) => void;
  resetInspectorWidth: () => void;
} {
  const [sidebarCollapsed, setSidebarCollapsed] = useState<boolean>(readStoredSidebarCollapsed);
  const [inspectorWidth, setInspectorWidthState] = useState<number>(readStoredInspectorWidth);
  const shortcutStyle = useMemo(() => resolveShortcutStyle(desktopShell), [desktopShell]);

  const toggleSidebar = useCallback(() => {
    setSidebarCollapsed((prev) => !prev);
  }, []);

  const setInspectorWidth = useCallback((nextWidth: number) => {
    const vw = typeof window !== "undefined" ? window.innerWidth : undefined;
    setInspectorWidthState(clampInspectorWidth(nextWidth, vw));
  }, []);

  const resetInspectorWidth = useCallback(() => {
    const vw = typeof window !== "undefined" ? window.innerWidth : undefined;
    setInspectorWidthState(clampInspectorWidth(DEFAULT_INSPECTOR_WIDTH, vw));
  }, []);

  useEffect(() => {
    try {
      window.localStorage.setItem(SIDEBAR_COLLAPSED_KEY, sidebarCollapsed ? "1" : "0");
    } catch {
      // 저장소 접근 제한 시 세션 상태만 유지한다.
    }
  }, [sidebarCollapsed]);

  useEffect(() => {
    try {
      window.localStorage.setItem(INSPECTOR_WIDTH_KEY, String(inspectorWidth));
    } catch {
      // 저장소 접근 제한 시 세션 상태만 유지한다.
    }
  }, [inspectorWidth]);

  useEffect(() => {
    const onResize = () => {
      setInspectorWidthState((current) => clampInspectorWidth(current, window.innerWidth));
    };
    window.addEventListener("resize", onResize);
    return () => window.removeEventListener("resize", onResize);
  }, []);

  useEffect(() => {
    const onKeyDown = (event: KeyboardEvent) => {
      if (!(event.metaKey || event.ctrlKey) || event.altKey || event.shiftKey) {
        return;
      }
      if (event.key.toLowerCase() !== "b") {
        return;
      }
      if (isEditableEventTarget(event.target)) {
        return;
      }
      event.preventDefault();
      toggleSidebar();
    };
    window.addEventListener("keydown", onKeyDown);
    return () => window.removeEventListener("keydown", onKeyDown);
  }, [toggleSidebar]);

  return {
    shortcutStyle,
    sidebarCollapsed,
    toggleSidebar,
    inspectorWidth,
    setInspectorWidth,
    resetInspectorWidth,
  };
}
