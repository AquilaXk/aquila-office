"use client";

import { useEffect, useState, useSyncExternalStore } from "react";
import { Minus, Pin, Square, X } from "lucide-react";
import { isDesktop } from "@/lib/desktop/env";

const emptySubscribe = () => () => {};
const DESKTOP_HEADER_HEIGHT_CSS = "36px";

export default function DesktopWindowHeader({
  title = "Aquila Office",
}: {
  title?: string;
}) {
  const isClient = useSyncExternalStore(
    emptySubscribe,
    () => true,
    () => false,
  );
  const [isMaximized, setIsMaximized] = useState(false);
  const [isPinned, setIsPinned] = useState(false);

  const desktopActive = isClient && isDesktop();

  useEffect(() => {
    if (!desktopActive) {
      document.documentElement.style.removeProperty("--desktop-header-height");
      return;
    }

    document.documentElement.style.setProperty(
      "--desktop-header-height",
      DESKTOP_HEADER_HEIGHT_CSS,
    );

    let cancelled = false;
    let unlistenResize: (() => void) | null = null;

    void (async () => {
      try {
        const { getCurrentWindow } = await import("@tauri-apps/api/window");
        const appWindow = getCurrentWindow();
        const syncMaximized = async () => {
          const maximized = await appWindow.isMaximized();
          if (!cancelled) {
            setIsMaximized(maximized);
          }
        };
        await syncMaximized();
        const unlisten = await appWindow.onResized(() => {
          void syncMaximized();
        });
        if (cancelled) {
          unlisten();
          return;
        }
        unlistenResize = unlisten;
      } catch {
        // 테스트 또는 브라우저 fallback
      }
    })();

    return () => {
      cancelled = true;
      document.documentElement.style.removeProperty("--desktop-header-height");
      if (unlistenResize) {
        unlistenResize();
      }
    };
  }, [desktopActive]);

  if (!desktopActive) {
    return null;
  }

  const nav =
    typeof navigator !== "undefined"
      ? (navigator as unknown as { userAgentData?: { platform?: string } })
      : null;
  const platform =
    nav?.userAgentData?.platform ||
    (typeof navigator !== "undefined"
      ? navigator.platform || navigator.userAgent
      : "");
  const isMac = /Mac/i.test(platform);

  const handleTogglePin = async () => {
    try {
      const { getCurrentWindow } = await import("@tauri-apps/api/window");
      const appWindow = getCurrentWindow();
      const next = !isPinned;
      await appWindow.setAlwaysOnTop(next);
      setIsPinned(next);
    } catch {
      // 브라우저 또는 테스트 환경 fallback
    }
  };

  const handleMinimize = async () => {
    try {
      const { getCurrentWindow } = await import("@tauri-apps/api/window");
      await getCurrentWindow().minimize();
    } catch {
      // safe fallback
    }
  };

  const handleToggleMaximize = async () => {
    try {
      const { getCurrentWindow } = await import("@tauri-apps/api/window");
      const appWindow = getCurrentWindow();
      await appWindow.toggleMaximize();
      setIsMaximized(await appWindow.isMaximized());
    } catch {
      // safe fallback
    }
  };

  const handleClose = async () => {
    try {
      const { getCurrentWindow } = await import("@tauri-apps/api/window");
      await getCurrentWindow().close();
    } catch {
      // safe fallback
    }
  };

  return (
    <header
      data-tauri-drag-region
      className={`desktop-window-header select-none flex items-center justify-between h-9 px-3 text-xs font-medium z-50 ${
        isMac ? "desktop-titlebar-macos" : ""
      }`}
      style={{
        background: "var(--ui-surface)",
        color: "var(--ui-text-secondary)",
        borderBottom: "1px solid var(--ui-border-subtle)",
      }}
    >
      <div data-tauri-drag-region className="flex items-center gap-2">
        <span className="font-semibold" style={{ color: "var(--ui-text-strong)" }}>
          {title}
        </span>
      </div>

      <div
        className="desktop-window-controls flex items-center -mr-1"
        style={{ WebkitAppRegion: "no-drag" } as React.CSSProperties}
      >
        <button
          type="button"
          onClick={handleTogglePin}
          className="px-2 h-8 flex items-center gap-1 transition-colors"
          style={{
            color: isPinned ? "var(--accent-primary)" : "var(--ui-text-secondary)",
          }}
          aria-label={isPinned ? "Pinned" : "Pin window"}
          title={isPinned ? "창 고정 해제" : "항상 위에 고정"}
        >
          <Pin size={12} />
          <span>{isPinned ? "고정됨" : "창 고정"}</span>
        </button>
        {!isMac && (
          <>
            <button
              type="button"
              onClick={handleMinimize}
              className="w-8 h-8 flex items-center justify-center transition-colors"
              aria-label="Minimize"
              title="최소화"
            >
              <Minus size={12} />
            </button>
            <button
              type="button"
              onClick={handleToggleMaximize}
              className="w-8 h-8 flex items-center justify-center transition-colors"
              aria-label={isMaximized ? "Restore" : "Maximize"}
              title={isMaximized ? "복원" : "최대화"}
            >
              <Square size={10} />
            </button>
            <button
              type="button"
              onClick={handleClose}
              className="w-8 h-8 flex items-center justify-center transition-colors"
              aria-label="Close"
              title="닫기"
            >
              <X size={12} />
            </button>
          </>
        )}
      </div>
    </header>
  );
}
