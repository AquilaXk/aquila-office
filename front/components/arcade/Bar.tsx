"use client";

import type { CSSProperties } from "react";

interface T4BarProps {
  label: string;
  value: number;
  max: number;
  color?: string;
  width?: number | string;
  style?: CSSProperties;
}

export default function T4Bar({
  label,
  value,
  max,
  color = "var(--accent-primary, #3279f9)",
  width = 160,
  style,
}: T4BarProps) {
  const pct = Math.max(0, Math.min(1, max > 0 ? value / max : 0));
  return (
    <div style={{ width, ...style }}>
      <div
        style={{
          display: "flex",
          justifyContent: "space-between",
          fontFamily: "var(--font-sans)",
          fontSize: 12,
          color: "var(--text-secondary, #b2bbc5)",
          marginBottom: 4,
        }}
      >
        <span>{label}</span>
        <span style={{ color: "var(--text-primary, #f8f9fc)", fontFamily: "var(--font-mono)" }}>
          {value}/{max}
        </span>
      </div>
      <div
        style={{
          height: 4,
          borderRadius: 9999,
          background: "rgba(230, 234, 240, 0.08)",
          position: "relative",
          overflow: "hidden",
        }}
      >
        <div
          style={{
            width: `${pct * 100}%`,
            height: "100%",
            borderRadius: 9999,
            background: color,
          }}
        />
      </div>
    </div>
  );
}
