"use client";

import { X } from "lucide-react";
import { useEffect, type ReactNode } from "react";
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
  useEffect(() => {
    if (!open || disableEscapeClose) {
      return;
    }
    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") {
        onClose();
      }
    };
    document.addEventListener("keydown", handleKeyDown);
    return () => document.removeEventListener("keydown", handleKeyDown);
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
        aria-modal="true"
        className={cn("ao-modal", "ao-modal-surface", `ao-modal-${size}`, className)}
        role="dialog"
        aria-label={label}
        style={{
          background: "#18191d",
          border: "1px solid rgba(230, 234, 240, 0.12)",
          borderRadius: 16,
          boxShadow: "0 24px 64px rgba(0, 0, 0, 0.48)",
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
