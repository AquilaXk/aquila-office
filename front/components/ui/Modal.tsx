"use client";

import { X } from "lucide-react";
import { useEffect, type ReactNode } from "react";
import { cn } from "./utils";

export type ModalProps = {
  open: boolean;
  title: string;
  children: ReactNode;
  onClose: () => void;
  className?: string;
};

export default function Modal({ open, title, children, onClose, className }: ModalProps) {
  useEffect(() => {
    if (!open) {
      return;
    }
    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") {
        onClose();
      }
    };
    document.addEventListener("keydown", handleKeyDown);
    return () => document.removeEventListener("keydown", handleKeyDown);
  }, [onClose, open]);

  if (!open) {
    return null;
  }

  return (
    <div className="ao-modal-backdrop" onMouseDown={(event) => {
      if (event.target === event.currentTarget) {
        onClose();
      }
    }}>
      <section
        aria-modal="true"
        className={cn("ao-modal", className)}
        role="dialog"
        aria-label={title}
      >
        <header className="ao-modal-header">
          <h2>{title}</h2>
          <button type="button" aria-label="닫기" className="ao-modal-close" onClick={onClose}>
            <X aria-hidden="true" />
          </button>
        </header>
        <div className="ao-modal-body">{children}</div>
      </section>
    </div>
  );
}
