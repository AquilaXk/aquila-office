"use client";

import { GoogleOAuthProvider } from "@react-oauth/google";
import type { ReactNode } from "react";

const GOOGLE_CLIENT_ID = process.env.NEXT_PUBLIC_GOOGLE_CLIENT_ID ?? "";

export default function Providers({ children }: { children: ReactNode }) {
  return (
    <GoogleOAuthProvider clientId={GOOGLE_CLIENT_ID}>
      {children}
      {!GOOGLE_CLIENT_ID ? (
        <div className="ao-toast ao-toast-warning" role="status">
          NEXT_PUBLIC_GOOGLE_CLIENT_ID 환경 변수를 설정해 주세요.
        </div>
      ) : null}
    </GoogleOAuthProvider>
  );
}
