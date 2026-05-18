"use client";

import { GoogleLogin } from "@react-oauth/google";
import { AlertCircle, Loader2, LogIn } from "lucide-react";
import { useRouter, useSearchParams } from "next/navigation";
import { Suspense, useEffect, useState } from "react";
import { isAuthenticated, loginWithGoogle, saveAuthTokens } from "@/lib/auth";

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
      router.replace(returnTo);
    }
  }, [returnTo, router]);

  const handleCredential = async (credential?: string) => {
    if (!credential) {
      setState({ status: "error", error: "Google 인증 토큰을 받지 못했습니다." });
      return;
    }

    setState({ status: "submitting", error: null });
    try {
      const tokens = await loginWithGoogle(credential);
      saveAuthTokens(tokens);
      router.replace(returnTo);
    } catch (error) {
      setState({
        status: "error",
        error: error instanceof Error ? error.message : "Google 로그인에 실패했습니다.",
      });
    }
  };

  return (
    <main className="auth-page">
      <section className="auth-panel" aria-labelledby="auth-title">
        <div className="auth-panel-heading">
          <LogIn aria-hidden="true" />
          <div>
            <span className="workspace-detail-label">Authentication</span>
            <h1 id="auth-title">Google 로그인</h1>
          </div>
        </div>

        <div className="auth-google-action">
          <GoogleLogin
            onSuccess={(response) => void handleCredential(response.credential)}
            onError={() => setState({ status: "error", error: "Google 로그인이 취소되었거나 실패했습니다." })}
            size="large"
            shape="rectangular"
            text="continue_with"
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
      </section>
    </main>
  );
}

function AuthFallback() {
  return (
    <main className="auth-page">
      <section className="auth-panel" aria-busy="true">
        <p className="auth-state" role="status">
          <Loader2 className="spin" aria-hidden="true" />
          로그인 화면을 준비하고 있습니다.
        </p>
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
