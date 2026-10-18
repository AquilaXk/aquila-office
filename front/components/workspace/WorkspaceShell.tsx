"use client";

import Link from "next/link";
import type { ReactNode } from "react";
import { Button } from "@/components/ui";
import { GlyphText } from "@/components/arcade";
import type { WorkspaceDetail } from "@/lib/api/workspaces";

export default function WorkspaceShell({
  children,
}: {
  workspace: WorkspaceDetail;
  children: ReactNode;
  sidebar?: ReactNode;
}) {
  return <>{children}</>;
}

export function WorkspaceErrorState({ message }: { message: string }) {
  return (
    <main
      className="theme-web flex min-h-screen items-center justify-center px-4"
      style={{ background: "var(--surface-canvas, #121317)" }}
    >
      <section
        className="w-full max-w-md px-6 py-8 text-center"
        style={{
          border: "1px solid rgba(234, 67, 53, 0.32)",
          borderRadius: 12,
          background: "#18191d",
        }}
      >
        <p
          style={{
            fontFamily: "var(--font-sans)",
            fontSize: 12,
            fontWeight: 600,
            letterSpacing: "0.04em",
            color: "var(--status-danger, #ea4335)",
            marginBottom: 12,
          }}
        >
          <GlyphText glyph="⚠">ERROR</GlyphText>
        </p>
        <p
          style={{
            fontFamily: "var(--font-sans)",
            fontSize: 14,
            color: "var(--text-primary, #f8f9fc)",
          }}
        >
          {message}
        </p>
        <Link href="/workspaces" className="mt-5 inline-flex">
          <Button variant="secondary">
            <GlyphText glyph="◀">WORKSPACES</GlyphText>
          </Button>
        </Link>
      </section>
    </main>
  );
}

export function WorkspaceLoadingState() {
  return (
    <main
      className="theme-web flex min-h-screen items-center justify-center px-4"
      style={{ background: "var(--surface-canvas, #121317)" }}
    >
      <div
        style={{
          fontFamily: "var(--font-sans)",
          fontSize: 13,
          fontWeight: 500,
          color: "var(--text-secondary, #b2bbc5)",
        }}
      >
        <GlyphText glyph="◆">Loading workspace...</GlyphText>
      </div>
    </main>
  );
}
