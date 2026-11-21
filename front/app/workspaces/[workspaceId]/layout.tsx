import type { ReactNode } from "react";

export function generateStaticParams() {
  return [{ workspaceId: "0" }];
}

export default function WorkspaceLayout({ children }: { children: ReactNode }) {
  return children;
}
