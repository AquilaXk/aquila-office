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
        background: "var(--ui-surface, #18191d)",
        border: "1px solid var(--ui-border-subtle, rgba(230, 234, 240, 0.08))",
        borderRadius: 6,
        ...style,
      }}
    >
      {(label || rightSlot) && (
        <div
          style={{
            gridColumn: "1 / -1",
            display: "flex",
            alignItems: "center",
            justifyContent: "space-between",
            gap: 8,
            paddingBottom: 6,
            marginBottom: 6,
            borderBottom: "1px solid var(--ui-border-subtle, rgba(230, 234, 240, 0.08))",
          }}
        >
          {label ? (
            <span
              style={{
                fontFamily: "var(--font-family-ui, var(--font-sans))",
                fontSize: 12,
                fontWeight: 600,
                letterSpacing: "0.02em",
                color: "var(--ui-text-secondary, #b2bbc5)",
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
                fontFamily: "var(--font-family-mono, var(--font-mono))",
                fontSize: 12,
                color: "var(--ui-text-muted, #6e7681)",
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
