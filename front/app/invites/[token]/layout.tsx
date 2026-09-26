import type { ReactNode } from "react";

export function generateStaticParams() {
  return [{ token: "0" }];
}

export default function InviteLayout({ children }: { children: ReactNode }) {
  return children;
}
