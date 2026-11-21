"use client";

import { useRef, type FormEvent, type ReactNode } from "react";
import {
  AlertCircle,
  ArrowLeft,
  GitBranch,
  Hash,
  KeyRound,
  Loader2,
  RefreshCw,
  ShieldCheck,
  Trash2,
  X,
} from "lucide-react";
import { useDialogA11y } from "@/components/ui/useDialogA11y";
import { ModalState } from "@/components/workspace/modals/TaskDetailModals";
import {
  connectionModalTitle,
  type ConnectionModalKind,
  type ConnectionMutationState,
  type GatewayConnectionState,
  type GatewayConnectionTestState,
  type GithubConnectionAction,
  type GithubConnectionState,
  type SlackConnectionAction,
  type SlackConnectionState,
} from "@/components/workspace/workspace-ui-helpers";
import { gatewayConnectionStatusLabel, gatewayStatusLabel } from "@/lib/gateway-utils";
import type { GithubCredential } from "@/lib/github";
import type { SlackIntegration } from "@/lib/slack";

export type ConnectionSettingsModalProps = {
  modal: ConnectionModalKind;
  canManage: boolean;
  slackState: SlackConnectionState;
  slackIntegrations: SlackIntegration[];
  slackMutation: ConnectionMutationState;
  slackAction: SlackConnectionAction | null;
  githubState: GithubConnectionState;
  githubCredentials: GithubCredential[];
  selectedGithubCredential: GithubCredential | null;
  selectedGithubCredentialId: number | null;
  editingGithubCredentialId: number | null;
  githubDisplayNameInput: string;
  githubTokenInput: string;
  githubEditDisplayNameInput: string;
  githubEditTokenInput: string;
  githubCreateMutation: ConnectionMutationState;
  githubActionMutation: ConnectionMutationState;
  githubAction: GithubConnectionAction | null;
  canCreateGithubCredential: boolean;
  canUpdateGithubCredential: boolean;
  gatewayState: GatewayConnectionState;
  gatewayUrlInput: string;
  gatewayTokenInput: string;
  gatewayTestState: GatewayConnectionTestState;
  gatewayBindingMutation: ConnectionMutationState;
  canSubmitGateway: boolean;
  onOpen: (kind: Exclude<ConnectionModalKind, null>) => void;
  onClose: () => void;
  onBack: () => void;
  onReloadSlack: () => void;
  onOpenSlackInstall: () => void;
  onDeleteSlack: (integration: SlackIntegration) => void;
  onReloadGithub: () => void;
  onCreateGithub: (event: FormEvent<HTMLFormElement>) => void;
  onSelectGithubCredential: (credentialId: number) => void;
  onStartGithubEdit: (credential: GithubCredential) => void;
  onCancelGithubEdit: () => void;
  onUpdateGithub: (event: FormEvent<HTMLFormElement>, credential: GithubCredential) => void;
  onDeleteGithub: (credential: GithubCredential) => void;
  onGithubDisplayNameChange: (value: string) => void;
  onGithubTokenChange: (value: string) => void;
  onGithubEditDisplayNameChange: (value: string) => void;
  onGithubEditTokenChange: (value: string) => void;
  onReloadGateway: () => void;
  onGatewayUrlChange: (value: string) => void;
  onGatewayTokenChange: (value: string) => void;
  onTestGateway: () => void;
  onBindGateway: (event: FormEvent<HTMLFormElement>) => void;
};

function ConnectionNotice({
  tone,
  icon,
  children,
}: {
  tone: "success" | "danger";
  icon: ReactNode;
  children: ReactNode;
}) {
  return (
    <div
      className={`workspace-settings-notice workspace-settings-notice-${tone}`}
      role={tone === "danger" ? "alert" : "status"}
    >
      {icon}
      <span>{children}</span>
    </div>
  );
}

export function ConnectionSettingsModal({
  modal,
  canManage,
  slackState,
  slackIntegrations,
  slackMutation,
  slackAction,
  githubState,
  githubCredentials,
  selectedGithubCredential,
  selectedGithubCredentialId,
  editingGithubCredentialId,
  githubDisplayNameInput,
  githubTokenInput,
  githubEditDisplayNameInput,
  githubEditTokenInput,
  githubCreateMutation,
  githubActionMutation,
  githubAction,
  canCreateGithubCredential,
  canUpdateGithubCredential,
  gatewayState,
  gatewayUrlInput,
  gatewayTokenInput,
  gatewayTestState,
  gatewayBindingMutation,
  canSubmitGateway,
  onOpen,
  onClose,
  onBack,
  onReloadSlack,
  onOpenSlackInstall,
  onDeleteSlack,
  onReloadGithub,
  onCreateGithub,
  onSelectGithubCredential,
  onStartGithubEdit,
  onCancelGithubEdit,
  onUpdateGithub,
  onDeleteGithub,
  onGithubDisplayNameChange,
  onGithubTokenChange,
  onGithubEditDisplayNameChange,
  onGithubEditTokenChange,
  onReloadGateway,
  onGatewayUrlChange,
  onGatewayTokenChange,
  onTestGateway,
  onBindGateway,
}: ConnectionSettingsModalProps) {
  const dialogRef = useRef<HTMLElement | null>(null);
  useDialogA11y(Boolean(modal), dialogRef, onClose);

  if (!modal) {
    return null;
  }

  const title = connectionModalTitle(modal);
  const isSlackLoading = slackState.status === "loading";
  const isGithubLoading = githubState.status === "loading";
  const isGatewayLoading = gatewayState.status === "loading";
  const currentGateway =
    gatewayState.status === "ready" || gatewayState.status === "error"
      ? gatewayState.gateway
      : null;

  return (
    <div
      className="workspace-modal-backdrop"
      role="presentation"
      onClick={(event) => {
        if (event.target === event.currentTarget) {
          onClose();
        }
      }}
    >
      <section
        ref={dialogRef}
        className={`workspace-modal workspace-connection-modal workspace-connection-modal-${modal}${
          modal === "selector" ? " workspace-connection-selector-modal" : ""
        }`}
        role="dialog"
        aria-modal="true"
        aria-label="연결 설정"
        tabIndex={-1}
      >
        <header className="workspace-modal-header">
          <div>
            <span className="workspace-detail-label">연동 및 실행 환경</span>
            <h2>{title}</h2>
          </div>
          <div className="workspace-modal-actions">
            {modal !== "selector" ? (
              <button
                className="secondary-action workspace-task-refresh"
                type="button"
                onClick={onBack}
              >
                <ArrowLeft aria-hidden="true" />
                <span>뒤로</span>
              </button>
            ) : null}
            <button
              className="icon-button workspace-modal-close"
              type="button"
              onClick={onClose}
              aria-label="닫기"
            >
              <X aria-hidden="true" />
            </button>
          </div>
        </header>

        <div className="workspace-modal-body">
          {modal === "selector" ? (
            <div className="workspace-connection-grid" aria-label="연동 대상 선택">
              <button
                className="workspace-connection-card"
                type="button"
                aria-label="Slack"
                onClick={() => onOpen("slack")}
              >
                <KeyRound aria-hidden="true" />
                <span>Slack</span>
                <strong>채널 설치 및 연동 목록</strong>
              </button>
              <button
                className="workspace-connection-card"
                type="button"
                aria-label="Discord"
                onClick={() => onOpen("discord")}
              >
                <Hash aria-hidden="true" />
                <span>Discord</span>
                <strong>지원 준비 중</strong>
              </button>
              <button
                className="workspace-connection-card"
                type="button"
                aria-label="GitHub"
                onClick={() => onOpen("github")}
              >
                <GitBranch aria-hidden="true" />
                <span>GitHub</span>
                <strong>토큰 자격 증명</strong>
              </button>
              <button
                className="workspace-connection-card"
                type="button"
                aria-label="AI / OpenClaw"
                onClick={() => onOpen("gateway")}
              >
                <ShieldCheck aria-hidden="true" />
                <span>AI / OpenClaw</span>
                <strong>OpenClaw 게이트웨이 연동</strong>
              </button>
            </div>
          ) : null}

          {modal !== "selector" && !canManage ? (
            <ConnectionNotice tone="danger" icon={<AlertCircle aria-hidden="true" />}>
              관리자 역할만 연결 설정을 변경할 수 있습니다.
            </ConnectionNotice>
          ) : null}

          {modal === "slack" ? (
            <>
              <div className="workspace-modal-summary">
                <strong>Slack 설치</strong>
                <p>설치 진입 시 현재 워크스페이스와 복귀 경로를 저장하고 연동 목록을 갱신합니다.</p>
              </div>
              <div className="workspace-invite-management-toolbar">
                <button
                  className="secondary-action workspace-invite-refresh"
                  type="button"
                  onClick={onOpenSlackInstall}
                  disabled={!canManage || slackMutation.status === "submitting"}
                >
                  {slackMutation.status === "submitting" ? (
                    <Loader2 className="spin" aria-hidden="true" />
                  ) : (
                    <KeyRound aria-hidden="true" />
                  )}
                  <span>
                    {slackMutation.status === "submitting" ? "확인 중" : "Slack 설치 열기"}
                  </span>
                </button>
                <button
                  className="secondary-action workspace-invite-refresh"
                  type="button"
                  onClick={onReloadSlack}
                  disabled={isSlackLoading}
                >
                  <RefreshCw className={isSlackLoading ? "spin" : undefined} aria-hidden="true" />
                  <span>목록 새로고침</span>
                </button>
              </div>
              {slackMutation.status === "error" && slackMutation.message ? (
                <ConnectionNotice tone="danger" icon={<AlertCircle aria-hidden="true" />}>
                  {slackMutation.message}
                </ConnectionNotice>
              ) : null}
              {slackMutation.status === "success" && slackMutation.message ? (
                <ConnectionNotice tone="success" icon={<ShieldCheck aria-hidden="true" />}>
                  {slackMutation.message}
                </ConnectionNotice>
              ) : null}
              {slackState.status === "loading" ? (
                <ModalState
                  icon={<Loader2 className="spin" aria-hidden="true" />}
                  text="Slack 연동 목록 확인 중"
                />
              ) : null}
              {slackState.status === "error" ? (
                <ModalState
                  icon={<AlertCircle aria-hidden="true" />}
                  text={slackState.error}
                  tone="danger"
                />
              ) : null}
              {slackIntegrations.length > 0 ? (
                <div className="workspace-connection-list">
                  {slackIntegrations.map((integration) => {
                    const busy = slackAction?.integrationId === integration.id;
                    return (
                      <article className="workspace-connection-row" key={integration.id}>
                        <div className="workspace-member-main">
                          <strong>{integration.channelName ?? integration.slackChannelId}</strong>
                          <p>
                            팀 {integration.slackTeamId} · 채널 {integration.slackChannelId} ·{" "}
                            {integration.maskedBotToken}
                          </p>
                        </div>
                        <button
                          className="danger-action workspace-github-row-button"
                          type="button"
                          onClick={() => onDeleteSlack(integration)}
                          disabled={!canManage || Boolean(slackAction)}
                        >
                          {busy ? (
                            <Loader2 className="spin" aria-hidden="true" />
                          ) : (
                            <Trash2 aria-hidden="true" />
                          )}
                          <span>{busy ? "삭제 중" : "삭제"}</span>
                        </button>
                      </article>
                    );
                  })}
                </div>
              ) : slackState.status === "ready" ? (
                <div className="workspace-member-empty">
                  <KeyRound aria-hidden="true" />
                  <strong>Slack 연동이 없습니다</strong>
                  <p>설치 완료 후 연결된 채널 목록이 표시됩니다.</p>
                </div>
              ) : null}
            </>
          ) : null}

          {modal === "discord" ? (
            <div className="workspace-member-empty">
              <Hash aria-hidden="true" />
              <strong>Discord 연동 준비 중</strong>
              <p>지원 일정이 열리면 동일한 연결 설정 화면에서 제공됩니다.</p>
            </div>
          ) : null}

          {modal === "github" ? (
            <>
              <form className="workspace-connection-form" onSubmit={onCreateGithub}>
                <div className="workspace-gateway-form-grid">
                  <label className="form-field">
                    <span>
                      <strong>표시 이름</strong>
                      <small>{githubDisplayNameInput.length}/100</small>
                    </span>
                    <input
                      maxLength={100}
                      value={githubDisplayNameInput}
                      onChange={(event) => onGithubDisplayNameChange(event.target.value)}
                      disabled={!canManage || githubCreateMutation.status === "submitting"}
                    />
                  </label>
                  <label className="form-field">
                    <span>
                      <strong>토큰</strong>
                      <small>필수</small>
                    </span>
                    <input
                      type="password"
                      value={githubTokenInput}
                      onChange={(event) => onGithubTokenChange(event.target.value)}
                      disabled={!canManage || githubCreateMutation.status === "submitting"}
                    />
                  </label>
                </div>
                {githubCreateMutation.status === "error" && githubCreateMutation.message ? (
                  <ConnectionNotice tone="danger" icon={<AlertCircle aria-hidden="true" />}>
                    {githubCreateMutation.message}
                  </ConnectionNotice>
                ) : null}
                {githubCreateMutation.status === "success" && githubCreateMutation.message ? (
                  <ConnectionNotice tone="success" icon={<ShieldCheck aria-hidden="true" />}>
                    {githubCreateMutation.message}
                  </ConnectionNotice>
                ) : null}
                <div className="workspace-settings-actions">
                  <button
                    className="primary-action primary-action-active"
                    type="submit"
                    disabled={!canCreateGithubCredential}
                  >
                    {githubCreateMutation.status === "submitting" ? (
                      <Loader2 className="spin" aria-hidden="true" />
                    ) : (
                      <KeyRound aria-hidden="true" />
                    )}
                    <span>
                      {githubCreateMutation.status === "submitting" ? "등록 중" : "자격 증명 등록"}
                    </span>
                  </button>
                </div>
              </form>

              <div className="workspace-invite-management-toolbar">
                <strong>GitHub 자격 증명</strong>
                <button
                  className="secondary-action workspace-invite-refresh"
                  type="button"
                  onClick={onReloadGithub}
                  disabled={isGithubLoading}
                >
                  <RefreshCw className={isGithubLoading ? "spin" : undefined} aria-hidden="true" />
                  <span>목록 새로고침</span>
                </button>
              </div>
              {githubActionMutation.status === "error" && githubActionMutation.message ? (
                <ConnectionNotice tone="danger" icon={<AlertCircle aria-hidden="true" />}>
                  {githubActionMutation.message}
                </ConnectionNotice>
              ) : null}
              {githubActionMutation.status === "success" && githubActionMutation.message ? (
                <ConnectionNotice tone="success" icon={<ShieldCheck aria-hidden="true" />}>
                  {githubActionMutation.message}
                </ConnectionNotice>
              ) : null}
              {githubState.status === "loading" ? (
                <ModalState
                  icon={<Loader2 className="spin" aria-hidden="true" />}
                  text="GitHub 자격 증명 확인 중"
                />
              ) : null}
              {githubState.status === "error" ? (
                <ModalState
                  icon={<AlertCircle aria-hidden="true" />}
                  text={githubState.error}
                  tone="danger"
                />
              ) : null}
              {selectedGithubCredential ? (
                <section className="workspace-modal-summary" aria-label="선택된 자격 증명">
                  <strong>선택된 자격 증명</strong>
                  <p>
                    {selectedGithubCredential.displayName} ·{" "}
                    <span className="workspace-github-token">
                      {selectedGithubCredential.maskedToken}
                    </span>
                  </p>
                </section>
              ) : null}
              {githubCredentials.length > 0 ? (
                <div className="workspace-connection-list">
                  {githubCredentials.map((credential) => {
                    const busy = githubAction?.credentialId === credential.credentialId;
                    const selected = selectedGithubCredentialId === credential.credentialId;
                    const editing = editingGithubCredentialId === credential.credentialId;

                    return (
                      <article className="workspace-connection-row" key={credential.credentialId}>
                        <button
                          className={`workspace-connection-select${selected ? " workspace-connection-select-active" : ""}`}
                          type="button"
                          aria-pressed={selected}
                          onClick={() => onSelectGithubCredential(credential.credentialId)}
                        >
                          <strong>{credential.displayName}</strong>
                          <span>{credential.maskedToken}</span>
                        </button>
                        <div className="workspace-github-row-actions">
                          <button
                            className="secondary-action workspace-github-row-button"
                            type="button"
                            onClick={() => onStartGithubEdit(credential)}
                            disabled={!canManage || Boolean(githubAction)}
                          >
                            <KeyRound aria-hidden="true" />
                            <span>수정</span>
                          </button>
                          <button
                            className="danger-action workspace-github-row-button"
                            type="button"
                            onClick={() => onDeleteGithub(credential)}
                            disabled={!canManage || Boolean(githubAction)}
                          >
                            {busy && githubAction?.type === "delete" ? (
                              <Loader2 className="spin" aria-hidden="true" />
                            ) : (
                              <Trash2 aria-hidden="true" />
                            )}
                            <span>
                              {busy && githubAction?.type === "delete" ? "삭제 중" : "삭제"}
                            </span>
                          </button>
                        </div>
                        {editing ? (
                          <form
                            className="workspace-github-edit-form"
                            onSubmit={(event) => onUpdateGithub(event, credential)}
                          >
                            <label className="form-field workspace-github-edit-field">
                              <span>
                                <strong>표시 이름</strong>
                                <small>필수</small>
                              </span>
                              <input
                                maxLength={100}
                                value={githubEditDisplayNameInput}
                                onChange={(event) =>
                                  onGithubEditDisplayNameChange(event.target.value)
                                }
                                disabled={Boolean(githubAction)}
                              />
                            </label>
                            <label className="form-field workspace-github-edit-field">
                              <span>
                                <strong>토큰</strong>
                                <small>변경 시 입력</small>
                              </span>
                              <input
                                type="password"
                                value={githubEditTokenInput}
                                onChange={(event) => onGithubEditTokenChange(event.target.value)}
                                disabled={Boolean(githubAction)}
                              />
                            </label>
                            <div className="workspace-github-row-actions">
                              <button
                                className="primary-action primary-action-active workspace-github-row-button"
                                type="submit"
                                disabled={!canUpdateGithubCredential}
                              >
                                {busy && githubAction?.type === "update" ? (
                                  <Loader2 className="spin" aria-hidden="true" />
                                ) : (
                                  <ShieldCheck aria-hidden="true" />
                                )}
                                <span>
                                  {busy && githubAction?.type === "update" ? "저장 중" : "저장"}
                                </span>
                              </button>
                              <button
                                className="secondary-action workspace-github-row-button"
                                type="button"
                                onClick={onCancelGithubEdit}
                                disabled={Boolean(githubAction)}
                              >
                                <X aria-hidden="true" />
                                <span>취소</span>
                              </button>
                            </div>
                          </form>
                        ) : null}
                      </article>
                    );
                  })}
                </div>
              ) : githubState.status === "ready" ? (
                <div className="workspace-member-empty">
                  <GitBranch aria-hidden="true" />
                  <strong>등록된 GitHub 자격 증명이 없습니다</strong>
                  <p>에이전트가 저장소 작업을 수행할 수 있도록 토큰을 등록해 주세요.</p>
                </div>
              ) : null}
            </>
          ) : null}

          {modal === "gateway" ? (
            <>
              <form className="workspace-connection-form workspace-gateway-form" onSubmit={onBindGateway}>
                <div className="workspace-gateway-form-grid">
                  <label className="form-field">
                    <span>
                      <strong>게이트웨이 URL</strong>
                      <small>http/https</small>
                    </span>
                    <input
                      aria-label="Gateway URL"
                      value={gatewayUrlInput}
                      onChange={(event) => onGatewayUrlChange(event.target.value)}
                      disabled={!canManage || gatewayBindingMutation.status === "submitting"}
                    />
                  </label>
                  <label className="form-field">
                    <span>
                      <strong>토큰</strong>
                      <small>필수</small>
                    </span>
                    <input
                      aria-label="Token"
                      type="password"
                      value={gatewayTokenInput}
                      onChange={(event) => onGatewayTokenChange(event.target.value)}
                      disabled={!canManage || gatewayBindingMutation.status === "submitting"}
                    />
                  </label>
                </div>
                <div className="workspace-settings-actions workspace-gateway-actions">
                  <button
                    className="secondary-action workspace-invite-refresh"
                    type="button"
                    onClick={onReloadGateway}
                    disabled={isGatewayLoading}
                  >
                    <RefreshCw
                      className={isGatewayLoading ? "spin" : undefined}
                      aria-hidden="true"
                    />
                    <span>상태 새로고침</span>
                  </button>
                  <button
                    className="secondary-action workspace-invite-refresh"
                    type="button"
                    onClick={onTestGateway}
                    disabled={!canSubmitGateway}
                  >
                    {gatewayTestState.status === "submitting" ? (
                      <Loader2 className="spin" aria-hidden="true" />
                    ) : (
                      <ShieldCheck aria-hidden="true" />
                    )}
                    <span>
                      {gatewayTestState.status === "submitting" ? "테스트 중" : "연결 테스트"}
                    </span>
                  </button>
                  <button
                    className="primary-action primary-action-active"
                    type="submit"
                    disabled={!canSubmitGateway}
                  >
                    {gatewayBindingMutation.status === "submitting" ? (
                      <Loader2 className="spin" aria-hidden="true" />
                    ) : (
                      <KeyRound aria-hidden="true" />
                    )}
                    <span>
                      {gatewayBindingMutation.status === "submitting"
                        ? "등록 중"
                        : "OpenClaw 연결 등록"}
                    </span>
                  </button>
                </div>
                {gatewayTestState.status === "error" ? (
                  <ConnectionNotice tone="danger" icon={<AlertCircle aria-hidden="true" />}>
                    {gatewayTestState.message}
                  </ConnectionNotice>
                ) : null}
                {gatewayTestState.status === "success" ? (
                  <ConnectionNotice tone="success" icon={<ShieldCheck aria-hidden="true" />}>
                    {gatewayTestState.message}
                  </ConnectionNotice>
                ) : null}
                {gatewayBindingMutation.status === "error" && gatewayBindingMutation.message ? (
                  <ConnectionNotice tone="danger" icon={<AlertCircle aria-hidden="true" />}>
                    {gatewayBindingMutation.message}
                  </ConnectionNotice>
                ) : null}
                {gatewayBindingMutation.status === "success" && gatewayBindingMutation.message ? (
                  <ConnectionNotice tone="success" icon={<ShieldCheck aria-hidden="true" />}>
                    {gatewayBindingMutation.message}
                  </ConnectionNotice>
                ) : null}
              </form>

              {gatewayState.status === "error" ? (
                <ModalState
                  icon={<AlertCircle aria-hidden="true" />}
                  text={gatewayState.error}
                  tone="danger"
                />
              ) : null}
              <div className="workspace-gateway-status-list" aria-label="게이트웨이 상태 요약">
                <div>
                  <span>연결 상태</span>
                  <strong>
                    {currentGateway ? gatewayStatusLabel(currentGateway.status) : "상태 없음"}
                  </strong>
                </div>
                <div>
                  <span>URL</span>
                  <strong>{(currentGateway?.gatewayUrl ?? gatewayUrlInput) || "-"}</strong>
                </div>
                <div>
                  <span>최근 응답</span>
                  <strong>
                    {currentGateway?.lastStatus
                      ? gatewayConnectionStatusLabel(currentGateway.lastStatus)
                      : "-"}
                  </strong>
                </div>
                <div>
                  <span>테스트 결과</span>
                  <strong>
                    {gatewayTestState.status === "success"
                      ? gatewayConnectionStatusLabel(gatewayTestState.result.status)
                      : "-"}
                  </strong>
                </div>
              </div>
            </>
          ) : null}
        </div>
      </section>
    </div>
  );
}
