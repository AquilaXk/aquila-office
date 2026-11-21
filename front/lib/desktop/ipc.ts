import { isDesktop } from "./env.ts";

export type GitFileChange = {
  path: string;
  status: "untracked" | "modified" | "deleted" | "added" | "renamed" | "changed";
};

export type GitStatusResult = {
  branch: string;
  isClean: boolean;
  files: GitFileChange[];
};

export type OAuthCallbackPayload = {
  url: string;
  code: string | null;
  state: string | null;
};

export type WorkspaceFileChangeEvent = {
  paths: string[];
};

/**
 * Invokes a Tauri desktop IPC command with type safety and web fallback.
 */
export async function invokeDesktop<T>(
  cmd: string,
  args?: Record<string, unknown>,
): Promise<T> {
  if (!isDesktop()) {
    throw new Error(`Desktop IPC command "${cmd}" called in non-desktop environment`);
  }

  const { invoke } = await import("@tauri-apps/api/core");
  return invoke<T>(cmd, args);
}

/**
 * Listens to a Tauri desktop event with type safety and automatic cleanup.
 */
export async function listenDesktop<T>(
  event: string,
  handler: (payload: T) => void,
): Promise<() => void> {
  if (!isDesktop()) {
    return () => {};
  }

  const { listen } = await import("@tauri-apps/api/event");
  const unlisten = await listen<T>(event, (ev) => handler(ev.payload));
  return unlisten;
}

// Typed Desktop Superpower APIs

export async function getWorkspaceGitStatus(path: string): Promise<GitStatusResult> {
  type RustGitStatus = {
    branch: string;
    is_clean: boolean;
    files: Array<{ path: string; status: string }>;
  };

  const raw = await invokeDesktop<RustGitStatus>("get_workspace_git_status", { path });
  return {
    branch: raw.branch,
    isClean: raw.is_clean,
    files: raw.files.map((f) => ({
      path: f.path,
      status: f.status as GitFileChange["status"],
    })),
  };
}

export async function getWorkspaceGitDiff(
  path: string,
  filePath?: string,
): Promise<string> {
  return invokeDesktop<string>("get_workspace_git_diff", {
    path,
    filePath: filePath ?? null,
  });
}

export async function startWatchingWorkspace(path: string): Promise<void> {
  return invokeDesktop<void>("start_watching_workspace", { path });
}

export async function stopWatchingWorkspace(path?: string): Promise<void> {
  return invokeDesktop<void>("stop_watching_workspace", { path: path ?? null });
}

export async function startLoopbackOAuth(): Promise<number> {
  return invokeDesktop<number>("start_loopback_oauth");
}

export async function stopLoopbackOAuth(): Promise<void> {
  return invokeDesktop<void>("stop_loopback_oauth");
}

export type GoogleTokenExchangePayload = {
  code: string;
  clientId: string;
  clientSecret?: string;
  redirectUri: string;
  codeVerifier?: string;
};

export type GoogleTokenResponse = {
  accessToken: string;
  idToken?: string;
  expiresIn?: number;
  tokenType?: string;
  refreshToken?: string;
  scope?: string;
};

export async function saveSecureToken(key: string, token: string): Promise<void> {
  return invokeDesktop<void>("save_secure_token", { key, token });
}

export async function getSecureToken(key: string): Promise<string | null> {
  return invokeDesktop<string | null>("get_secure_token", { key });
}

export async function deleteSecureToken(key: string): Promise<void> {
  return invokeDesktop<void>("delete_secure_token", { key });
}
