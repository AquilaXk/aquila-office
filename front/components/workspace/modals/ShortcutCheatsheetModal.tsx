"use client";

import { useRef } from "react";
import { Keyboard, X } from "lucide-react";
import { useDialogA11y } from "@/components/ui/useDialogA11y";
import {
  formatCommandShortcut,
  formatViewIndexShortcut,
  type ShortcutStyle,
} from "@/lib/workspace-shortcuts";

export type ShortcutCheatsheetModalProps = {
  open: boolean;
  shortcutStyle: ShortcutStyle;
  onClose: () => void;
};

export function ShortcutCheatsheetModal({
  open,
  shortcutStyle,
  onClose,
}: ShortcutCheatsheetModalProps) {
  const dialogRef = useRef<HTMLElement | null>(null);
  const { onBackdropPointerDown, onBackdropClick } = useDialogA11y(open, dialogRef, onClose);

  if (!open) {
    return null;
  }

  return (
    <div
      className="workspace-modal-backdrop"
      role="presentation"
      onPointerDown={(event) => onBackdropPointerDown(event.target === event.currentTarget)}
      onClick={(event) => onBackdropClick(event.target === event.currentTarget)}
    >
      <section
        ref={dialogRef}
        className="workspace-modal modal-dialog ao-shortcut-dialog"
        role="dialog"
        aria-modal="true"
        aria-label="키보드 단축키 안내"
        tabIndex={-1}
      >
        <header className="workspace-modal-header">
          <div className="flex items-center gap-2">
            <Keyboard size={16} aria-hidden="true" />
            <div>
              <span className="workspace-detail-label">도움말</span>
              <h2>키보드 단축키</h2>
            </div>
          </div>
          <button
            className="icon-button workspace-modal-close"
            type="button"
            onClick={onClose}
            aria-label="닫기"
          >
            <X aria-hidden="true" />
          </button>
        </header>

        <div className="workspace-modal-body">
          <div className="ao-shortcut-grid">
            <section className="ao-shortcut-group" aria-label="전역 탐색">
              <h3 className="ao-shortcut-group-title">전역 탐색</h3>
              <div className="ao-shortcut-row">
                <span>명령 팔레트 열기</span>
                <kbd className="ao-shortcut-kbd">{formatCommandShortcut(shortcutStyle, "K")}</kbd>
              </div>
              <div className="ao-shortcut-row">
                <span>오피스 스테이지</span>
                <kbd className="ao-shortcut-kbd">{formatViewIndexShortcut(shortcutStyle, 1)}</kbd>
              </div>
              <div className="ao-shortcut-row">
                <span>작업 보드</span>
                <kbd className="ao-shortcut-kbd">{formatViewIndexShortcut(shortcutStyle, 2)}</kbd>
              </div>
              <div className="ao-shortcut-row">
                <span>승인 대기함</span>
                <kbd className="ao-shortcut-kbd">{formatViewIndexShortcut(shortcutStyle, 3)}</kbd>
              </div>
              <div className="ao-shortcut-row">
                <span>오케스트레이션</span>
                <kbd className="ao-shortcut-kbd">{formatViewIndexShortcut(shortcutStyle, 4)}</kbd>
              </div>
              <div className="ao-shortcut-row">
                <span>산출물 및 Git Diff</span>
                <kbd className="ao-shortcut-kbd">{formatViewIndexShortcut(shortcutStyle, 5)}</kbd>
              </div>
            </section>

            <section className="ao-shortcut-group" aria-label="인스펙터 및 대화">
              <h3 className="ao-shortcut-group-title">인스펙터 및 대화</h3>
              <div className="ao-shortcut-row">
                <span>단축키 안내 열기</span>
                <kbd className="ao-shortcut-kbd">?</kbd>
              </div>
              <div className="ao-shortcut-row">
                <span>모달 · 인스펙터 닫기</span>
                <kbd className="ao-shortcut-kbd">Esc</kbd>
              </div>
              <div className="ao-shortcut-row">
                <span>메시지 전송</span>
                <kbd className="ao-shortcut-kbd">Enter</kbd>
              </div>
              <div className="ao-shortcut-row">
                <span>대화 줄바꿈</span>
                <kbd className="ao-shortcut-kbd">Shift+Enter</kbd>
              </div>
              <div className="ao-shortcut-row">
                <span>최근 프롬프트 불러오기</span>
                <kbd className="ao-shortcut-kbd">↑ / ↓</kbd>
              </div>
              <div className="ao-shortcut-row">
                <span>검색 필터 포커스</span>
                <kbd className="ao-shortcut-kbd">{formatCommandShortcut(shortcutStyle, "K")}</kbd>
              </div>
            </section>
          </div>
        </div>
      </section>
    </div>
  );
}
