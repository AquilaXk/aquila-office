"use client";

import { useCallback, useEffect, useRef, type RefObject } from "react";

const FOCUSABLE_SELECTOR = [
  "a[href]",
  "button:not([disabled])",
  "input:not([disabled])",
  "select:not([disabled])",
  "textarea:not([disabled])",
  "[tabindex]:not([tabindex='-1'])",
].join(", ");

/**
 * 모달 다이얼로그에 Escape 닫기, 초기 포커스, Tab 포커스 트랩 및 닫힘 시 포커스 복원을 적용한다.
 */
export function useDialogA11y(
  open: boolean,
  dialogRef: RefObject<HTMLElement | null>,
  onClose: () => void,
) {
  const pointerDownOnBackdropRef = useRef(false);

  const onBackdropPointerDown = useCallback((isBackdrop: boolean) => {
    pointerDownOnBackdropRef.current = isBackdrop;
  }, []);

  const onBackdropClick = useCallback(
    (isBackdrop: boolean) => {
      const startedOutside = pointerDownOnBackdropRef.current;
      pointerDownOnBackdropRef.current = false;
      if (startedOutside && isBackdrop) {
        onClose();
      }
    },
    [onClose],
  );

  useEffect(() => {
    if (!open) {
      pointerDownOnBackdropRef.current = false;
      return;
    }

    const previousFocused =
      document.activeElement instanceof HTMLElement ? document.activeElement : null;

    const frameId = window.requestAnimationFrame(() => {
      const dialog = dialogRef.current;
      if (!dialog) {
        return;
      }
      if (!dialog.contains(document.activeElement)) {
        const first = dialog.querySelector<HTMLElement>(FOCUSABLE_SELECTOR);
        if (first) {
          first.focus();
        } else {
          dialog.focus();
        }
      }
    });

    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") {
        event.preventDefault();
        onClose();
        return;
      }

      if (event.key !== "Tab") {
        return;
      }

      const dialog = dialogRef.current;
      if (!dialog) {
        return;
      }

      const focusable = Array.from(dialog.querySelectorAll<HTMLElement>(FOCUSABLE_SELECTOR)).filter(
        (element) => element.offsetParent !== null || element === document.activeElement,
      );
      if (focusable.length === 0) {
        event.preventDefault();
        dialog.focus();
        return;
      }

      const first = focusable[0];
      const last = focusable[focusable.length - 1];
      if (event.shiftKey) {
        if (document.activeElement === first || !dialog.contains(document.activeElement)) {
          event.preventDefault();
          last.focus();
        }
      } else if (document.activeElement === last || !dialog.contains(document.activeElement)) {
        event.preventDefault();
        first.focus();
      }
    };

    window.addEventListener("keydown", handleKeyDown);
    return () => {
      window.cancelAnimationFrame(frameId);
      window.removeEventListener("keydown", handleKeyDown);
      if (previousFocused && previousFocused.isConnected) {
        previousFocused.focus();
      }
    };
  }, [dialogRef, onClose, open]);

  return { onBackdropPointerDown, onBackdropClick };
}
