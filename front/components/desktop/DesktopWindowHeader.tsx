"use client";

import { useState, useSyncExternalStore } from "react";
import { Minus, Square, X } from "lucide-react";
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

  if (!isClient || !isDesktop()) {
    return null;
  }

  const nav = typeof navigator !== "undefined" ? (navigator as unknown as { userAgentData?: { platform?: string } }) : null;
  const platform = nav?.userAgentData?.platform || (typeof navigator !== "undefined" ? navigator.platform || navigator.userAgent : "");
  const isMac = /Mac/i.test(platform);

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
      className={`desktop-window-header select-none flex items-center justify-between h-9 bg-[var(--surface-card,#212529)] text-[var(--ink-secondary,#E5E7EB)] border-b border-[var(--border-subtle,#343A40)] px-3 text-xs font-medium z-50 ${
        isMac ? "desktop-titlebar-macos" : ""
      }`}
    >
      <div data-tauri-drag-region className="flex items-center gap-2">
        <span className="font-semibold text-[var(--ink-primary,#F8F9FA)]">{title}</span>
      </div>

      {!isMac && (
        <div className="flex items-center -mr-1">
          <button
            type="button"
            onClick={handleMinimize}
            className="w-8 h-8 flex items-center justify-center hover:bg-[var(--surface-subtle,#2A2E33)] hover:text-white transition-colors"
            aria-label="Minimize"
          >
            <Minus size={12} />
          </button>
          <button
            type="button"
            onClick={handleToggleMaximize}
            className="w-8 h-8 flex items-center justify-center hover:bg-[var(--surface-subtle,#2A2E33)] hover:text-white transition-colors"
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
        </div>
      )}
    </header>
  );
}
