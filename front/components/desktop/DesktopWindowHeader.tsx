"use client";

import { useState, useSyncExternalStore } from "react";
import { Minus, Pin, Square, X } from "lucide-react";
import { isDesktop } from "@/lib/desktop/env";

const emptySubscribe = () => () => {};

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

  if (!isClient || !isDesktop()) {
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
      // safe fallback
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
      className={`desktop-window-header select-none flex items-center justify-between h-9 bg-[var(--surface-panel,#18191d)] text-[var(--text-secondary,#b2bbc5)] border-b border-[var(--border-subtle,rgba(230,234,240,0.08))] px-3 text-xs font-medium z-50 ${
        isMac ? "desktop-titlebar-macos" : ""
      }`}
    >
      <div data-tauri-drag-region className="flex items-center gap-2">
        <span className="font-semibold text-[var(--text-primary,#f8f9fc)]">
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
          className="px-2 h-8 flex items-center gap-1 hover:bg-[var(--surface-2,#212226)] hover:text-white transition-colors"
          aria-label={isPinned ? "Pinned" : "Pin window"}
        >
          <Pin size={12} />
          <span>{isPinned ? "Pinned" : "Pin window"}</span>
        </button>
        {!isMac && (
          <>
            <button
              type="button"
              onClick={handleMinimize}
              className="w-8 h-8 flex items-center justify-center hover:bg-[var(--surface-2,#212226)] hover:text-white transition-colors"
              aria-label="Minimize"
            >
              <Minus size={12} />
            </button>
            <button
              type="button"
              onClick={handleToggleMaximize}
              className="w-8 h-8 flex items-center justify-center hover:bg-[var(--surface-2,#212226)] hover:text-white transition-colors"
              aria-label={isMaximized ? "Restore" : "Maximize"}
            >
              <Square size={10} />
            </button>
            <button
              type="button"
              onClick={handleClose}
              className="w-8 h-8 flex items-center justify-center hover:bg-[#e81123] hover:text-white transition-colors"
              aria-label="Close"
            >
              <X size={12} />
            </button>
          </>
        )}
      </div>
    </header>
  );
}
