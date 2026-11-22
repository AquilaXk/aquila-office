"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import {
  clampInspectorWidth,
  clampSidebarWidth,
  DEFAULT_INSPECTOR_WIDTH,
  DEFAULT_SIDEBAR_WIDTH,
  isEditableEventTarget,
  resolveShortcutStyle,
  type ShortcutStyle,
} from "@/lib/workspace-shortcuts";

const SIDEBAR_COLLAPSED_KEY = "ao.sidebar.collapsed";
const SIDEBAR_WIDTH_KEY = "ao.sidebar.width";
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

function readStoredSidebarWidth(): number {
  if (typeof window === "undefined") {
    return DEFAULT_SIDEBAR_WIDTH;
  }
  try {
    const raw = window.localStorage.getItem(SIDEBAR_WIDTH_KEY);
    if (!raw) {
      return DEFAULT_SIDEBAR_WIDTH;
    }
    return clampSidebarWidth(Number(raw), window.innerWidth);
  } catch {
    return DEFAULT_SIDEBAR_WIDTH;
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
  sidebarWidth: number;
  setSidebarWidth: (nextWidth: number) => void;
  resetSidebarWidth: () => void;
  inspectorWidth: number;
  setInspectorWidth: (nextWidth: number) => void;
  resetInspectorWidth: () => void;
} {
  const [sidebarCollapsed, setSidebarCollapsed] = useState<boolean>(readStoredSidebarCollapsed);
  const [sidebarWidth, setSidebarWidthState] = useState<number>(readStoredSidebarWidth);
  const [inspectorWidth, setInspectorWidthState] = useState<number>(readStoredInspectorWidth);
  const shortcutStyle = useMemo(() => resolveShortcutStyle(desktopShell), [desktopShell]);

  const toggleSidebar = useCallback(() => {
    setSidebarCollapsed((prev) => !prev);
  }, []);

  const setSidebarWidth = useCallback((nextWidth: number) => {
    const vw = typeof window !== "undefined" ? window.innerWidth : undefined;
    setSidebarWidthState(clampSidebarWidth(nextWidth, vw));
  }, []);

  const resetSidebarWidth = useCallback(() => {
    const vw = typeof window !== "undefined" ? window.innerWidth : undefined;
    setSidebarWidthState(clampSidebarWidth(DEFAULT_SIDEBAR_WIDTH, vw));
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
      window.localStorage.setItem(SIDEBAR_WIDTH_KEY, String(sidebarWidth));
    } catch {
      // 저장소 접근 제한 시 세션 상태만 유지한다.
    }
  }, [sidebarWidth]);

  useEffect(() => {
    try {
      window.localStorage.setItem(INSPECTOR_WIDTH_KEY, String(inspectorWidth));
    } catch {
      // 저장소 접근 제한 시 세션 상태만 유지한다.
    }
  }, [inspectorWidth]);

  useEffect(() => {
    const onResize = () => {
      setSidebarWidthState((current) => clampSidebarWidth(current, window.innerWidth));
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
    sidebarWidth,
    setSidebarWidth,
    resetSidebarWidth,
    inspectorWidth,
    setInspectorWidth,
    resetInspectorWidth,
  };
}
