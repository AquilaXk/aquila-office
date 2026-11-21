"use client";

import { X } from "lucide-react";
import { useEffect, useRef, type ReactNode } from "react";
import { cn } from "./utils";

export type ModalProps = {
  open: boolean;
  onClose: () => void;
  title?: ReactNode;
  eyebrow?: string;
  size?: "sm" | "md" | "lg" | "full";
  disableEscapeClose?: boolean;
  children: ReactNode;
  className?: string;
};

export type ModalBodyProps = {
  children: ReactNode;
  className?: string;
};

export type ModalFooterProps = {
  children?: ReactNode;
  className?: string;
};

const FOCUSABLE_SELECTOR = [
  "a[href]",
  "button:not([disabled])",
  "input:not([disabled])",
  "select:not([disabled])",
  "textarea:not([disabled])",
  "[tabindex]:not([tabindex=\"-1\"])",
].join(",");

function ModalBody({ children, className }: ModalBodyProps) {
  return <div className={cn("ao-modal-body", className)}>{children}</div>;
}

function ModalFooter({ children, className }: ModalFooterProps) {
  return <footer className={cn("ao-modal-footer", className)}>{children}</footer>;
}

function ModalRoot({
  open,
  title,
  children,
  onClose,
  size = "md",
  disableEscapeClose = false,
  className,
}: ModalProps) {
  const dialogRef = useRef<HTMLElement | null>(null);

  useEffect(() => {
    if (!open) {
      return;
    }
    const previousFocus =
      typeof document !== "undefined" && document.activeElement instanceof HTMLElement
        ? document.activeElement
        : null;
    const dialog = dialogRef.current;
    if (dialog) {
      const firstFocusable = dialog.querySelector<HTMLElement>(FOCUSABLE_SELECTOR);
      if (firstFocusable) {
        firstFocusable.focus();
      } else {
        dialog.focus();
      }
    }

    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape" && !disableEscapeClose) {
        event.preventDefault();
        onClose();
        return;
      }
      if (event.key === "Tab" && dialog) {
        const focusable = Array.from(dialog.querySelectorAll<HTMLElement>(FOCUSABLE_SELECTOR)).filter(
          (node) => node.offsetParent !== null,
        );
        if (focusable.length === 0) {
          event.preventDefault();
          return;
        }
        const first = focusable[0];
        const last = focusable[focusable.length - 1];
        if (event.shiftKey && document.activeElement === first) {
          event.preventDefault();
          last.focus();
        } else if (!event.shiftKey && document.activeElement === last) {
          event.preventDefault();
          first.focus();
        }
      }
    };

    document.addEventListener("keydown", handleKeyDown);
    return () => {
      document.removeEventListener("keydown", handleKeyDown);
      previousFocus?.focus();
    };
  }, [disableEscapeClose, onClose, open]);

  if (!open) {
    return null;
  }

  const label = typeof title === "string" ? title : "Modal";

  return (
    <div
      className="ao-modal-backdrop"
      onMouseDown={(event) => {
        if (event.target === event.currentTarget) {
          onClose();
        }
      }}
    >
      <section
        ref={dialogRef}
        tabIndex={-1}
        aria-modal="true"
        className={cn("ao-modal", "ao-modal-surface", `ao-modal-${size}`, className)}
        role="dialog"
        aria-label={label}
        style={{
          background: "var(--ui-surface)",
          border: "1px solid var(--ui-border-strong)",
          borderRadius: 8,
          boxShadow: "var(--ui-shadow)",
        }}
      >
        <header className="ao-modal-header">
          {title ? <h2>{title}</h2> : <span aria-hidden="true" />}
          <button
            type="button"
            aria-label="닫기"
            className="ao-modal-close"
            onClick={onClose}
          >
            <X aria-hidden="true" />
          </button>
        </header>
        <ModalBody>{children}</ModalBody>
      </section>
    </div>
  );
}

const Modal = Object.assign(ModalRoot, {
  Body: ModalBody,
  Footer: ModalFooter,
});

export default Modal;
