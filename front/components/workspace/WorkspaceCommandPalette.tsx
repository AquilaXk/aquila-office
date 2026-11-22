/*
 * SPDX-License-Identifier: Apache-2.0
 * Command Palette keyboard/listbox pattern adapted from pacocoursey/cmdk (MIT License).
 */
"use client";

import { useId, useMemo, useRef, useState, type KeyboardEvent } from "react";
import { Search, X } from "lucide-react";
import { useDialogA11y } from "@/components/ui/useDialogA11y";
import {
  MAX_CANDIDATE_LENGTH,
  MAX_QUERY_LENGTH,
  searchBitapItems,
} from "@/lib/bitap-fuzzy-search";

export type CommandPaletteCategory = "navigation" | "agent" | "member" | "action" | "theme";

export interface WorkspaceCommandItem {
  id: string;
  label: string;
  description?: string;
  keywords?: string;
  category: CommandPaletteCategory;
  categoryLabel: string;
  shortcut?: string;
  onSelect: () => void;
}

export interface WorkspaceCommandPaletteProps {
  open: boolean;
  items: readonly WorkspaceCommandItem[];
  onClose: () => void;
}

export default function WorkspaceCommandPalette({
  open,
  items,
  onClose,
}: WorkspaceCommandPaletteProps) {
  if (!open) {
    return null;
  }
  return <WorkspaceCommandPaletteDialog items={items} onClose={onClose} />;
}

function WorkspaceCommandPaletteDialog({
  items,
  onClose,
}: {
  items: readonly WorkspaceCommandItem[];
  onClose: () => void;
}) {
  const [query, setQuery] = useState("");
  const [activeIndex, setActiveIndex] = useState(0);
  const dialogRef = useRef<HTMLElement | null>(null);
  const inputRef = useRef<HTMLInputElement | null>(null);
  const listboxId = useId();
  const titleId = useId();
  const { onBackdropPointerDown, onBackdropClick } = useDialogA11y(true, dialogRef, onClose);

  const filteredItems = useMemo(() => {
    const trimmed = query.trim().slice(0, MAX_QUERY_LENGTH);
    if (!trimmed) {
      return items.slice();
    }
    return searchBitapItems(
      items,
      trimmed,
      (item) =>
        `${item.categoryLabel} ${item.label} ${item.description ?? ""} ${item.keywords ?? ""}`.slice(
          0,
          MAX_CANDIDATE_LENGTH,
        ),
    ).map((match) => match.item);
  }, [items, query]);

  const clampedIndex =
    filteredItems.length > 0 ? Math.min(activeIndex, filteredItems.length - 1) : -1;
  const activeOptionId =
    clampedIndex >= 0 ? `${listboxId}-option-${filteredItems[clampedIndex].id}` : undefined;

  const handleKeyDown = (event: KeyboardEvent<HTMLElement>) => {
    if (event.key === "Escape") {
      event.preventDefault();
      event.stopPropagation();
      onClose();
      return;
    }
    if (filteredItems.length === 0) {
      return;
    }
    if (event.key === "ArrowDown") {
      event.preventDefault();
      setActiveIndex((prev) => (prev + 1) % filteredItems.length);
      return;
    }
    if (event.key === "ArrowUp") {
      event.preventDefault();
      setActiveIndex((prev) => (prev - 1 + filteredItems.length) % filteredItems.length);
      return;
    }
    if (event.key === "Home") {
      event.preventDefault();
      setActiveIndex(0);
      return;
    }
    if (event.key === "End") {
      event.preventDefault();
      setActiveIndex(filteredItems.length - 1);
      return;
    }
    if (event.key === "Enter" && !event.nativeEvent.isComposing && clampedIndex >= 0) {
      event.preventDefault();
      const target = filteredItems[clampedIndex];
      target.onSelect();
      onClose();
    }
  };

  return (
    <div
      className="workspace-modal-backdrop workspace-command-backdrop"
      role="presentation"
      onPointerDown={(event) => onBackdropPointerDown(event.target === event.currentTarget)}
      onClick={(event) => onBackdropClick(event.target === event.currentTarget)}
    >
      <section
        ref={dialogRef}
        className="workspace-modal workspace-command-dialog"
        role="dialog"
        aria-modal="true"
        aria-labelledby={titleId}
        tabIndex={-1}
        onKeyDown={handleKeyDown}
      >
        <h2 id={titleId} className="sr-only">
          Workspace command palette
        </h2>
        <div className="workspace-command-search-row">
          <Search aria-hidden="true" className="workspace-command-search-icon" />
          <input
            ref={inputRef}
            className="workspace-command-input"
            type="text"
            role="combobox"
            aria-autocomplete="list"
            aria-expanded="true"
            aria-controls={listboxId}
            aria-activedescendant={activeOptionId}
            aria-label="명령, 방, 에이전트, 모달 검색"
            placeholder="방 이동, 에이전트 포커스, 모달 열기, 테마 전환..."
            value={query}
            maxLength={MAX_QUERY_LENGTH}
            onChange={(event) => {
              setQuery(event.target.value);
              setActiveIndex(0);
            }}
          />
          <kbd className="workspace-command-kbd">ESC</kbd>
          <button
            type="button"
            className="icon-button workspace-modal-close"
            onClick={onClose}
            aria-label="커맨드 팔레트 닫기"
          >
            <X aria-hidden="true" />
          </button>
        </div>

        <ul id={listboxId} className="workspace-command-list" role="listbox" aria-label="명령 목록">
          {filteredItems.length > 0 ? (
            filteredItems.map((item, index) => {
              const selected = index === clampedIndex;
              return (
                <li
                  key={item.id}
                  id={`${listboxId}-option-${item.id}`}
                  role="option"
                  aria-selected={selected}
                  className={selected ? "workspace-command-option workspace-command-option-active" : "workspace-command-option"}
                >
                  <button
                    type="button"
                    className="workspace-command-option-button"
                    onMouseEnter={() => setActiveIndex(index)}
                    onClick={() => {
                      item.onSelect();
                      onClose();
                    }}
                  >
                    <span className="workspace-command-badge">{item.categoryLabel}</span>
                    <span className="workspace-command-main">
                      <strong>{item.label}</strong>
                      {item.description ? <small>{item.description}</small> : null}
                    </span>
                    {item.shortcut ? <kbd className="workspace-command-kbd">{item.shortcut}</kbd> : null}
                  </button>
                </li>
              );
            })
          ) : (
            <li className="workspace-command-empty" role="presentation">
              일치하는 명령이나 항목이 없습니다.
            </li>
          )}
        </ul>
      </section>
    </div>
  );
}
