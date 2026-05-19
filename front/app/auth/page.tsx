"use client";

import { GoogleLogin } from "@react-oauth/google";
import Image from "next/image";
import { AlertCircle, Loader2 } from "lucide-react";
import { useRouter, useSearchParams } from "next/navigation";
import { Suspense, useEffect, useState, type ReactNode } from "react";
import { isAuthenticated, loginWithGoogle, saveAuthTokens } from "@/lib/auth";
import { BRAND_MASCOT_PATH, SERVICE_NAME } from "@/lib/branding";

export default function AuthPage() {
  return (
    <Suspense fallback={<AuthFallback />}>
      <AuthContent />
    </Suspense>
  );
}

function AuthContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const returnTo = safeReturnTo(searchParams.get("returnTo"));
  const [state, setState] = useState<
    | { status: "idle"; error: null }
    | { status: "submitting"; error: null }
    | { status: "error"; error: string }
  >({ status: "idle", error: null });

  useEffect(() => {
    if (isAuthenticated()) {
      router.replace(nextStep(returnTo));
    }
  }, [router, returnTo]);

  const handleCredential = async (credential?: string) => {
    if (!credential) {
      setState({ status: "error", error: "Google 인증 토큰을 받지 못했습니다." });
      return;
    }

    setState({ status: "submitting", error: null });
    try {
      const tokens = await loginWithGoogle(credential);
      saveAuthTokens(tokens);
      router.replace(nextStep(returnTo));
    } catch (error) {
      setState({
        status: "error",
        error: error instanceof Error ? error.message : "Google 로그인에 실패했습니다.",
      });
    }
  };

  return (
    <AuthLayout>
      <div className="auth-menu-line" aria-hidden="true">
        <span>&gt;</span>
        <strong>GOOGLE LOGIN</strong>
      </div>

      <div className="auth-google-action">
        <GoogleLogin
          onSuccess={(response) => void handleCredential(response.credential)}
          onError={() => setState({ status: "error", error: "Google 로그인이 취소되었거나 실패했습니다." })}
          theme="filled_black"
          size="large"
          shape="rectangular"
          text="continue_with"
          logo_alignment="left"
          width="280"
          useOneTap={false}
        />
      </div>

      {state.status === "submitting" ? (
        <p className="auth-state" role="status">
          <Loader2 className="spin" aria-hidden="true" />
          인증 세션을 저장하고 있습니다.
        </p>
      ) : null}
      {state.status === "error" ? (
        <p className="auth-state auth-state-error" role="alert">
          <AlertCircle aria-hidden="true" />
          {state.error}
        </p>
      ) : null}
    </AuthLayout>
  );
}

function AuthFallback() {
  return (
    <AuthLayout busy>
      <p className="auth-state" role="status">
        <Loader2 className="spin" aria-hidden="true" />
        로그인 화면을 준비하고 있습니다.
      </p>
    </AuthLayout>
  );
}

function AuthLayout({
  busy = false,
  children,
}: {
  busy?: boolean;
  children: ReactNode;
}) {
  return (
    <main className="auth-page" aria-label="인증 화면">
      <section className="auth-panel" aria-labelledby="auth-title" aria-busy={busy || undefined}>
        <div className="auth-crest">
          <Image
            src={BRAND_MASCOT_PATH}
            alt={`${SERVICE_NAME} crest`}
            width={88}
            height={88}
            priority
          />
        </div>
        <p className="eyebrow">AUTHENTICATION</p>
        <h1 id="auth-title">{SERVICE_NAME.toUpperCase()}</h1>
        <p className="auth-subtitle">secure workspace access</p>
        <div className="auth-panel-body">{children}</div>
        <p className="auth-screen-hint">[ GOOGLE ACCOUNT REQUIRED ]</p>
      </section>
    </main>
  );
}

function safeReturnTo(raw: string | null): string {
  if (!raw || !raw.startsWith("/") || raw.startsWith("//")) {
    return "/workspaces";
  }
  return raw;
}

function nextStep(returnTo: string): string {
  const params = new URLSearchParams({ returnTo });
  return `/auth/character?${params.toString()}`;
}
