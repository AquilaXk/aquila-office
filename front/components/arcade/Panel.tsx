"use client";

import type { CSSProperties, ReactNode } from "react";

interface T4PanelProps {
  children: ReactNode;
  label?: string;
  rightSlot?: ReactNode;
  accent?: string;
  style?: CSSProperties;
  className?: string;
}

export default function T4Panel({
  children,
  label,
  rightSlot,
  style,
  className,
}: T4PanelProps) {
  return (
    <div
      className={className}
      style={{
        position: "relative",
        background: "#18191d",
        border: "1px solid rgba(230, 234, 240, 0.08)",
        borderRadius: 12,
        ...style,
      }}
    >
      {(label || rightSlot) && (
        <div
          style={{
            position: "absolute",
            top: -9,
            left: 12,
            right: 12,
            display: "flex",
            alignItems: "center",
            justifyContent: "space-between",
            pointerEvents: "none",
            zIndex: 1,
          }}
        >
          {label ? (
            <span
              style={{
                padding: "0 6px",
                background: "var(--surface-canvas, #121317)",
                fontFamily: "var(--font-sans)",
                fontSize: 12,
                fontWeight: 500,
                letterSpacing: "0.04em",
                color: "var(--text-secondary, #b2bbc5)",
              }}
            >
              {label}
            </span>
          ) : (
            <span />
          )}
          {rightSlot ? (
            <span
              style={{
                padding: "0 6px",
                background: "var(--surface-canvas, #121317)",
                fontFamily: "var(--font-mono)",
                fontSize: 12,
                color: "var(--text-muted, #6e7681)",
              }}
            >
              {rightSlot}
            </span>
          ) : null}
        </div>
      )}
      {children}
    </div>
  );
}
