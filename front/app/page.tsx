"use client";

import {
  AlertCircle,
  Building2,
  FolderOpen,
  LogIn,
  LogOut,
  Plus,
  RefreshCw,
  Save,
  Server,
  UserRound,
  Wifi,
  WifiOff,
} from "lucide-react";
import Image from "next/image";
import Link from "next/link";
import { useCallback, useEffect, useState, type FormEvent } from "react";
import { API_BASE_URL, ApiError, apiFetch } from "@/lib/api";
import {
  clearAuthTokens,
  loginWithGoogle,
  logoutAuth,
  readAuthTokens,
  refreshAuthToken,
  resolveAccessToken,
  saveAuthTokens,
  type AuthTokens,
} from "@/lib/auth";
import {
  BRAND_MASCOT_PATH,
  REPRESENTATIVE_IMAGE_PATHS,
  SERVICE_NAME,
} from "@/lib/branding";
import {
  fetchMemberProfile,
  updateMemberProfile,
  type MemberProfile,
} from "@/lib/members";
import { fetchWorkspaces, type WorkspaceSummary } from "@/lib/workspaces";

type HealthResponse = {
  status: string;
  service: string;
  checkedAt: string;
};

type HealthState =
  | { status: "idle" | "loading"; data: null; error: null }
  | { status: "online"; data: HealthResponse; error: null }
  | { status: "offline"; data: null; error: string };

type WorkspaceState =
  | { status: "idle" | "loading"; data: null; error: null }
  | { status: "ready"; data: WorkspaceSummary[]; error: null }
  | { status: "offline"; data: null; error: string };

type AuthState =
  | { status: "idle"; email: string | null; message: string | null; error: null }
  | { status: "submitting"; email: string | null; message: string | null; error: null }
  | { status: "ready"; email: string; message: string; error: null }
  | { status: "error"; email: string | null; message: null; error: string };

type ProfileState =
  | { status: "idle" | "loading"; profile: null; error: null }
  | { status: "ready"; profile: MemberProfile; error: null }
  | { status: "offline"; profile: null; error: string };

type ProfileMutationState =
  | { status: "idle"; message: null }
  | { status: "submitting"; message: null }
  | { status: "success"; message: string }
  | { status: "error"; message: string };

const assetHighlights = [
  {
    src: REPRESENTATIVE_IMAGE_PATHS[0],
    name: "office-bank-preview.png",
    title: "Treasury desk",
    caption: "고밀도 업무 패널",
  },
  {
    src: REPRESENTATIVE_IMAGE_PATHS[1],
    name: "office-blog-preview.png",
    title: "Archive desk",
    caption: "운영 피드 대시보드",
  },
] as const;

const brandMascotAsset = "brand-mascot.png";

export default function Home() {
  const [accessToken, setAccessToken] = useState(() => resolveAccessToken());
  const [idToken, setIdToken] = useState("");
  const [authState, setAuthState] = useState<AuthState>(() =>
    authStateFromTokens(readAuthTokens()),
  );
  const [health, setHealth] = useState<HealthState>({
    status: "idle",
    data: null,
    error: null,
  });
  const [workspaces, setWorkspaces] = useState<WorkspaceState>({
    status: "idle",
    data: null,
    error: null,
  });
  const [profile, setProfile] = useState<ProfileState>({
    status: "idle",
    profile: null,
    error: null,
  });
  const [profileDisplayName, setProfileDisplayName] = useState("");
  const [profileAvatarKind, setProfileAvatarKind] = useState("");
  const [profileSkinColor, setProfileSkinColor] = useState("");
  const [profileHairColor, setProfileHairColor] = useState("");
  const [profileShirtColor, setProfileShirtColor] = useState("");
  const [profileUpdateState, setProfileUpdateState] = useState<ProfileMutationState>({
    status: "idle",
    message: null,
  });

  const syncProfileInputs = useCallback((nextProfile: MemberProfile) => {
    setProfileDisplayName(nextProfile.displayName);
    setProfileAvatarKind(nextProfile.avatarKind);
    setProfileSkinColor(nextProfile.avatarColors.skin);
    setProfileHairColor(nextProfile.avatarColors.hair);
    setProfileShirtColor(nextProfile.avatarColors.shirt);
  }, []);

  const refreshDashboard = useCallback(async () => {
    setHealth({ status: "loading", data: null, error: null });
    setWorkspaces({ status: "loading", data: null, error: null });
    setProfile(
      accessToken
        ? { status: "loading", profile: null, error: null }
        : { status: "idle", profile: null, error: null },
    );

    const [healthResult, workspaceResult, profileResult] = await Promise.all([
      fetchHealth(),
      fetchWorkspaceList(accessToken),
      accessToken
        ? fetchProfileState(accessToken)
        : Promise.resolve({ status: "idle", profile: null, error: null } satisfies ProfileState),
    ]);
    setHealth(healthResult);
    setWorkspaces(workspaceResult);
    setProfile(profileResult);
    if (profileResult.status === "ready") {
      syncProfileInputs(profileResult.profile);
    }
  }, [accessToken, syncProfileInputs]);

  useEffect(() => {
    let ignore = false;

    Promise.all([
      fetchHealth(),
      fetchWorkspaceList(accessToken),
      accessToken
        ? fetchProfileState(accessToken)
        : Promise.resolve({ status: "idle", profile: null, error: null } satisfies ProfileState),
    ]).then(
      ([healthResult, workspaceResult, profileResult]) => {
        if (!ignore) {
          setHealth(healthResult);
          setWorkspaces(workspaceResult);
          setProfile(profileResult);
          if (profileResult.status === "ready") {
            syncProfileInputs(profileResult.profile);
          }
        }
      },
    );

    return () => {
      ignore = true;
    };
  }, [accessToken, syncProfileInputs]);

  const reloadProfile = async () => {
    if (!accessToken) {
      setProfile({ status: "idle", profile: null, error: null });
      return;
    }

    setProfile({ status: "loading", profile: null, error: null });
    setProfileUpdateState({ status: "idle", message: null });
    const nextProfile = await fetchProfileState(accessToken);
    setProfile(nextProfile);
    if (nextProfile.status === "ready") {
      syncProfileInputs(nextProfile.profile);
    }
  };

  const handleLogin = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    if (!idToken.trim()) {
      setAuthState({
        status: "error",
        email: authState.email,
        message: null,
        error: "Google id token을 입력해 주세요.",
      });
      return;
    }

    setAuthState({
      status: "submitting",
      email: authState.email,
      message: null,
      error: null,
    });

    try {
      const tokens = await loginWithGoogle(idToken);
      saveAuthTokens(tokens);
      setAccessToken(tokens.accessToken);
      setIdToken("");
      setAuthState({
        status: "ready",
        email: tokens.email,
        message: "인증 세션이 저장되었습니다.",
        error: null,
      });
    } catch (error) {
      setAuthState({
        status: "error",
        email: authState.email,
        message: null,
        error: messageFromError(error, "로그인 요청을 처리하지 못했습니다."),
      });
    }
  };

  const handleRefreshToken = async () => {
    const tokens = readAuthTokens();
    if (!tokens) {
      setAuthState({
        status: "error",
        email: null,
        message: null,
        error: "저장된 refresh token이 없습니다.",
      });
      return;
    }

    setAuthState({
      status: "submitting",
      email: tokens.email,
      message: null,
      error: null,
    });

    try {
      const refreshed = await refreshAuthToken(tokens.refreshToken);
      const nextTokens = { ...tokens, ...refreshed };
      saveAuthTokens(nextTokens);
      setAccessToken(nextTokens.accessToken);
      setAuthState({
        status: "ready",
        email: nextTokens.email,
        message: "인증 세션이 갱신되었습니다.",
        error: null,
      });
    } catch (error) {
      setAuthState({
        status: "error",
        email: tokens.email,
        message: null,
        error: messageFromError(error, "token 갱신 요청을 처리하지 못했습니다."),
      });
    }
  };

  const handleLogout = async () => {
    const tokens = readAuthTokens();

    setAuthState({
      status: "submitting",
      email: tokens?.email ?? authState.email,
      message: null,
      error: null,
    });

    try {
      if (tokens) {
        await logoutAuth(tokens.refreshToken);
      }
      clearAuthTokens();
      setAccessToken(resolveAccessToken());
      setAuthState({
        status: "idle",
        email: null,
        message: "인증 세션이 삭제되었습니다.",
        error: null,
      });
    } catch (error) {
      clearAuthTokens();
      setAccessToken(resolveAccessToken());
      setAuthState({
        status: "error",
        email: null,
        message: null,
        error: messageFromError(error, "로그아웃 요청을 처리하지 못했습니다."),
      });
    }
  };

  const handleUpdateProfile = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    if (!accessToken) {
      setProfileUpdateState({
        status: "error",
        message: "저장된 access token이 없습니다.",
      });
      return;
    }

    const nextDisplayName = profileDisplayName.trim();
    const nextAvatarKind = profileAvatarKind.trim();
    const nextSkinColor = profileSkinColor.trim();
    const nextHairColor = profileHairColor.trim();
    const nextShirtColor = profileShirtColor.trim();
    if (!nextDisplayName || !nextAvatarKind || !nextSkinColor || !nextHairColor || !nextShirtColor) {
      setProfileUpdateState({
        status: "error",
        message: "표시 이름, avatar kind, skin/hair/shirt 색상을 모두 입력해 주세요.",
      });
      return;
    }

    setProfileUpdateState({ status: "submitting", message: null });
    try {
      const updatedProfile = await updateMemberProfile(accessToken, {
        displayName: nextDisplayName,
        avatarKind: nextAvatarKind,
        avatarColors: {
          skin: nextSkinColor,
          hair: nextHairColor,
          shirt: nextShirtColor,
        },
      });
      setProfile({ status: "ready", profile: updatedProfile, error: null });
      syncProfileInputs(updatedProfile);
      setProfileUpdateState({
        status: "success",
        message: "회원 프로필이 저장되었습니다.",
      });
    } catch (error) {
      setProfileUpdateState({
        status: "error",
        message: messageFromError(error, "회원 프로필을 저장하지 못했습니다."),
      });
    }
  };

  const checkedAt = health.data?.checkedAt
    ? new Intl.DateTimeFormat("ko-KR", {
        dateStyle: "medium",
        timeStyle: "medium",
      }).format(new Date(health.data.checkedAt))
    : "-";

  const isLoading = health.status === "loading";
  const isOnline = health.status === "online";
  const isWorkspaceLoading = workspaces.status === "loading";
  const workspaceCount = workspaces.data?.length ?? 0;
  const isAuthSubmitting = authState.status === "submitting";
  const hasAccessToken = Boolean(accessToken);
  const isProfileLoading = profile.status === "loading";
  const isProfileUpdating = profileUpdateState.status === "submitting";
  const canUpdateProfile =
    hasAccessToken &&
    profileDisplayName.trim().length > 0 &&
    profileAvatarKind.trim().length > 0 &&
    profileSkinColor.trim().length > 0 &&
    profileHairColor.trim().length > 0 &&
    profileShirtColor.trim().length > 0 &&
    !isProfileUpdating;

  return (
    <main className="app-shell">
      <aside className="sidebar" aria-label={SERVICE_NAME}>
        <div className="brand-block">
          <div className="brand-crest">
            <Image
              src={BRAND_MASCOT_PATH}
              alt={`${SERVICE_NAME} crest`}
              width={72}
              height={72}
              priority
              data-asset={brandMascotAsset}
            />
          </div>
          <div className="brand-copy">
            <span>{SERVICE_NAME}</span>
            <strong>Command office</strong>
          </div>
        </div>
        <nav className="nav-list">
          <span className="nav-item">System</span>
          <span className="nav-item nav-item-active">Workspace</span>
          <span className="nav-item">Members</span>
        </nav>
      </aside>

      <section className="workspace">
        <header className="topbar">
          <div>
            <p className="eyebrow">{SERVICE_NAME}</p>
            <h1>워크스페이스 작전 현황</h1>
            <p className="topbar-summary">
              API 상태와 워크스페이스 접근 흐름을 한 화면에서 점검합니다.
            </p>
          </div>
          <button
            className="icon-button"
            type="button"
            onClick={() => void refreshDashboard()}
            disabled={isLoading || isWorkspaceLoading}
            title="상태 새로고침"
            aria-label="상태 새로고침"
          >
            <RefreshCw
              aria-hidden="true"
              className={isLoading || isWorkspaceLoading ? "spin" : undefined}
            />
          </button>
        </header>

        <section className="command-band" aria-label="대표 운영 화면">
          <div className="command-band-copy">
            <span>Operational archive</span>
            <strong>gothic command surface</strong>
            <p>브랜드 자산과 운영 패널을 어두운 금속 톤의 업무 화면으로 묶었습니다.</p>
          </div>
          <div className="asset-strip">
            {assetHighlights.map((asset) => (
              <figure className="asset-card" data-asset={asset.name} key={asset.name}>
                <Image
                  src={asset.src}
                  alt={asset.title}
                  width={360}
                  height={220}
                  sizes="(max-width: 860px) 100vw, 360px"
                />
                <figcaption>
                  <span>{asset.title}</span>
                  <strong>{asset.caption}</strong>
                </figcaption>
              </figure>
            ))}
          </div>
        </section>

        <section className="workspace-section" aria-label="인증 세션">
          <div className="section-header">
            <div>
              <p className="eyebrow">Auth</p>
              <h2>인증 세션</h2>
            </div>
            <span className="role-badge">
              {hasAccessToken ? "token ready" : "token empty"}
            </span>
          </div>

          <form className="workspace-create-form" onSubmit={(event) => void handleLogin(event)}>
            <label className="form-field">
              <span>Google id token</span>
              <input
                type="text"
                value={idToken}
                onChange={(event) => setIdToken(event.target.value)}
                placeholder="google-1|aquila@example.com|Aquila User"
                disabled={isAuthSubmitting}
              />
            </label>
            <div className="form-actions">
              <button className="primary-action primary-action-active" type="submit" disabled={isAuthSubmitting}>
                {isAuthSubmitting ? <RefreshCw className="spin" aria-hidden="true" /> : <LogIn aria-hidden="true" />}
                <span>{isAuthSubmitting ? "처리 중" : "로그인"}</span>
              </button>
              <button
                className="secondary-action"
                type="button"
                onClick={() => void handleRefreshToken()}
                disabled={isAuthSubmitting}
              >
                <RefreshCw aria-hidden="true" />
                <span>Refresh</span>
              </button>
              <button
                className="secondary-action"
                type="button"
                onClick={() => void handleLogout()}
                disabled={isAuthSubmitting}
              >
                <LogOut aria-hidden="true" />
                <span>Logout</span>
              </button>
            </div>
          </form>

          <div className={authState.status === "error" ? "state-panel state-panel-error" : "state-panel"}>
            <UserRound aria-hidden="true" />
            <strong>{authState.email ?? "브라우저 세션"}</strong>
            <p>{authState.error ?? authState.message ?? "저장된 token으로 보호 API를 호출합니다."}</p>
          </div>

          {hasAccessToken ? (
            <section className="workspace-create-form" aria-label="회원 프로필 설정">
              <div className="section-header">
                <div>
                  <p className="eyebrow">Profile</p>
                  <h2>회원 프로필</h2>
                </div>
                <button
                  className="secondary-action"
                  type="button"
                  onClick={() => void reloadProfile()}
                  disabled={isProfileLoading || isProfileUpdating}
                >
                  <RefreshCw
                    className={isProfileLoading ? "spin" : undefined}
                    aria-hidden="true"
                  />
                  <span>프로필 새로고침</span>
                </button>
              </div>

              {profile.status === "ready" ? (
                <div className="state-panel">
                  <UserRound aria-hidden="true" />
                  <strong>{profile.profile.displayName}</strong>
                  <p>{`${profile.profile.avatarKind} · skin ${profile.profile.avatarColors.skin} · hair ${profile.profile.avatarColors.hair} · shirt ${profile.profile.avatarColors.shirt}`}</p>
                </div>
              ) : null}

              {profile.status === "loading" ? (
                <div className="state-panel">
                  <RefreshCw className="spin" aria-hidden="true" />
                  <strong>프로필 확인 중</strong>
                  <p>회원 프로필 정보를 불러오고 있습니다.</p>
                </div>
              ) : null}

              {profile.status === "offline" ? (
                <div className="state-panel state-panel-error">
                  <AlertCircle aria-hidden="true" />
                  <strong>프로필 확인 실패</strong>
                  <p>{profile.error}</p>
                </div>
              ) : null}

              <form className="workspace-create-form" onSubmit={(event) => void handleUpdateProfile(event)}>
                <label className="form-field">
                  <span>
                    <strong>표시 이름</strong>
                    <small>{profileDisplayName.length}/80</small>
                  </span>
                  <input
                    value={profileDisplayName}
                    onChange={(event) => {
                      setProfileDisplayName(event.target.value);
                      setProfileUpdateState({ status: "idle", message: null });
                    }}
                    maxLength={80}
                    disabled={isProfileUpdating}
                  />
                </label>

                <label className="form-field">
                  <span>
                    <strong>Avatar Kind</strong>
                    <small>{profileAvatarKind.length}/40</small>
                  </span>
                  <input
                    value={profileAvatarKind}
                    onChange={(event) => {
                      setProfileAvatarKind(event.target.value);
                      setProfileUpdateState({ status: "idle", message: null });
                    }}
                    maxLength={40}
                    disabled={isProfileUpdating}
                  />
                </label>

                <label className="form-field">
                  <span>
                    <strong>Skin</strong>
                    <small>avatarColors.skin</small>
                  </span>
                  <input
                    value={profileSkinColor}
                    onChange={(event) => {
                      setProfileSkinColor(event.target.value);
                      setProfileUpdateState({ status: "idle", message: null });
                    }}
                    disabled={isProfileUpdating}
                  />
                </label>

                <label className="form-field">
                  <span>
                    <strong>Hair</strong>
                    <small>avatarColors.hair</small>
                  </span>
                  <input
                    value={profileHairColor}
                    onChange={(event) => {
                      setProfileHairColor(event.target.value);
                      setProfileUpdateState({ status: "idle", message: null });
                    }}
                    disabled={isProfileUpdating}
                  />
                </label>

                <label className="form-field">
                  <span>
                    <strong>Shirt</strong>
                    <small>avatarColors.shirt</small>
                  </span>
                  <input
                    value={profileShirtColor}
                    onChange={(event) => {
                      setProfileShirtColor(event.target.value);
                      setProfileUpdateState({ status: "idle", message: null });
                    }}
                    disabled={isProfileUpdating}
                  />
                </label>

                {profileUpdateState.status === "error" ? (
                  <div className="form-notice form-notice-error" role="status">
                    <AlertCircle aria-hidden="true" />
                    <span>{profileUpdateState.message}</span>
                  </div>
                ) : null}

                {profileUpdateState.status === "success" ? (
                  <div className="form-notice form-notice-success" role="status">
                    <UserRound aria-hidden="true" />
                    <span>{profileUpdateState.message}</span>
                  </div>
                ) : null}

                <div className="form-actions">
                  <button
                    className="primary-action primary-action-active"
                    type="submit"
                    disabled={!canUpdateProfile}
                  >
                    {isProfileUpdating ? (
                      <RefreshCw className="spin" aria-hidden="true" />
                    ) : (
                      <Save aria-hidden="true" />
                    )}
                    <span>{isProfileUpdating ? "저장 중" : "프로필 저장"}</span>
                  </button>
                </div>
              </form>
            </section>
          ) : null}
        </section>

        <section className="status-grid" aria-label="시스템 상태">
          <article className="status-panel status-panel-primary">
            <div className="panel-header">
              <Server aria-hidden="true" />
              <span>Backend API</span>
            </div>
            <strong>{health.data?.service ?? "aquila-office-back"}</strong>
            <p>{API_BASE_URL}</p>
          </article>

          <article className={isOnline ? "status-panel online" : "status-panel offline"}>
            <div className="panel-header">
              {isOnline ? <Wifi aria-hidden="true" /> : <WifiOff aria-hidden="true" />}
              <span>연결 상태</span>
            </div>
            <strong>{isLoading ? "확인 중" : isOnline ? "온라인" : "오프라인"}</strong>
            <p>{health.error ?? checkedAt}</p>
          </article>

          <article className="status-panel">
            <div className="panel-header">
              <Building2 aria-hidden="true" />
              <span>Workspaces</span>
            </div>
            <strong>{isWorkspaceLoading ? "확인 중" : `${workspaceCount}개`}</strong>
            <p>{workspaces.error ?? "가입된 워크스페이스"}</p>
          </article>
        </section>

        <section className="workspace-section" aria-label="워크스페이스 목록">
          <div className="section-header">
            <div>
              <p className="eyebrow">Workspace</p>
              <h2>내 워크스페이스</h2>
            </div>
            <Link className="primary-action primary-action-active" href="/workspaces/new">
              <Plus aria-hidden="true" />
              <span>생성</span>
            </Link>
          </div>

          {workspaces.status === "ready" && workspaces.data.length > 0 ? (
            <div className="workspace-list">
              {workspaces.data.map((workspace) => (
                <Link
                  className="workspace-row workspace-row-link"
                  href={`/workspaces/${workspace.workspaceId}`}
                  key={workspace.workspaceId}
                  aria-label={`${workspace.name} 상세 보기`}
                >
                  <div className="workspace-row-icon">
                    <Building2 aria-hidden="true" />
                  </div>
                  <div className="workspace-row-main">
                    <strong>{workspace.name}</strong>
                    <p>{workspace.description ?? "설명 없음"}</p>
                  </div>
                  <div className="workspace-row-meta">
                    <span className="role-badge">{workspace.myRole}</span>
                    <span>{formatDate(workspace.joinedAt)}</span>
                  </div>
                </Link>
              ))}
            </div>
          ) : (
            <WorkspaceStatePanel state={workspaces} />
          )}
        </section>
      </section>
    </main>
  );
}

function WorkspaceStatePanel({ state }: { state: WorkspaceState }) {
  if (state.status === "loading" || state.status === "idle") {
    return (
      <div className="state-panel">
        <FolderOpen aria-hidden="true" />
        <strong>목록 확인 중</strong>
        <p>워크스페이스 정보를 불러오고 있습니다.</p>
      </div>
    );
  }

  if (state.status === "offline") {
    return (
      <div className="state-panel state-panel-error">
        <AlertCircle aria-hidden="true" />
        <strong>목록을 불러오지 못했습니다</strong>
        <p>{state.error}</p>
      </div>
    );
  }

  return (
    <div className="state-panel">
      <UserRound aria-hidden="true" />
      <strong>가입된 워크스페이스가 없습니다</strong>
      <p>새 워크스페이스를 만들면 이곳에서 바로 확인할 수 있습니다.</p>
    </div>
  );
}

async function fetchHealth(): Promise<HealthState> {
  try {
    const data = await apiFetch<HealthResponse>("/api/health");
    return { status: "online", data, error: null };
  } catch (error) {
    const message =
      error instanceof ApiError ? error.message : "API 연결을 확인하지 못했습니다.";
    return { status: "offline", data: null, error: message };
  }
}

async function fetchWorkspaceList(accessToken: string): Promise<WorkspaceState> {
  try {
    const data = await fetchWorkspaces(accessToken);
    return { status: "ready", data, error: null };
  } catch (error) {
    const message =
      error instanceof ApiError ? error.message : "워크스페이스 목록을 확인하지 못했습니다.";
    return { status: "offline", data: null, error: message };
  }
}

async function fetchProfileState(accessToken: string): Promise<ProfileState> {
  try {
    const profile = await fetchMemberProfile(accessToken);
    return { status: "ready", profile, error: null };
  } catch (error) {
    const message =
      error instanceof ApiError ? error.message : "회원 프로필을 확인하지 못했습니다.";
    return { status: "offline", profile: null, error: message };
  }
}

function authStateFromTokens(tokens: AuthTokens | null): AuthState {
  if (!tokens) {
    return { status: "idle", email: null, message: null, error: null };
  }

  return {
    status: "ready",
    email: tokens.email,
    message: "저장된 인증 세션을 사용 중입니다.",
    error: null,
  };
}

function messageFromError(error: unknown, fallback: string): string {
  return error instanceof ApiError ? error.message : fallback;
}

function formatDate(value: string): string {
  return new Intl.DateTimeFormat("ko-KR", {
    dateStyle: "medium",
  }).format(new Date(value));
}
