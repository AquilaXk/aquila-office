"use client";

import {
  Check,
  ChevronDown,
  Crosshair,
  GitBranch,
  Palette,
  Pause,
  Play,
} from "lucide-react";
import {
  useCallback,
  useId,
  useRef,
  useState,
  useSyncExternalStore,
  type CSSProperties,
  type KeyboardEvent as ReactKeyboardEvent,
  type ToggleEvent as ReactToggleEvent,
} from "react";
import type { OfficeThemeId } from "@/lib/pixel-office/pixtuoid-sprites";

export type StageThemeOption = {
  id: OfficeThemeId;
  label: string;
};

export type StageDisplayToolbarProps = {
  officeTheme: OfficeThemeId;
  themeOptions: ReadonlyArray<StageThemeOption>;
  showCollabLinks: boolean;
  motionPaused: boolean;
  hasPlayerPosition?: boolean;
  onSelectTheme: (theme: OfficeThemeId) => void;
  onToggleCollabLinks: () => void;
  onToggleMotionPaused: () => void;
  onFocusMyPosition?: () => void;
};

const noopSubscribe = () => () => {};

function getFocusgroupSupportSnapshot(): boolean {
  return typeof HTMLElement !== "undefined" && "focusGroup" in HTMLElement.prototype;
}

function getServerFocusgroupSupportSnapshot(): boolean {
  return false;
}

/**
 * 2D 오피스 스테이지 상단에 뜨는 플로팅 표시 제어 툴바다.
 * Native HTML Popover API(`popover="auto"`) 테마 메뉴와 `focusgroup` roving tabindex 폴백을 함께 지원한다.
 */
export function StageDisplayToolbar({
  officeTheme,
  themeOptions,
  showCollabLinks,
  motionPaused,
  hasPlayerPosition = false,
  onSelectTheme,
  onToggleCollabLinks,
  onToggleMotionPaused,
  onFocusMyPosition,
}: StageDisplayToolbarProps) {
  const reactId = useId();
  const safeId = reactId.replace(/[^a-zA-Z0-9_-]/g, "");
  const popoverId = `stage-theme-popover-${safeId}`;
  const anchorName = `--stage-theme-anchor-${safeId}`;

  const toolbarRef = useRef<HTMLDivElement | null>(null);
  const triggerRef = useRef<HTMLButtonElement | null>(null);
  const popoverRef = useRef<HTMLDivElement | null>(null);

  const [focusIndex, setFocusIndex] = useState(0);
  const [themeMenuOpen, setThemeMenuOpen] = useState(false);

  const supportsFocusgroup = useSyncExternalStore(
    noopSubscribe,
    getFocusgroupSupportSnapshot,
    getServerFocusgroupSupportSnapshot,
  );

  const activeThemeLabel =
    themeOptions.find((option) => option.id === officeTheme)?.label ?? "자동";

  const handleToolbarKeyDown = useCallback(
    (event: ReactKeyboardEvent<HTMLDivElement>) => {
      if (supportsFocusgroup) return;
      const root = toolbarRef.current;
      if (!root) return;
      const buttons = Array.from(
        root.querySelectorAll<HTMLButtonElement>(".ao-toolbar-btn:not([disabled])"),
      );
      if (buttons.length === 0) return;

      const currentIdx = buttons.findIndex((btn) => btn === document.activeElement);
      let nextIdx = -1;
      if (event.key === "ArrowRight") {
        nextIdx = currentIdx < 0 ? 0 : (currentIdx + 1) % buttons.length;
      } else if (event.key === "ArrowLeft") {
        nextIdx = currentIdx <= 0 ? buttons.length - 1 : currentIdx - 1;
      } else if (event.key === "Home") {
        nextIdx = 0;
      } else if (event.key === "End") {
        nextIdx = buttons.length - 1;
      }

      if (nextIdx >= 0) {
        event.preventDefault();
        setFocusIndex(nextIdx);
        buttons[nextIdx]?.focus();
      }
    },
    [supportsFocusgroup],
  );

  const handlePopoverToggle = useCallback((event: ReactToggleEvent<HTMLDivElement>) => {
    setThemeMenuOpen(event.newState === "open");
  }, []);

  const handlePopoverKeyDown = useCallback((event: ReactKeyboardEvent<HTMLDivElement>) => {
    const root = popoverRef.current;
    if (!root) return;
    const items = Array.from(
      root.querySelectorAll<HTMLButtonElement>(".ao-stage-theme-option:not([disabled])"),
    );
    if (items.length === 0) return;
    const currentIdx = items.findIndex((item) => item === document.activeElement);
    if (event.key === "ArrowDown") {
      event.preventDefault();
      items[(currentIdx + 1) % items.length]?.focus();
    } else if (event.key === "ArrowUp") {
      event.preventDefault();
      items[currentIdx <= 0 ? items.length - 1 : currentIdx - 1]?.focus();
    } else if (event.key === "Home") {
      event.preventDefault();
      items[0]?.focus();
    } else if (event.key === "End") {
      event.preventDefault();
      items[items.length - 1]?.focus();
    }
  }, []);

  const handleChooseTheme = useCallback(
    (nextTheme: OfficeThemeId) => {
      onSelectTheme(nextTheme);
      try {
        popoverRef.current?.hidePopover?.();
      } catch {
        // Ignore in environments without native popover support
      }
      triggerRef.current?.focus();
    },
    [onSelectTheme],
  );

  const triggerStyle = { anchorName } as CSSProperties;
  const popoverStyle = { positionAnchor: anchorName } as CSSProperties;

  const buttonTabIndex = (idx: number) =>
    supportsFocusgroup ? 0 : focusIndex === idx ? 0 : -1;

  return (
    <div
      ref={toolbarRef}
      role="toolbar"
      aria-label="오피스 스테이지 표시 도구"
      className="ao-stage-toolbar ao-stage-island"
      {...({ focusgroup: "toolbar wrap" } as Record<string, string>)}
      onKeyDown={handleToolbarKeyDown}
    >
      <div role="group" aria-label="오피스 테마" className="ao-toolbar-group">
        <button
          ref={triggerRef}
          type="button"
          className="ao-toolbar-btn"
          style={triggerStyle}
          popoverTarget={popoverId}
          aria-haspopup="menu"
          aria-expanded={themeMenuOpen}
          aria-label={`오피스 테마: ${activeThemeLabel}`}
          title="오피스 스테이지 테마 변경"
          tabIndex={buttonTabIndex(0)}
          onClick={() => setFocusIndex(0)}
        >
          <Palette size={13} aria-hidden="true" />
          <span>{activeThemeLabel}</span>
          <ChevronDown size={12} aria-hidden="true" />
        </button>

        <div
          ref={popoverRef}
          id={popoverId}
          popover="auto"
          role="menu"
          aria-label="오피스 스테이지 테마 선택"
          className="ao-stage-theme-popover"
          style={popoverStyle}
          onToggle={handlePopoverToggle}
          onKeyDown={handlePopoverKeyDown}
        >
          {themeOptions.map((option) => {
            const selected = officeTheme === option.id;
            return (
              <button
                key={option.id}
                type="button"
                role="menuitemradio"
                aria-checked={selected}
                className="ao-stage-theme-option"
                onClick={() => handleChooseTheme(option.id)}
              >
                <span>{option.label}</span>
                {selected ? <Check size={13} aria-hidden="true" /> : null}
              </button>
            );
          })}
        </div>
      </div>

      <span role="separator" aria-orientation="vertical" className="ao-toolbar-sep" />

      <div role="group" aria-label="시각 효과" className="ao-toolbar-group">
        <button
          type="button"
          className="ao-toolbar-btn"
          aria-pressed={showCollabLinks}
          tabIndex={buttonTabIndex(1)}
          onClick={() => {
            setFocusIndex(1);
            onToggleCollabLinks();
          }}
        >
          <GitBranch size={13} aria-hidden="true" />
          <span>협업 연결선</span>
        </button>

        <button
          type="button"
          className="ao-toolbar-btn"
          aria-pressed={motionPaused}
          tabIndex={buttonTabIndex(2)}
          onClick={() => {
            setFocusIndex(2);
            onToggleMotionPaused();
          }}
        >
          {motionPaused ? (
            <Play size={13} aria-hidden="true" />
          ) : (
            <Pause size={13} aria-hidden="true" />
          )}
          <span>{motionPaused ? "모션 재생" : "모션 일시정지"}</span>
        </button>
      </div>

      {onFocusMyPosition ? (
        <>
          <span role="separator" aria-orientation="vertical" className="ao-toolbar-sep" />
          <button
            type="button"
            className="ao-toolbar-btn"
            disabled={!hasPlayerPosition}
            aria-label="내 위치로 카메라 이동"
            title="내 위치로 카메라 이동"
            tabIndex={buttonTabIndex(3)}
            onClick={() => {
              setFocusIndex(3);
              onFocusMyPosition();
            }}
          >
            <Crosshair size={13} aria-hidden="true" />
            <span>내 위치</span>
          </button>
        </>
      ) : null}
    </div>
  );
}
