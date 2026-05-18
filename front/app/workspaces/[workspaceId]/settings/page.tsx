"use client";

import {
  AlertCircle,
  ArrowLeft,
  Building2,
  CalendarDays,
  CheckCircle2,
  Copy,
  GitBranch,
  Hash,
  KeyRound,
  Link2,
  Loader2,
  Mail,
  RefreshCw,
  Save,
  ShieldCheck,
  Trash2,
  UserRound,
  Users,
  WifiOff,
  X,
} from "lucide-react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import {
  use,
  useCallback,
  useEffect,
  useMemo,
  useState,
  type FormEvent,
  type ReactNode,
} from "react";
import { ApiError } from "@/lib/api";
import { resolveAccessToken } from "@/lib/auth";
import {
  createWorkspaceGatewayBinding,
  fetchWorkspaceGatewayStatus,
  testWorkspaceGatewayConnection,
  type WorkspaceGatewayConnectionTest,
  type WorkspaceGatewayStatus,
} from "@/lib/gateway";
import {
  gatewayConnectionStatusLabel,
  gatewayStatusLabel,
  normalizeGatewayForm,
} from "@/lib/gateway-utils";
import {
  createGithubCredential,
  deleteGithubCredential,
  fetchGithubCredentials,
  updateGithubCredential,
  type GithubCredential,
} from "@/lib/github";
import {
  createIntegrationSettingsActions,
  createIntegrationSettingsOverview,
  type IntegrationSettingsAction,
} from "@/lib/integration-settings";
import {
  createWorkspaceInvite,
  deleteWorkspaceInvite,
  extendWorkspaceInvite,
  fetchWorkspaceInvites,
  type WorkspaceInvite,
  type WorkspaceInviteManagement,
  type WorkspaceInviteManagementStatus,
} from "@/lib/invites";
import {
  createSlackChannelMapping,
  createSlackIntegration,
  deleteSlackIntegration,
  fetchSlackChannelMappings,
  fetchSlackInstallUrl,
  fetchSlackIntegrations,
  updateSlackIntegration,
  type SlackChannelMapping,
  type SlackIntegration,
} from "@/lib/slack";
import {
  changeWorkspaceMemberRole,
  deleteWorkspace,
  fetchWorkspaceDetail,
  fetchWorkspaceMembers,
  removeWorkspaceMember,
  updateWorkspace,
  type WorkspaceDetail,
  type WorkspaceMember,
  type WorkspaceMemberRole,
} from "@/lib/workspaces";

const NAME_MAX_LENGTH = 100;
const DESCRIPTION_MAX_LENGTH = 500;
const EMPTY_GITHUB_CREDENTIALS: GithubCredential[] = [];
const EMPTY_SLACK_INTEGRATIONS: SlackIntegration[] = [];
const EMPTY_SLACK_CHANNEL_MAPPINGS: SlackChannelMapping[] = [];
const SHOW_CONNECTION_SETTINGS = false;

type WorkspaceSettingsState =
  | { status: "loading"; workspace: null; members: []; error: null }
  | { status: "ready"; workspace: WorkspaceDetail; members: WorkspaceMember[]; error: null }
  | { status: "error" | "offline"; workspace: null; members: []; error: string };

type MutationState =
  | { status: "idle"; message: null }
  | { status: "submitting"; message: null }
  | { status: "success"; message: string }
  | { status: "error"; message: string };

type MemberAction =
  | { type: "role"; memberId: number }
  | { type: "remove"; memberId: number };

type InviteLogState =
  | { status: "idle"; invites: []; error: null }
  | { status: "loading"; invites: []; error: null }
  | { status: "ready"; invites: WorkspaceInviteManagement[]; error: null }
  | { status: "error"; invites: []; error: string };

type GithubCredentialState =
  | { status: "idle"; credentials: []; error: null }
  | { status: "loading"; credentials: []; error: null }
  | { status: "ready"; credentials: GithubCredential[]; error: null }
  | { status: "error"; credentials: []; error: string };

type SlackIntegrationState =
  | { status: "idle"; integrations: []; error: null }
  | { status: "loading"; integrations: []; error: null }
  | { status: "ready"; integrations: SlackIntegration[]; error: null }
  | { status: "error"; integrations: []; error: string };

type SlackChannelMappingState =
  | { status: "idle"; mappings: []; error: null }
  | { status: "loading"; mappings: []; error: null }
  | { status: "ready"; mappings: SlackChannelMapping[]; error: null }
  | { status: "error"; mappings: []; error: string };

type GatewayStatusState =
  | { status: "idle"; gateway: null; error: null }
  | { status: "loading"; gateway: null; error: null }
  | { status: "ready"; gateway: WorkspaceGatewayStatus; error: null }
  | { status: "error"; gateway: null; error: string };

type GatewayConnectionTestState =
  | { status: "idle"; result: null; message: null }
  | { status: "submitting"; result: null; message: null }
  | { status: "success"; result: WorkspaceGatewayConnectionTest; message: string }
  | { status: "error"; result: null; message: string };

type InviteStatusFilter = WorkspaceInviteManagementStatus | "ALL";
type WorkspaceSettingsSection = "workspace" | "members" | "invite" | "invites";
type InviteCreateMode = "email" | "link";

type GithubCredentialAction =
  | { type: "update"; credentialId: number }
  | { type: "delete"; credentialId: number };

type SlackIntegrationAction =
  | { type: "update"; integrationId: number }
  | { type: "delete"; integrationId: number };

const INVITE_STATUS_FILTERS: { value: InviteStatusFilter; label: string }[] = [
  { value: "PENDING", label: "대기" },
  { value: "ACCEPTED", label: "수락" },
  { value: "EXPIRED", label: "만료" },
  { value: "REVOKED", label: "취소" },
  { value: "ALL", label: "전체" },
];

const workspaceSettingsSections: {
  id: WorkspaceSettingsSection;
  label: string;
  ariaLabel: string;
}[] = [
  { id: "workspace", label: "Workspace", ariaLabel: "워크스페이스 정보" },
  { id: "members", label: "Members", ariaLabel: "멤버 관리" },
  { id: "invite", label: "Invite", ariaLabel: "초대 생성" },
  { id: "invites", label: "Invites", ariaLabel: "초대 목록" },
];

export default function WorkspaceSettingsPage({
  params,
}: {
  params: Promise<{ workspaceId: string }>;
}) {
  const { workspaceId } = use(params);
  const numericWorkspaceId = useMemo(() => Number(workspaceId), [workspaceId]);
  const router = useRouter();
  const [state, setState] = useState<WorkspaceSettingsState>({
    status: "loading",
    workspace: null,
    members: [],
    error: null,
  });
  const [activeSettingsSection, setActiveSettingsSection] =
    useState<WorkspaceSettingsSection>("workspace");
  const [nameInput, setNameInput] = useState("");
  const [descriptionInput, setDescriptionInput] = useState("");
  const [updateState, setUpdateState] = useState<MutationState>({
    status: "idle",
    message: null,
  });
  const [deleteConfirmation, setDeleteConfirmation] = useState("");
  const [deleteWorkspaceModalOpen, setDeleteWorkspaceModalOpen] = useState(false);
  const [deleteState, setDeleteState] = useState<MutationState>({
    status: "idle",
    message: null,
  });
  const [memberAction, setMemberAction] = useState<MemberAction | null>(null);
  const [memberState, setMemberState] = useState<MutationState>({
    status: "idle",
    message: null,
  });
  const [inviteLogState, setInviteLogState] = useState<InviteLogState>({
    status: "idle",
    invites: [],
    error: null,
  });
  const [inviteCreateMode, setInviteCreateMode] = useState<InviteCreateMode>("email");
  const [inviteEmailInput, setInviteEmailInput] = useState("");
  const [inviteRole, setInviteRole] = useState<WorkspaceMemberRole>("MEMBER");
  const [inviteExpiresInDays, setInviteExpiresInDays] = useState("7");
  const [inviteCreateState, setInviteCreateState] = useState<MutationState>({
    status: "idle",
    message: null,
  });
  const [createdInvite, setCreatedInvite] = useState<WorkspaceInvite | null>(null);
  const [copiedInviteToken, setCopiedInviteToken] = useState<string | null>(null);
  const [inviteFilter, setInviteFilter] = useState<InviteStatusFilter>("PENDING");
  const [inviteActionId, setInviteActionId] = useState<number | null>(null);
  const [inviteActionState, setInviteActionState] = useState<MutationState>({
    status: "idle",
    message: null,
  });
  const [githubCredentialState, setGithubCredentialState] = useState<GithubCredentialState>({
    status: "idle",
    credentials: [],
    error: null,
  });
  const [githubDisplayNameInput, setGithubDisplayNameInput] = useState("");
  const [githubTokenInput, setGithubTokenInput] = useState("");
  const [githubCreateState, setGithubCreateState] = useState<MutationState>({
    status: "idle",
    message: null,
  });
  const [githubAction, setGithubAction] = useState<GithubCredentialAction | null>(null);
  const [editingGithubCredentialId, setEditingGithubCredentialId] = useState<number | null>(null);
  const [githubEditDisplayName, setGithubEditDisplayName] = useState("");
  const [githubEditToken, setGithubEditToken] = useState("");
  const [githubActionState, setGithubActionState] = useState<MutationState>({
    status: "idle",
    message: null,
  });
  const [slackIntegrationState, setSlackIntegrationState] = useState<SlackIntegrationState>({
    status: "idle",
    integrations: [],
    error: null,
  });
  const [slackTeamIdInput, setSlackTeamIdInput] = useState("");
  const [slackChannelIdInput, setSlackChannelIdInput] = useState("");
  const [slackChannelNameInput, setSlackChannelNameInput] = useState("");
  const [slackBotTokenInput, setSlackBotTokenInput] = useState("");
  const [slackInstallState, setSlackInstallState] = useState<MutationState>({
    status: "idle",
    message: null,
  });
  const [slackCreateState, setSlackCreateState] = useState<MutationState>({
    status: "idle",
    message: null,
  });
  const [slackAction, setSlackAction] = useState<SlackIntegrationAction | null>(null);
  const [editingSlackIntegrationId, setEditingSlackIntegrationId] = useState<number | null>(null);
  const [slackEditTeamId, setSlackEditTeamId] = useState("");
  const [slackEditChannelId, setSlackEditChannelId] = useState("");
  const [slackEditChannelName, setSlackEditChannelName] = useState("");
  const [slackEditBotToken, setSlackEditBotToken] = useState("");
  const [slackActionState, setSlackActionState] = useState<MutationState>({
    status: "idle",
    message: null,
  });
  const [slackChannelMappingState, setSlackChannelMappingState] =
    useState<SlackChannelMappingState>({
      status: "idle",
      mappings: [],
      error: null,
    });
  const [slackMappingTeamIdInput, setSlackMappingTeamIdInput] = useState("");
  const [slackMappingChannelIdInput, setSlackMappingChannelIdInput] = useState("");
  const [slackMappingChannelNameInput, setSlackMappingChannelNameInput] = useState("");
  const [slackMappingBotTokenInput, setSlackMappingBotTokenInput] = useState("");
  const [slackMappingCreateState, setSlackMappingCreateState] = useState<MutationState>({
    status: "idle",
    message: null,
  });
  const [gatewayStatusState, setGatewayStatusState] = useState<GatewayStatusState>({
    status: "idle",
    gateway: null,
    error: null,
  });
  const [gatewayUrlInput, setGatewayUrlInput] = useState("");
  const [gatewayTokenInput, setGatewayTokenInput] = useState("");
  const [gatewayConnectionTestState, setGatewayConnectionTestState] =
    useState<GatewayConnectionTestState>({
      status: "idle",
      result: null,
      message: null,
    });
  const [gatewayBindingState, setGatewayBindingState] = useState<MutationState>({
    status: "idle",
    message: null,
  });

  const fetchSettings = useCallback(async () => {
    if (!isValidWorkspaceId(numericWorkspaceId)) {
      return {
        status: "error",
        workspace: null,
        members: [],
        error: "올바르지 않은 워크스페이스입니다.",
      } satisfies WorkspaceSettingsState;
    }

    try {
      const [workspace, members] = await Promise.all([
        fetchWorkspaceDetail(resolveAccessToken(), numericWorkspaceId),
        fetchWorkspaceMembers(resolveAccessToken(), numericWorkspaceId),
      ]);
      return { status: "ready", workspace, members, error: null } satisfies WorkspaceSettingsState;
    } catch (error) {
      return toFailureState(error, "워크스페이스 설정을 확인하지 못했습니다.");
    }
  }, [numericWorkspaceId]);

  const fetchInviteLog = useCallback(
    async (status: WorkspaceInviteManagementStatus | undefined = "PENDING") => {
      try {
        const invites = await fetchWorkspaceInvites(
          resolveAccessToken(),
          numericWorkspaceId,
          status,
        );
        return { status: "ready", invites, error: null } satisfies InviteLogState;
      } catch (error) {
        return {
          status: "error",
          invites: [],
          error: messageFromError(error, "초대 목록을 확인하지 못했습니다."),
        } satisfies InviteLogState;
      }
    },
    [numericWorkspaceId],
  );

  const fetchGithubCredentialList = useCallback(async () => {
    try {
      const credentials = await fetchGithubCredentials(
        resolveAccessToken(),
        numericWorkspaceId,
      );
      return { status: "ready", credentials, error: null } satisfies GithubCredentialState;
    } catch (error) {
      return {
        status: "error",
        credentials: [],
        error: messageFromError(error, "GitHub 자격 증명 목록을 확인하지 못했습니다."),
      } satisfies GithubCredentialState;
    }
  }, [numericWorkspaceId]);

  const fetchSlackIntegrationList = useCallback(async () => {
    try {
      const integrations = await fetchSlackIntegrations(
        resolveAccessToken(),
        numericWorkspaceId,
      );
      return { status: "ready", integrations, error: null } satisfies SlackIntegrationState;
    } catch (error) {
      return {
        status: "error",
        integrations: [],
        error: messageFromError(error, "Slack 연동 목록을 확인하지 못했습니다."),
      } satisfies SlackIntegrationState;
    }
  }, [numericWorkspaceId]);

  const fetchSlackChannelMappingList = useCallback(async () => {
    try {
      const mappings = await fetchSlackChannelMappings(
        resolveAccessToken(),
        numericWorkspaceId,
      );
      return { status: "ready", mappings, error: null } satisfies SlackChannelMappingState;
    } catch (error) {
      return {
        status: "error",
        mappings: [],
        error: messageFromError(error, "Slack 채널 매핑 목록을 확인하지 못했습니다."),
      } satisfies SlackChannelMappingState;
    }
  }, [numericWorkspaceId]);

  const fetchGatewayStatus = useCallback(async () => {
    try {
      const gateway = await fetchWorkspaceGatewayStatus(
        resolveAccessToken(),
        numericWorkspaceId,
      );
      return { status: "ready", gateway, error: null } satisfies GatewayStatusState;
    } catch (error) {
      return {
        status: "error",
        gateway: null,
        error: messageFromError(error, "gateway 상태를 확인하지 못했습니다."),
      } satisfies GatewayStatusState;
    }
  }, [numericWorkspaceId]);

  const reloadSettings = useCallback(async () => {
    setState({ status: "loading", workspace: null, members: [], error: null });
    const nextState = await fetchSettings();
    setState(nextState);
    if (nextState.status === "ready") {
      setNameInput(nextState.workspace.name);
      setDescriptionInput(nextState.workspace.description ?? "");
      setDeleteConfirmation("");
    }
  }, [fetchSettings]);

  useEffect(() => {
    let ignore = false;

    async function loadInitialSettings() {
      const nextState = await fetchSettings();
      if (!ignore) {
        setState(nextState);
        if (nextState.status === "ready") {
          setNameInput(nextState.workspace.name);
          setDescriptionInput(nextState.workspace.description ?? "");
          setDeleteConfirmation("");
        }
      }
    }

    void loadInitialSettings();

    return () => {
      ignore = true;
    };
  }, [fetchSettings]);

  useEffect(() => {
    let ignore = false;

    async function loadInitialInviteLog() {
      if (state.status !== "ready" || state.workspace.myRole !== "ADMIN") {
        return;
      }

      await Promise.resolve();
      if (!ignore) {
        setInviteLogState({ status: "loading", invites: [], error: null });
      }

      const nextState = await fetchInviteLog(toInviteStatusParam(inviteFilter));
      if (!ignore) {
        setInviteLogState(nextState);
      }
    }

    void loadInitialInviteLog();

    return () => {
      ignore = true;
    };
  }, [fetchInviteLog, inviteFilter, state]);

  useEffect(() => {
    let ignore = false;

    async function loadInitialGithubCredentials() {
      if (!SHOW_CONNECTION_SETTINGS || state.status !== "ready" || state.workspace.myRole !== "ADMIN") {
        setGithubCredentialState({ status: "idle", credentials: [], error: null });
        return;
      }

      await Promise.resolve();
      if (!ignore) {
        setGithubCredentialState({ status: "loading", credentials: [], error: null });
      }

      const nextState = await fetchGithubCredentialList();
      if (!ignore) {
        setGithubCredentialState(nextState);
      }
    }

    void loadInitialGithubCredentials();

    return () => {
      ignore = true;
    };
  }, [fetchGithubCredentialList, state]);

  useEffect(() => {
    let ignore = false;

    async function loadInitialSlackIntegrations() {
      if (!SHOW_CONNECTION_SETTINGS || state.status !== "ready" || state.workspace.myRole !== "ADMIN") {
        setSlackIntegrationState({ status: "idle", integrations: [], error: null });
        return;
      }

      await Promise.resolve();
      if (!ignore) {
        setSlackIntegrationState({ status: "loading", integrations: [], error: null });
      }

      const nextState = await fetchSlackIntegrationList();
      if (!ignore) {
        setSlackIntegrationState(nextState);
      }
    }

    void loadInitialSlackIntegrations();

    return () => {
      ignore = true;
    };
  }, [fetchSlackIntegrationList, state]);

  useEffect(() => {
    let ignore = false;

    async function loadInitialSlackChannelMappings() {
      if (!SHOW_CONNECTION_SETTINGS || state.status !== "ready" || state.workspace.myRole !== "ADMIN") {
        setSlackChannelMappingState({ status: "idle", mappings: [], error: null });
        return;
      }

      await Promise.resolve();
      if (!ignore) {
        setSlackChannelMappingState({ status: "loading", mappings: [], error: null });
      }

      const nextState = await fetchSlackChannelMappingList();
      if (!ignore) {
        setSlackChannelMappingState(nextState);
      }
    }

    void loadInitialSlackChannelMappings();

    return () => {
      ignore = true;
    };
  }, [fetchSlackChannelMappingList, state]);

  useEffect(() => {
    let ignore = false;

    async function loadGatewayStatus() {
      if (!SHOW_CONNECTION_SETTINGS || state.status !== "ready" || state.workspace.myRole !== "ADMIN") {
        setGatewayStatusState({ status: "idle", gateway: null, error: null });
        return;
      }

      await Promise.resolve();
      if (!ignore) {
        setGatewayStatusState({ status: "loading", gateway: null, error: null });
      }

      const nextState = await fetchGatewayStatus();
      if (!ignore) {
        setGatewayStatusState(nextState);
        if (nextState.status === "ready" && nextState.gateway.gatewayUrl) {
          setGatewayUrlInput((previous) => previous || nextState.gateway.gatewayUrl || "");
        }
      }
    }

    void loadGatewayStatus();

    return () => {
      ignore = true;
    };
  }, [fetchGatewayStatus, state]);

  const workspace = state.workspace;
  const members = state.status === "ready" ? state.members : [];
  const isAdmin = workspace?.myRole === "ADMIN";
  const adminCount = members.filter((member) => member.role === "ADMIN").length;
  const trimmedName = nameInput.trim();
  const trimmedDescription = descriptionInput.trim();
  const trimmedInviteEmail = inviteEmailInput.trim();
  const parsedInviteExpiresInDays = parseInviteExpiresInDays(inviteExpiresInDays);
  const isUpdatingWorkspace = updateState.status === "submitting";
  const isDeletingWorkspace = deleteState.status === "submitting";
  const canManageWorkspace = state.status === "ready" && isAdmin;
  const canUpdateWorkspace =
    canManageWorkspace && trimmedName.length > 0 && !isUpdatingWorkspace;
  const canDeleteWorkspace =
    canManageWorkspace &&
    deleteConfirmation === workspace?.name &&
    !isDeletingWorkspace;
  const canManageMembers = state.status === "ready" && isAdmin;
  const isInviteLogLoading = inviteLogState.status === "loading";
  const isCreatingInvite = inviteCreateState.status === "submitting";
  const hasInviteAction = inviteActionId !== null;
  const canCreateInvite =
    isAdmin &&
    trimmedInviteEmail.length > 0 &&
    parsedInviteExpiresInDays !== null &&
    !isCreatingInvite &&
    !hasInviteAction;
  const isGithubCredentialLoading = githubCredentialState.status === "loading";
  const isSlackIntegrationLoading = slackIntegrationState.status === "loading";
  const isGatewayStatusLoading = gatewayStatusState.status === "loading";
  const githubCredentials =
    githubCredentialState.status === "ready"
      ? githubCredentialState.credentials
      : EMPTY_GITHUB_CREDENTIALS;
  const slackIntegrations =
    slackIntegrationState.status === "ready"
      ? slackIntegrationState.integrations
      : EMPTY_SLACK_INTEGRATIONS;
  const slackChannelMappings =
    slackChannelMappingState.status === "ready"
      ? slackChannelMappingState.mappings
      : EMPTY_SLACK_CHANNEL_MAPPINGS;
  const gatewayStatus =
    gatewayStatusState.status === "ready" ? gatewayStatusState.gateway : null;
  const integrationSettingsOverview = createIntegrationSettingsOverview({
    githubCredentials,
    slackIntegrations,
    slackChannelMappings,
    gatewayStatus,
  });
  const integrationSettingsActions = createIntegrationSettingsActions({
    githubCredentials,
    slackIntegrations,
    slackChannelMappings,
    gatewayStatus,
  });
  const normalizedGatewayForm = useMemo(
    () =>
      normalizeGatewayForm({
        gatewayUrl: gatewayUrlInput,
        token: gatewayTokenInput,
        validateConnection: true,
      }),
    [gatewayTokenInput, gatewayUrlInput],
  );
  const isTestingGateway = gatewayConnectionTestState.status === "submitting";
  const isBindingGateway = gatewayBindingState.status === "submitting";
  const canSubmitGateway =
    isAdmin && normalizedGatewayForm.isValid && !isTestingGateway && !isBindingGateway;
  const trimmedGithubDisplayName = githubDisplayNameInput.trim();
  const trimmedGithubToken = githubTokenInput.trim();
  const isCreatingGithubCredential = githubCreateState.status === "submitting";
  const hasGithubCredentialAction = githubAction !== null;
  const canCreateGithubCredential =
    isAdmin &&
    trimmedGithubDisplayName.length > 0 &&
    trimmedGithubToken.length > 0 &&
    !isCreatingGithubCredential &&
    !hasGithubCredentialAction;
  const trimmedSlackTeamId = slackTeamIdInput.trim();
  const trimmedSlackChannelId = slackChannelIdInput.trim();
  const trimmedSlackChannelName = slackChannelNameInput.trim();
  const trimmedSlackBotToken = slackBotTokenInput.trim();
  const isOpeningSlackInstall = slackInstallState.status === "submitting";
  const isCreatingSlackIntegration = slackCreateState.status === "submitting";
  const hasSlackIntegrationAction = slackAction !== null;
  const isSlackChannelMappingLoading = slackChannelMappingState.status === "loading";
  const trimmedSlackMappingTeamId = slackMappingTeamIdInput.trim();
  const trimmedSlackMappingChannelId = slackMappingChannelIdInput.trim();
  const trimmedSlackMappingChannelName = slackMappingChannelNameInput.trim();
  const trimmedSlackMappingBotToken = slackMappingBotTokenInput.trim();
  const isCreatingSlackChannelMapping = slackMappingCreateState.status === "submitting";
  const canCreateSlackIntegration =
    isAdmin &&
    trimmedSlackTeamId.length > 0 &&
    trimmedSlackChannelId.length > 0 &&
    trimmedSlackBotToken.length > 0 &&
    !isCreatingSlackIntegration &&
    !hasSlackIntegrationAction;
  const canCreateSlackChannelMapping =
    isAdmin &&
    trimmedSlackMappingTeamId.length > 0 &&
    trimmedSlackMappingChannelId.length > 0 &&
    trimmedSlackMappingBotToken.length > 0 &&
    !isCreatingSlackChannelMapping;

  const reloadInviteLog = async () => {
    if (!isAdmin) {
      return;
    }

    setInviteLogState({ status: "loading", invites: [], error: null });
    setInviteActionState({ status: "idle", message: null });
    setInviteLogState(await fetchInviteLog(toInviteStatusParam(inviteFilter)));
  };

  const reloadSlackIntegrations = async () => {
    if (!isAdmin) {
      return;
    }

    setSlackIntegrationState({ status: "loading", integrations: [], error: null });
    setSlackActionState({ status: "idle", message: null });
    setSlackIntegrationState(await fetchSlackIntegrationList());
  };

  const reloadSlackChannelMappings = async () => {
    if (!isAdmin) {
      return;
    }

    setSlackChannelMappingState({ status: "loading", mappings: [], error: null });
    setSlackMappingCreateState({ status: "idle", message: null });
    setSlackChannelMappingState(await fetchSlackChannelMappingList());
  };

  const reloadGatewayStatus = async () => {
    if (!isAdmin) {
      return;
    }

    setGatewayStatusState({ status: "loading", gateway: null, error: null });
    const nextState = await fetchGatewayStatus();
    setGatewayStatusState(nextState);
    if (nextState.status === "ready" && nextState.gateway.gatewayUrl) {
      setGatewayUrlInput((previous) => previous || nextState.gateway.gatewayUrl || "");
    }
  };

  const handleTestGatewayConnection = async () => {
    if (!isAdmin) {
      return;
    }

    if (!normalizedGatewayForm.isValid) {
      setGatewayConnectionTestState({
        status: "error",
        result: null,
        message:
          normalizedGatewayForm.urlError ??
          normalizedGatewayForm.tokenError ??
          "gateway 연결 정보를 확인해 주세요.",
      });
      return;
    }

    setGatewayConnectionTestState({ status: "submitting", result: null, message: null });
    try {
      const result = await testWorkspaceGatewayConnection(
        resolveAccessToken(),
        numericWorkspaceId,
        {
          gatewayUrl: normalizedGatewayForm.gatewayUrl,
          token: normalizedGatewayForm.token,
        },
      );
      setGatewayConnectionTestState({
        status: "success",
        result,
        message: result.message || gatewayConnectionStatusLabel(result.status),
      });
    } catch (error) {
      setGatewayConnectionTestState({
        status: "error",
        result: null,
        message: messageFromError(error, "gateway 연결 테스트를 처리하지 못했습니다."),
      });
    }
  };

  const handleCreateGatewayBinding = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    if (!isAdmin) {
      return;
    }

    if (!normalizedGatewayForm.isValid) {
      setGatewayBindingState({
        status: "error",
        message:
          normalizedGatewayForm.urlError ??
          normalizedGatewayForm.tokenError ??
          "gateway 연결 정보를 확인해 주세요.",
      });
      return;
    }

    setGatewayBindingState({ status: "submitting", message: null });
    try {
      await createWorkspaceGatewayBinding(resolveAccessToken(), numericWorkspaceId, {
        gatewayUrl: normalizedGatewayForm.gatewayUrl,
        token: normalizedGatewayForm.token,
        validateConnection: normalizedGatewayForm.validateConnection,
      });
      const nextGatewayStatus = await fetchGatewayStatus();
      setGatewayStatusState(nextGatewayStatus);
      setGatewayUrlInput(normalizedGatewayForm.gatewayUrl);
      setGatewayTokenInput("");
      setGatewayBindingState({
        status: "success",
        message: "gateway binding이 등록되었습니다.",
      });
    } catch (error) {
      setGatewayBindingState({
        status: "error",
        message: messageFromError(error, "gateway binding을 등록하지 못했습니다."),
      });
    }
  };

  const handleCreateSettingsInvite = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    if (!isAdmin) {
      return;
    }

    if (!trimmedInviteEmail) {
      setInviteCreateState({ status: "error", message: "초대 이메일을 입력해 주세요." });
      return;
    }

    if (parsedInviteExpiresInDays === null) {
      setInviteCreateState({ status: "error", message: "만료 기간은 1일부터 30일까지 입력해 주세요." });
      return;
    }

    setInviteCreateState({ status: "submitting", message: null });
    try {
      const invite = await createWorkspaceInvite(resolveAccessToken(), numericWorkspaceId, {
        email: trimmedInviteEmail,
        mode: inviteCreateMode,
        role: inviteRole,
        expiresInDays: parsedInviteExpiresInDays,
      });
      setCreatedInvite(invite);
      setCopiedInviteToken(null);
      setInviteCreateState({ status: "success", message: "초대가 생성되었습니다." });
      setInviteEmailInput("");
      setInviteLogState((previous) =>
        previous.status === "ready"
          ? {
              ...previous,
              invites: mergeInviteLog(previous.invites, {
                ...invite,
                revokedAt: null,
              }),
            }
          : previous,
      );
    } catch (error) {
      setInviteCreateState({
        status: "error",
        message: messageFromError(error, "초대를 생성하지 못했습니다."),
      });
    }
  };

  const copyCreatedInviteUrl = async () => {
    if (!createdInvite) {
      return;
    }

    try {
      await writeClipboardText(createInviteUrl(createdInvite));
      setCopiedInviteToken(createdInvite.token);
      setInviteCreateState({ status: "success", message: "초대 링크가 복사되었습니다." });
    } catch {
      setInviteCreateState({ status: "error", message: "초대 링크를 복사하지 못했습니다." });
    }
  };

  const copyInviteManagementUrl = async (invite: WorkspaceInviteManagement) => {
    try {
      await writeClipboardText(createInviteUrl(invite));
      setCopiedInviteToken(invite.token);
      setInviteActionState({ status: "success", message: "초대 링크가 복사되었습니다." });
    } catch {
      setInviteActionState({ status: "error", message: "초대 링크를 복사하지 못했습니다." });
    }
  };

  const handleExtendInvite = async (invite: WorkspaceInviteManagement) => {
    if (!isAdmin || invite.status !== "PENDING" || hasInviteAction) {
      return;
    }

    if (parsedInviteExpiresInDays === null) {
      setInviteActionState({ status: "error", message: "연장 기간은 1일부터 30일까지 입력해 주세요." });
      return;
    }

    setInviteActionId(invite.inviteId);
    setInviteActionState({ status: "submitting", message: null });
    try {
      const extendedInvite = await extendWorkspaceInvite(
        resolveAccessToken(),
        numericWorkspaceId,
        invite.inviteId,
        {
          additionalDays: parsedInviteExpiresInDays,
        },
      );
      setInviteLogState((previous) =>
        previous.status === "ready"
          ? {
              ...previous,
              invites: mergeInviteLog(previous.invites, extendedInvite),
            }
          : previous,
      );
      setInviteActionState({ status: "success", message: "초대 만료일이 연장되었습니다." });
    } catch (error) {
      setInviteActionState({
        status: "error",
        message: messageFromError(error, "초대 만료일을 연장하지 못했습니다."),
      });
    } finally {
      setInviteActionId(null);
    }
  };

  const handleDeleteInvite = async (invite: WorkspaceInviteManagement) => {
    if (!isAdmin || invite.status !== "PENDING" || hasInviteAction) {
      return;
    }

    setInviteActionId(invite.inviteId);
    setInviteActionState({ status: "submitting", message: null });
    try {
      await deleteWorkspaceInvite(resolveAccessToken(), numericWorkspaceId, invite.inviteId);
      setInviteLogState((previous) =>
        previous.status === "ready"
          ? {
              ...previous,
              invites: updateInviteLogAfterRevoke(previous.invites, invite.inviteId, inviteFilter),
            }
          : previous,
      );
      setInviteActionState({ status: "success", message: "초대가 취소되었습니다." });
    } catch (error) {
      setInviteActionState({
        status: "error",
        message: messageFromError(error, "초대를 취소하지 못했습니다."),
      });
    } finally {
      setInviteActionId(null);
    }
  };

  const handleUpdateWorkspace = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    if (!canManageWorkspace || state.status !== "ready") {
      return;
    }

    if (!trimmedName) {
      setUpdateState({ status: "error", message: "워크스페이스 이름을 입력해 주세요." });
      return;
    }

    setUpdateState({ status: "submitting", message: null });
    try {
      const updated = await updateWorkspace(resolveAccessToken(), numericWorkspaceId, {
        name: trimmedName,
        description: trimmedDescription || undefined,
      });
      setState({ ...state, workspace: updated });
      setNameInput(updated.name);
      setDescriptionInput(updated.description ?? "");
      setDeleteConfirmation("");
      setUpdateState({ status: "success", message: "워크스페이스 정보가 저장되었습니다." });
    } catch (error) {
      setUpdateState({
        status: "error",
        message: messageFromError(error, "워크스페이스 정보를 저장하지 못했습니다."),
      });
    }
  };

  const closeDeleteWorkspaceModal = () => {
    if (isDeletingWorkspace) {
      return;
    }

    setDeleteWorkspaceModalOpen(false);
    setDeleteConfirmation("");
    setDeleteState({ status: "idle", message: null });
  };

  const handleDeleteWorkspace = async () => {
    if (!canManageWorkspace || state.status !== "ready" || !workspace) {
      return;
    }

    if (deleteConfirmation !== workspace.name) {
      setDeleteState({ status: "error", message: "워크스페이스 이름을 정확히 입력해 주세요." });
      return;
    }

    setDeleteState({ status: "submitting", message: null });
    try {
      await deleteWorkspace(resolveAccessToken(), numericWorkspaceId);
      router.replace("/");
    } catch (error) {
      setDeleteState({
        status: "error",
        message: messageFromError(error, "워크스페이스를 삭제하지 못했습니다."),
      });
    }
  };

  const handleChangeMemberRole = async (
    member: WorkspaceMember,
    nextRole: WorkspaceMemberRole,
  ) => {
    if (!canManageMembers || member.role === nextRole || isLastAdmin(member, adminCount, nextRole)) {
      return;
    }

    setMemberAction({ type: "role", memberId: member.memberId });
    setMemberState({ status: "submitting", message: null });
    try {
      await changeWorkspaceMemberRole(
        resolveAccessToken(),
        numericWorkspaceId,
        member.memberId,
        nextRole,
      );
      setState((previous) =>
        previous.status === "ready"
          ? {
              ...previous,
              members: previous.members.map((currentMember) =>
                currentMember.memberId === member.memberId
                  ? { ...currentMember, role: nextRole }
                  : currentMember,
              ),
            }
          : previous,
      );
      setMemberState({ status: "success", message: "멤버 역할이 변경되었습니다." });
    } catch (error) {
      setMemberState({
        status: "error",
        message: messageFromError(error, "멤버 역할을 변경하지 못했습니다."),
      });
    } finally {
      setMemberAction(null);
    }
  };

  const handleRemoveMember = async (member: WorkspaceMember) => {
    if (!canManageMembers || isLastAdmin(member, adminCount)) {
      return;
    }

    setMemberAction({ type: "remove", memberId: member.memberId });
    setMemberState({ status: "submitting", message: null });
    try {
      await removeWorkspaceMember(resolveAccessToken(), numericWorkspaceId, member.memberId);
      setState((previous) =>
        previous.status === "ready"
          ? {
              ...previous,
              members: previous.members.filter(
                (currentMember) => currentMember.memberId !== member.memberId,
              ),
            }
          : previous,
      );
      setMemberState({ status: "success", message: "멤버가 제거되었습니다." });
    } catch (error) {
      setMemberState({
        status: "error",
        message: messageFromError(error, "멤버를 제거하지 못했습니다."),
      });
    } finally {
      setMemberAction(null);
    }
  };

  const handleCreateGithubCredential = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    if (!isAdmin) {
      return;
    }

    if (!trimmedGithubDisplayName || !trimmedGithubToken) {
      setGithubCreateState({
        status: "error",
        message: "이름과 token을 모두 입력해 주세요.",
      });
      return;
    }

    setGithubCreateState({ status: "submitting", message: null });
    try {
      const createdCredential = await createGithubCredential(
        resolveAccessToken(),
        numericWorkspaceId,
        {
          displayName: trimmedGithubDisplayName,
          token: trimmedGithubToken,
        },
      );
      setGithubCredentialState((previous) =>
        previous.status === "ready"
          ? {
              ...previous,
              credentials: [createdCredential, ...previous.credentials],
            }
          : { status: "ready", credentials: [createdCredential], error: null },
      );
      setGithubDisplayNameInput("");
      setGithubTokenInput("");
      setGithubCreateState({
        status: "success",
        message: "GitHub 자격 증명이 등록되었습니다.",
      });
    } catch (error) {
      setGithubCreateState({
        status: "error",
        message: messageFromError(error, "GitHub 자격 증명을 등록하지 못했습니다."),
      });
    }
  };

  const startGithubCredentialEdit = (credential: GithubCredential) => {
    if (!isAdmin || hasGithubCredentialAction) {
      return;
    }

    setEditingGithubCredentialId(credential.credentialId);
    setGithubEditDisplayName(credential.displayName);
    setGithubEditToken("");
    setGithubActionState({ status: "idle", message: null });
  };

  const cancelGithubCredentialEdit = () => {
    if (hasGithubCredentialAction) {
      return;
    }

    setEditingGithubCredentialId(null);
    setGithubEditDisplayName("");
    setGithubEditToken("");
  };

  const handleUpdateGithubCredential = async (
    event: FormEvent<HTMLFormElement>,
    credential: GithubCredential,
  ) => {
    event.preventDefault();

    if (!isAdmin || hasGithubCredentialAction) {
      return;
    }

    const nextDisplayName = githubEditDisplayName.trim();
    const nextToken = githubEditToken.trim();
    if (!nextDisplayName) {
      setGithubActionState({
        status: "error",
        message: "표시 이름을 입력해 주세요.",
      });
      return;
    }

    setGithubAction({ type: "update", credentialId: credential.credentialId });
    setGithubActionState({ status: "submitting", message: null });
    try {
      const updatedCredential = await updateGithubCredential(
        resolveAccessToken(),
        numericWorkspaceId,
        credential.credentialId,
        {
          displayName: nextDisplayName,
          token: nextToken || undefined,
        },
      );
      setGithubCredentialState((previous) =>
        previous.status === "ready"
          ? {
              ...previous,
              credentials: previous.credentials.map((currentCredential) =>
                currentCredential.credentialId === updatedCredential.credentialId
                  ? updatedCredential
                  : currentCredential,
              ),
            }
          : previous,
      );
      setEditingGithubCredentialId(null);
      setGithubEditDisplayName("");
      setGithubEditToken("");
      setGithubActionState({
        status: "success",
        message: "GitHub 자격 증명이 수정되었습니다.",
      });
    } catch (error) {
      setGithubActionState({
        status: "error",
        message: messageFromError(error, "GitHub 자격 증명을 수정하지 못했습니다."),
      });
    } finally {
      setGithubAction(null);
    }
  };

  const handleDeleteGithubCredential = async (credential: GithubCredential) => {
    if (!isAdmin || hasGithubCredentialAction) {
      return;
    }

    setGithubAction({ type: "delete", credentialId: credential.credentialId });
    setGithubActionState({ status: "submitting", message: null });
    try {
      await deleteGithubCredential(
        resolveAccessToken(),
        numericWorkspaceId,
        credential.credentialId,
      );
      setGithubCredentialState((previous) =>
        previous.status === "ready"
          ? {
              ...previous,
              credentials: previous.credentials.filter(
                (currentCredential) =>
                  currentCredential.credentialId !== credential.credentialId,
              ),
            }
          : previous,
      );
      if (editingGithubCredentialId === credential.credentialId) {
        setEditingGithubCredentialId(null);
        setGithubEditDisplayName("");
        setGithubEditToken("");
      }
      setGithubActionState({
        status: "success",
        message: "GitHub 자격 증명이 삭제되었습니다.",
      });
    } catch (error) {
      setGithubActionState({
        status: "error",
        message: messageFromError(error, "GitHub 자격 증명을 삭제하지 못했습니다."),
      });
    } finally {
      setGithubAction(null);
    }
  };

  const handleOpenSlackInstallUrl = async () => {
    if (!isAdmin || isOpeningSlackInstall) {
      return;
    }

    setSlackInstallState({ status: "submitting", message: null });
    try {
      const installUrl = await fetchSlackInstallUrl(resolveAccessToken(), numericWorkspaceId);
      if (typeof window !== "undefined") {
        window.open(installUrl.url, "_blank", "noopener,noreferrer");
      }
      setSlackInstallState({
        status: "success",
        message: "Slack 설치 페이지를 열었습니다.",
      });
    } catch (error) {
      setSlackInstallState({
        status: "error",
        message: messageFromError(error, "Slack 설치 URL을 확인하지 못했습니다."),
      });
    }
  };

  const handleCreateSlackIntegration = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    if (!isAdmin) {
      return;
    }

    if (!trimmedSlackTeamId || !trimmedSlackChannelId || !trimmedSlackBotToken) {
      setSlackCreateState({
        status: "error",
        message: "Team ID, Channel ID, bot token을 모두 입력해 주세요.",
      });
      return;
    }

    setSlackCreateState({ status: "submitting", message: null });
    try {
      const createdIntegration = await createSlackIntegration(
        resolveAccessToken(),
        numericWorkspaceId,
        {
          slackTeamId: trimmedSlackTeamId,
          slackChannelId: trimmedSlackChannelId,
          channelName: trimmedSlackChannelName || undefined,
          botToken: trimmedSlackBotToken,
        },
      );
      setSlackIntegrationState((previous) =>
        previous.status === "ready"
          ? {
              ...previous,
              integrations: [createdIntegration, ...previous.integrations],
            }
          : { status: "ready", integrations: [createdIntegration], error: null },
      );
      setSlackTeamIdInput("");
      setSlackChannelIdInput("");
      setSlackChannelNameInput("");
      setSlackBotTokenInput("");
      setSlackCreateState({
        status: "success",
        message: "Slack 연동이 등록되었습니다.",
      });
    } catch (error) {
      setSlackCreateState({
        status: "error",
        message: messageFromError(error, "Slack 연동을 등록하지 못했습니다."),
      });
    }
  };

  const handleCreateSlackChannelMapping = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    if (!isAdmin) {
      return;
    }

    if (
      !trimmedSlackMappingTeamId ||
      !trimmedSlackMappingChannelId ||
      !trimmedSlackMappingBotToken
    ) {
      setSlackMappingCreateState({
        status: "error",
        message: "Team ID, Channel ID, bot token을 모두 입력해 주세요.",
      });
      return;
    }

    setSlackMappingCreateState({ status: "submitting", message: null });
    try {
      const createdMapping = await createSlackChannelMapping(
        resolveAccessToken(),
        numericWorkspaceId,
        {
          teamId: trimmedSlackMappingTeamId,
          channelId: trimmedSlackMappingChannelId,
          channelName: trimmedSlackMappingChannelName || undefined,
          botToken: trimmedSlackMappingBotToken,
        },
      );
      setSlackChannelMappingState((previous) =>
        previous.status === "ready"
          ? {
              ...previous,
              mappings: [createdMapping, ...previous.mappings],
            }
          : { status: "ready", mappings: [createdMapping], error: null },
      );
      setSlackMappingTeamIdInput("");
      setSlackMappingChannelIdInput("");
      setSlackMappingChannelNameInput("");
      setSlackMappingBotTokenInput("");
      setSlackMappingCreateState({
        status: "success",
        message: "Slack 채널 매핑이 등록되었습니다.",
      });
    } catch (error) {
      setSlackMappingCreateState({
        status: "error",
        message: messageFromError(error, "Slack 채널 매핑을 등록하지 못했습니다."),
      });
    }
  };

  const startSlackIntegrationEdit = (integration: SlackIntegration) => {
    if (!isAdmin || hasSlackIntegrationAction) {
      return;
    }

    setEditingSlackIntegrationId(integration.id);
    setSlackEditTeamId(integration.slackTeamId);
    setSlackEditChannelId(integration.slackChannelId);
    setSlackEditChannelName(integration.channelName ?? "");
    setSlackEditBotToken("");
    setSlackActionState({ status: "idle", message: null });
  };

  const cancelSlackIntegrationEdit = () => {
    if (hasSlackIntegrationAction) {
      return;
    }

    setEditingSlackIntegrationId(null);
    setSlackEditTeamId("");
    setSlackEditChannelId("");
    setSlackEditChannelName("");
    setSlackEditBotToken("");
  };

  const handleUpdateSlackIntegration = async (
    event: FormEvent<HTMLFormElement>,
    integration: SlackIntegration,
  ) => {
    event.preventDefault();

    if (!isAdmin || hasSlackIntegrationAction) {
      return;
    }

    const nextTeamId = slackEditTeamId.trim();
    const nextChannelId = slackEditChannelId.trim();
    const nextChannelName = slackEditChannelName.trim();
    const nextBotToken = slackEditBotToken.trim();
    if (!nextTeamId || !nextChannelId) {
      setSlackActionState({
        status: "error",
        message: "Team ID와 Channel ID를 입력해 주세요.",
      });
      return;
    }

    setSlackAction({ type: "update", integrationId: integration.id });
    setSlackActionState({ status: "submitting", message: null });
    try {
      const updatedIntegration = await updateSlackIntegration(
        resolveAccessToken(),
        numericWorkspaceId,
        integration.id,
        {
          slackTeamId: nextTeamId,
          slackChannelId: nextChannelId,
          channelName: nextChannelName || undefined,
          botToken: nextBotToken || undefined,
        },
      );
      setSlackIntegrationState((previous) =>
        previous.status === "ready"
          ? {
              ...previous,
              integrations: previous.integrations.map((currentIntegration) =>
                currentIntegration.id === updatedIntegration.id
                  ? updatedIntegration
                  : currentIntegration,
              ),
            }
          : previous,
      );
      setEditingSlackIntegrationId(null);
      setSlackEditTeamId("");
      setSlackEditChannelId("");
      setSlackEditChannelName("");
      setSlackEditBotToken("");
      setSlackActionState({
        status: "success",
        message: "Slack 연동이 수정되었습니다.",
      });
    } catch (error) {
      setSlackActionState({
        status: "error",
        message: messageFromError(error, "Slack 연동을 수정하지 못했습니다."),
      });
    } finally {
      setSlackAction(null);
    }
  };

  const handleDeleteSlackIntegration = async (integration: SlackIntegration) => {
    if (!isAdmin || hasSlackIntegrationAction) {
      return;
    }

    setSlackAction({ type: "delete", integrationId: integration.id });
    setSlackActionState({ status: "submitting", message: null });
    try {
      await deleteSlackIntegration(resolveAccessToken(), numericWorkspaceId, integration.id);
      setSlackIntegrationState((previous) =>
        previous.status === "ready"
          ? {
              ...previous,
              integrations: previous.integrations.filter(
                (currentIntegration) => currentIntegration.id !== integration.id,
              ),
            }
          : previous,
      );
      if (editingSlackIntegrationId === integration.id) {
        setEditingSlackIntegrationId(null);
        setSlackEditTeamId("");
        setSlackEditChannelId("");
        setSlackEditChannelName("");
        setSlackEditBotToken("");
      }
      setSlackActionState({
        status: "success",
        message: "Slack 연동이 삭제되었습니다.",
      });
    } catch (error) {
      setSlackActionState({
        status: "error",
        message: messageFromError(error, "Slack 연동을 삭제하지 못했습니다."),
      });
    } finally {
      setSlackAction(null);
    }
  };

  return (
    <main className="workspace-settings-shell">
      <section className="workspace-settings-panel" aria-label="워크스페이스 설정">
        <header className="workspace-settings-header">
          <div className="workspace-settings-nav">
            <Link className="secondary-action workspace-settings-back" href="/">
              <ArrowLeft aria-hidden="true" />
              <span>목록으로</span>
            </Link>
            {isValidWorkspaceId(numericWorkspaceId) ? (
              <Link
                className="secondary-action workspace-settings-back"
                href={`/workspaces/${numericWorkspaceId}`}
              >
                <Building2 aria-hidden="true" />
                <span>상세로</span>
              </Link>
            ) : null}
          </div>

          <div className="workspace-settings-title">
            <div className="workspace-settings-icon">
              <ShieldCheck aria-hidden="true" />
            </div>
            <div>
              <p className="eyebrow">Workspace Settings</p>
              <h1>{workspace?.name ?? "워크스페이스 설정"}</h1>
            </div>
          </div>
        </header>

        {state.status === "loading" ? (
          <WorkspaceSettingsStateMessage
            tone="neutral"
            icon={<Loader2 className="spin" aria-hidden="true" />}
            title="설정 확인 중"
            message="워크스페이스 설정과 멤버 정보를 불러오고 있습니다."
          />
        ) : null}

        {state.status === "error" || state.status === "offline" ? (
          <WorkspaceSettingsStateMessage
            tone={state.status === "offline" ? "offline" : "danger"}
            icon={
              state.status === "offline" ? (
                <WifiOff aria-hidden="true" />
              ) : (
                <AlertCircle aria-hidden="true" />
              )
            }
            title={state.status === "offline" ? "연결 실패" : "설정 확인 실패"}
            message={state.error}
            action={
              <button
                className="secondary-action workspace-settings-retry"
                type="button"
                onClick={() => void reloadSettings()}
              >
                <RefreshCw aria-hidden="true" />
                <span>다시 시도</span>
              </button>
            }
          />
        ) : null}

        {workspace ? (
          <section className="workspace-settings-content" aria-label="설정 내용">
            <div className="workspace-settings-summary">
              <div>
                <span className="workspace-detail-label">현재 워크스페이스</span>
                <strong>{workspace.name}</strong>
                <p>{workspace.description ?? "설명 없음"}</p>
              </div>
              <span className={isAdmin ? "workspace-count-badge" : "workspace-lock-badge"}>
                {isAdmin ? "ADMIN" : "MEMBER"}
              </span>
            </div>

            <dl className="workspace-settings-meta">
              <SettingsMetaItem
                icon={<ShieldCheck aria-hidden="true" />}
                label="내 역할"
                value={<span className="role-badge">{formatRole(workspace.myRole)}</span>}
              />
              <SettingsMetaItem
                icon={<Users aria-hidden="true" />}
                label="멤버"
                value={`${members.length}명 · 관리자 ${adminCount}명`}
              />
              <SettingsMetaItem
                icon={<CalendarDays aria-hidden="true" />}
                label="참여 일시"
                value={formatDateTime(workspace.joinedAt)}
              />
              <SettingsMetaItem
                icon={<Hash aria-hidden="true" />}
                label="Workspace ID"
                value={workspace.workspaceId}
              />
            </dl>

            <nav className="workspace-settings-tabs" aria-label="워크스페이스 설정 섹션">
              {workspaceSettingsSections.map((section) => (
                <button
                  className={
                    activeSettingsSection === section.id
                      ? "workspace-settings-tab workspace-settings-tab-active"
                      : "workspace-settings-tab"
                  }
                  type="button"
                  key={section.id}
                  aria-label={section.ariaLabel}
                  aria-pressed={activeSettingsSection === section.id}
                  onClick={() => setActiveSettingsSection(section.id)}
                >
                  {section.label}
                </button>
              ))}
            </nav>

            <section
              className="workspace-integration-overview"
              aria-label="연동 설정 요약"
              hidden={!SHOW_CONNECTION_SETTINGS}
            >
              <div className="workspace-section-title">
                <div>
                  <span className="workspace-detail-label">Integrations</span>
                  <h2>연동 준비도</h2>
                </div>
                <span className="workspace-count-badge">
                  {formatIntegrationSettingsReadiness(integrationSettingsOverview.readiness)}
                </span>
              </div>

              <div className="workspace-integration-score">
                <strong>{integrationSettingsOverview.summary}</strong>
                <span>
                  {integrationSettingsOverview.connectedCount}개 활성 ·{" "}
                  {integrationSettingsOverview.totalCount}개 항목
                </span>
              </div>

              <div className="workspace-integration-section-grid">
                {integrationSettingsOverview.sections.map((section) => (
                  <article
                    className={`workspace-integration-section workspace-integration-section-${section.state}`}
                    key={section.id}
                  >
                    <span>{section.label}</span>
                    <strong>{section.count}</strong>
                    <p>{section.detail}</p>
                  </article>
                ))}
              </div>

              <div className="workspace-integration-action-grid">
                {integrationSettingsActions.map((action) => (
                  <IntegrationSettingsActionCard action={action} key={action.id} />
                ))}
              </div>
            </section>

            <section
              className={
                activeSettingsSection === "workspace"
                  ? "workspace-settings-section"
                  : "workspace-settings-section workspace-settings-section-hidden"
              }
              aria-label="워크스페이스 정보 수정"
              hidden={activeSettingsSection !== "workspace"}
            >
              <div className="workspace-section-title">
                <div>
                  <span className="workspace-detail-label">Settings</span>
                  <h2>기본 정보</h2>
                </div>
                <span className={canManageWorkspace ? "workspace-count-badge" : "workspace-lock-badge"}>
                  {canManageWorkspace ? "관리 가능" : "읽기 전용"}
                </span>
              </div>

              <form className="workspace-settings-form" onSubmit={handleUpdateWorkspace}>
                <label className="form-field">
                  <span>
                    <strong>이름</strong>
                    <small>{nameInput.length}/{NAME_MAX_LENGTH}</small>
                  </span>
                  <input
                    value={nameInput}
                    onChange={(event) => {
                      setNameInput(event.target.value);
                      setUpdateState({ status: "idle", message: null });
                    }}
                    maxLength={NAME_MAX_LENGTH}
                    disabled={!canManageWorkspace || isUpdatingWorkspace}
                  />
                </label>

                <label className="form-field">
                  <span>
                    <strong>설명</strong>
                    <small>{descriptionInput.length}/{DESCRIPTION_MAX_LENGTH}</small>
                  </span>
                  <textarea
                    value={descriptionInput}
                    onChange={(event) => {
                      setDescriptionInput(event.target.value);
                      setUpdateState({ status: "idle", message: null });
                    }}
                    maxLength={DESCRIPTION_MAX_LENGTH}
                    rows={4}
                    disabled={!canManageWorkspace || isUpdatingWorkspace}
                  />
                </label>

                {updateState.status === "error" ? (
                  <SettingsNotice tone="danger" icon={<AlertCircle aria-hidden="true" />}>
                    {updateState.message}
                  </SettingsNotice>
                ) : null}

                {updateState.status === "success" ? (
                  <SettingsNotice tone="success" icon={<CheckCircle2 aria-hidden="true" />}>
                    {updateState.message}
                  </SettingsNotice>
                ) : null}

                <div className="workspace-settings-actions">
                  <button
                    className="primary-action primary-action-active"
                    type="submit"
                    disabled={!canUpdateWorkspace}
                  >
                    {isUpdatingWorkspace ? (
                      <Loader2 className="spin" aria-hidden="true" />
                    ) : (
                      <Save aria-hidden="true" />
                    )}
                    <span>{isUpdatingWorkspace ? "저장 중" : "저장"}</span>
                  </button>
                </div>
              </form>
            </section>

            <section
              className={
                activeSettingsSection === "workspace"
                  ? "workspace-settings-section workspace-settings-danger"
                  : "workspace-settings-section workspace-settings-danger workspace-settings-section-hidden"
              }
              aria-label="워크스페이스 삭제"
              hidden={activeSettingsSection !== "workspace"}
            >
              <div className="workspace-section-title">
                <div>
                  <span className="workspace-detail-label">Delete</span>
                  <h2>워크스페이스 삭제</h2>
                </div>
                <span className="workspace-lock-badge">ADMIN</span>
              </div>

              <div className="workspace-settings-actions">
                <button
                  className="danger-action"
                  type="button"
                  onClick={() => {
                    setDeleteWorkspaceModalOpen(true);
                    setDeleteState({ status: "idle", message: null });
                  }}
                  disabled={!canManageWorkspace}
                >
                  <Trash2 aria-hidden="true" />
                  <span>삭제 확인</span>
                </button>
              </div>
            </section>

            {deleteWorkspaceModalOpen ? (
              <div className="workspace-settings-modal-backdrop">
                <div
                  className="workspace-settings-delete-modal"
                  role="dialog"
                  aria-modal="true"
                  aria-label="워크스페이스 삭제 확인"
                >
                  <div className="workspace-section-title">
                    <div>
                      <span className="workspace-detail-label">Delete</span>
                      <h2>워크스페이스 삭제</h2>
                    </div>
                    <button
                      className="icon-button"
                      type="button"
                      aria-label="닫기"
                      onClick={closeDeleteWorkspaceModal}
                      disabled={isDeletingWorkspace}
                    >
                      <X aria-hidden="true" />
                    </button>
                  </div>

                  <div className="workspace-settings-form">
                    <label className="form-field">
                      <span>
                        <strong>삭제 확인</strong>
                        <small>{workspace.name}</small>
                      </span>
                      <input
                        value={deleteConfirmation}
                        onChange={(event) => {
                          setDeleteConfirmation(event.target.value);
                          setDeleteState({ status: "idle", message: null });
                        }}
                        disabled={!canManageWorkspace || isDeletingWorkspace}
                      />
                    </label>

                    {deleteState.status === "error" ? (
                      <SettingsNotice tone="danger" icon={<AlertCircle aria-hidden="true" />}>
                        {deleteState.message}
                      </SettingsNotice>
                    ) : null}

                    <div className="workspace-settings-actions workspace-delete-modal-actions">
                      <button
                        className="secondary-action"
                        type="button"
                        onClick={closeDeleteWorkspaceModal}
                        disabled={isDeletingWorkspace}
                      >
                        취소
                      </button>
                      <button
                        className="danger-action"
                        type="button"
                        onClick={() => void handleDeleteWorkspace()}
                        disabled={!canDeleteWorkspace}
                      >
                        {isDeletingWorkspace ? (
                          <Loader2 className="spin" aria-hidden="true" />
                        ) : (
                          <Trash2 aria-hidden="true" />
                        )}
                        <span>{isDeletingWorkspace ? "삭제 중" : "삭제"}</span>
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            ) : null}

            <section
              className={
                activeSettingsSection === "members"
                  ? "workspace-settings-section"
                  : "workspace-settings-section workspace-settings-section-hidden"
              }
              aria-label="멤버 현황"
              hidden={activeSettingsSection !== "members"}
            >
              <div className="workspace-section-title">
                <div>
                  <span className="workspace-detail-label">Members</span>
                  <h2>멤버 관리</h2>
                </div>
                <span className={canManageMembers ? "workspace-count-badge" : "workspace-lock-badge"}>
                  {members.length}명
                </span>
              </div>

              {memberState.status === "error" ? (
                <SettingsNotice tone="danger" icon={<AlertCircle aria-hidden="true" />}>
                  {memberState.message}
                </SettingsNotice>
              ) : null}

              {memberState.status === "success" ? (
                <SettingsNotice tone="success" icon={<CheckCircle2 aria-hidden="true" />}>
                  {memberState.message}
                </SettingsNotice>
              ) : null}

              {members.length > 0 ? (
                <div className="workspace-settings-member-list">
                  {members.map((member) => {
                    const roleBusy =
                      memberAction?.type === "role" && memberAction.memberId === member.memberId;
                    const removeBusy =
                      memberAction?.type === "remove" && memberAction.memberId === member.memberId;
                    const lastAdmin = isLastAdmin(member, adminCount);

                    return (
                      <article className="workspace-settings-member-row" key={member.memberId}>
                        <div className="workspace-member-avatar">
                          <UserRound aria-hidden="true" />
                        </div>
                        <div className="workspace-member-main">
                          <strong>{member.name}</strong>
                          <p>{member.email}</p>
                          <span>{formatDateTime(member.joinedAt)}</span>
                        </div>
                        <div className="workspace-settings-member-actions">
                          <RoleSegmentedControl
                            role={member.role}
                            busy={roleBusy}
                            disabled={!canManageMembers || Boolean(memberAction)}
                            disableAdminDemotion={lastAdmin}
                            onChange={(nextRole) => void handleChangeMemberRole(member, nextRole)}
                          />
                          <button
                            className="danger-action workspace-member-remove"
                            type="button"
                            onClick={() => void handleRemoveMember(member)}
                            disabled={!canManageMembers || Boolean(memberAction) || lastAdmin}
                          >
                            {removeBusy ? (
                              <Loader2 className="spin" aria-hidden="true" />
                            ) : (
                              <Trash2 aria-hidden="true" />
                            )}
                            <span>{removeBusy ? "제거 중" : "제거"}</span>
                          </button>
                        </div>
                      </article>
                    );
                  })}
                </div>
              ) : (
                <div className="workspace-member-empty">
                  <Users aria-hidden="true" />
                  <strong>표시할 멤버가 없습니다</strong>
                  <p>워크스페이스에 참여한 멤버가 생기면 이곳에 표시됩니다.</p>
                </div>
              )}
            </section>

            <section
              className={
                activeSettingsSection === "invite"
                  ? "workspace-settings-section"
                  : "workspace-settings-section workspace-settings-section-hidden"
              }
              aria-label="워크스페이스 초대 생성"
              hidden={activeSettingsSection !== "invite"}
            >
              <div className="workspace-section-title">
                <div>
                  <span className="workspace-detail-label">Invite</span>
                  <h2>초대 생성</h2>
                </div>
                <span className={isAdmin ? "workspace-count-badge" : "workspace-lock-badge"}>
                  {isAdmin ? inviteCreateMode.toUpperCase() : "읽기 전용"}
                </span>
              </div>

              {!isAdmin ? (
                <p className="workspace-invite-help">
                  관리자 역할만 초대를 생성할 수 있습니다.
                </p>
              ) : null}

              {isAdmin ? (
                <form className="workspace-settings-form" onSubmit={handleCreateSettingsInvite}>
                  <div className="workspace-invite-create-mode" role="group" aria-label="초대 생성 모드">
                    <button
                      className={
                        inviteCreateMode === "email"
                          ? "workspace-invite-mode-option workspace-invite-mode-option-active"
                          : "workspace-invite-mode-option"
                      }
                      type="button"
                      aria-pressed={inviteCreateMode === "email"}
                      onClick={() => {
                        setInviteCreateMode("email");
                        setInviteCreateState({ status: "idle", message: null });
                      }}
                    >
                      <Mail aria-hidden="true" />
                      <span>Email</span>
                    </button>
                    <button
                      className={
                        inviteCreateMode === "link"
                          ? "workspace-invite-mode-option workspace-invite-mode-option-active"
                          : "workspace-invite-mode-option"
                      }
                      type="button"
                      aria-pressed={inviteCreateMode === "link"}
                      onClick={() => {
                        setInviteCreateMode("link");
                        setInviteCreateState({ status: "idle", message: null });
                      }}
                    >
                      <Link2 aria-hidden="true" />
                      <span>Link</span>
                    </button>
                  </div>

                  <div className="workspace-gateway-form-grid">
                    <label className="form-field">
                      <span>
                        <strong>이메일</strong>
                        <small>{inviteCreateMode === "email" ? "email" : "link"}</small>
                      </span>
                      <input
                        value={inviteEmailInput}
                        onChange={(event) => {
                          setInviteEmailInput(event.target.value);
                          setInviteCreateState({ status: "idle", message: null });
                        }}
                        type="email"
                        disabled={!isAdmin || isCreatingInvite}
                      />
                    </label>

                    <label className="form-field">
                      <span>
                        <strong>만료</strong>
                        <small>1-30 days</small>
                      </span>
                      <input
                        value={inviteExpiresInDays}
                        onChange={(event) => {
                          setInviteExpiresInDays(event.target.value);
                          setInviteCreateState({ status: "idle", message: null });
                          setInviteActionState({ status: "idle", message: null });
                        }}
                        inputMode="numeric"
                        disabled={!isAdmin || isCreatingInvite}
                      />
                    </label>
                  </div>

                  <div className="workspace-role-control" role="group" aria-label="초대 역할">
                    {(["ADMIN", "MEMBER"] as WorkspaceMemberRole[]).map((role) => (
                      <button
                        className={
                          inviteRole === role
                            ? "workspace-role-option workspace-role-option-active"
                            : "workspace-role-option"
                        }
                        type="button"
                        key={role}
                        aria-pressed={inviteRole === role}
                        onClick={() => {
                          setInviteRole(role);
                          setInviteCreateState({ status: "idle", message: null });
                        }}
                        disabled={isCreatingInvite}
                      >
                        {formatRole(role)}
                      </button>
                    ))}
                  </div>

                  {inviteCreateState.status === "error" ? (
                    <SettingsNotice tone="danger" icon={<AlertCircle aria-hidden="true" />}>
                      {inviteCreateState.message}
                    </SettingsNotice>
                  ) : null}

                  {inviteCreateState.status === "success" ? (
                    <SettingsNotice tone="success" icon={<CheckCircle2 aria-hidden="true" />}>
                      {inviteCreateState.message}
                    </SettingsNotice>
                  ) : null}

                  <div className="workspace-settings-actions">
                    <button
                      className="primary-action primary-action-active"
                      type="submit"
                      disabled={!canCreateInvite}
                    >
                      {isCreatingInvite ? (
                        <Loader2 className="spin" aria-hidden="true" />
                      ) : (
                        <Mail aria-hidden="true" />
                      )}
                      <span>{isCreatingInvite ? "생성 중" : "초대 생성"}</span>
                    </button>
                  </div>
                </form>
              ) : null}

              {isAdmin && createdInvite ? (
                <article className="workspace-invite-created-card">
                  <div className="workspace-member-main">
                    <strong>{createdInvite.email}</strong>
                    <p>{createInvitePath(createdInvite)}</p>
                    <span>만료 {formatDateTime(createdInvite.expiresAt)}</span>
                  </div>
                  <button
                    className="secondary-action workspace-invite-copy"
                    type="button"
                    onClick={() => void copyCreatedInviteUrl()}
                  >
                    <Copy aria-hidden="true" />
                    <span>
                      {copiedInviteToken === createdInvite.token ? "복사됨" : "복사"}
                    </span>
                  </button>
                </article>
              ) : null}
            </section>

            <section
              className={
                activeSettingsSection === "invites"
                  ? "workspace-settings-section"
                  : "workspace-settings-section workspace-settings-section-hidden"
              }
              aria-label="워크스페이스 초대 관리"
              hidden={activeSettingsSection !== "invites"}
            >
              <div className="workspace-section-title">
                <div>
                  <span className="workspace-detail-label">Invites</span>
                  <h2>초대 관리</h2>
                </div>
                <span className={isAdmin ? "workspace-count-badge" : "workspace-lock-badge"}>
                  {isAdmin ? "ADMIN" : "읽기 전용"}
                </span>
              </div>

              {!isAdmin ? (
                <p className="workspace-invite-help">
                  관리자 역할만 초대 목록을 확인할 수 있습니다.
                </p>
              ) : null}

              {isAdmin ? (
                <div className="workspace-invite-management-toolbar" aria-label="초대 상태 필터">
                  <div className="workspace-invite-filters">
                    {INVITE_STATUS_FILTERS.map((filter) => (
                      <button
                        className={
                          filter.value === inviteFilter
                            ? "workspace-invite-filter workspace-invite-filter-active"
                            : "workspace-invite-filter"
                        }
                        type="button"
                        key={filter.value}
                        onClick={() => setInviteFilter(filter.value)}
                        disabled={isInviteLogLoading}
                      >
                        {filter.label}
                      </button>
                    ))}
                  </div>
                  <button
                    className="secondary-action workspace-invite-refresh"
                    type="button"
                    onClick={() => void reloadInviteLog()}
                    disabled={isInviteLogLoading}
                  >
                    <RefreshCw
                      className={isInviteLogLoading ? "spin" : undefined}
                      aria-hidden="true"
                    />
                    <span>새로고침</span>
                  </button>
                </div>
              ) : null}

              {isAdmin && inviteActionState.status === "error" ? (
                <SettingsNotice tone="danger" icon={<AlertCircle aria-hidden="true" />}>
                  {inviteActionState.message}
                </SettingsNotice>
              ) : null}

              {isAdmin && inviteActionState.status === "success" ? (
                <SettingsNotice tone="success" icon={<CheckCircle2 aria-hidden="true" />}>
                  {inviteActionState.message}
                </SettingsNotice>
              ) : null}

              {isAdmin && inviteLogState.status === "loading" ? (
                <div className="workspace-member-empty">
                  <Loader2 className="spin" aria-hidden="true" />
                  <strong>초대 목록 확인 중</strong>
                  <p>워크스페이스 초대 목록을 불러오고 있습니다.</p>
                </div>
              ) : null}

              {isAdmin && inviteLogState.status === "error" ? (
                <SettingsNotice tone="danger" icon={<AlertCircle aria-hidden="true" />}>
                  {inviteLogState.error}
                </SettingsNotice>
              ) : null}

              {isAdmin && inviteLogState.status === "ready" && inviteLogState.invites.length > 0 ? (
                <div className="workspace-settings-member-list">
                  {inviteLogState.invites.map((invite) => (
                    <article className="workspace-settings-member-row" key={invite.inviteId}>
                      <div className="workspace-member-avatar">
                        <Mail aria-hidden="true" />
                      </div>
                      <div className="workspace-member-main">
                        <strong>{invite.email}</strong>
                        <p>{`/invites/${invite.token}`}</p>
                        <span>생성 {formatDateTime(invite.createdAt)}</span>
                      </div>
                      <div className="workspace-settings-member-actions">
                        <div className="workspace-member-meta">
                          <span className={`invite-status-badge invite-status-${invite.status.toLowerCase()}`}>
                            {formatInviteStatus(invite.status)}
                          </span>
                          <span>만료 {formatDateTime(invite.expiresAt)}</span>
                        </div>
                        <button
                          className="secondary-action workspace-invite-copy"
                          type="button"
                          onClick={() => void copyInviteManagementUrl(invite)}
                        >
                          <Copy aria-hidden="true" />
                          <span>{copiedInviteToken === invite.token ? "복사됨" : "복사"}</span>
                        </button>
                        <button
                          className="secondary-action workspace-invite-extend"
                          type="button"
                          onClick={() => void handleExtendInvite(invite)}
                          disabled={!isAdmin || invite.status !== "PENDING" || hasInviteAction}
                        >
                          {inviteActionId === invite.inviteId ? (
                            <Loader2 className="spin" aria-hidden="true" />
                          ) : (
                            <CalendarDays aria-hidden="true" />
                          )}
                          <span>{inviteActionId === invite.inviteId ? "처리 중" : "연장"}</span>
                        </button>
                        <button
                          className="danger-action workspace-invite-delete"
                          type="button"
                          onClick={() => void handleDeleteInvite(invite)}
                          disabled={!isAdmin || invite.status !== "PENDING" || hasInviteAction}
                        >
                          {inviteActionId === invite.inviteId ? (
                            <Loader2 className="spin" aria-hidden="true" />
                          ) : (
                            <Trash2 aria-hidden="true" />
                          )}
                          <span>{inviteActionId === invite.inviteId ? "처리 중" : "취소/삭제"}</span>
                        </button>
                      </div>
                    </article>
                  ))}
                </div>
              ) : null}

              {isAdmin && inviteLogState.status === "ready" && inviteLogState.invites.length === 0 ? (
                <div className="workspace-member-empty">
                  <Mail aria-hidden="true" />
                  <strong>표시할 초대가 없습니다</strong>
                  <p>조건에 맞는 워크스페이스 초대가 생기면 이곳에 표시됩니다.</p>
                </div>
              ) : null}
            </section>

            <section
              className="workspace-settings-section"
              aria-label="Slack 연동 설정"
              hidden={!SHOW_CONNECTION_SETTINGS}
            >
              <div className="workspace-section-title">
                <div>
                  <span className="workspace-detail-label">Slack</span>
                  <h2>연동 설정</h2>
                </div>
                <span className={isAdmin ? "workspace-count-badge" : "workspace-lock-badge"}>
                  {isAdmin ? "ADMIN" : "읽기 전용"}
                </span>
              </div>

              {!isAdmin ? (
                <p className="workspace-github-help">
                  관리자 역할만 Slack 설치와 수동 연동을 관리할 수 있습니다.
                </p>
              ) : null}

              {isAdmin ? (
                <div className="workspace-invite-management-toolbar">
                  <button
                    className="secondary-action workspace-invite-refresh"
                    type="button"
                    onClick={() => void handleOpenSlackInstallUrl()}
                    disabled={isOpeningSlackInstall}
                  >
                    {isOpeningSlackInstall ? (
                      <Loader2 className="spin" aria-hidden="true" />
                    ) : (
                      <KeyRound aria-hidden="true" />
                    )}
                    <span>{isOpeningSlackInstall ? "확인 중" : "Slack 설치 열기"}</span>
                  </button>
                  <button
                    className="secondary-action workspace-invite-refresh"
                    type="button"
                    onClick={() => void reloadSlackIntegrations()}
                    disabled={isSlackIntegrationLoading}
                  >
                    <RefreshCw
                      className={isSlackIntegrationLoading ? "spin" : undefined}
                      aria-hidden="true"
                    />
                    <span>목록 새로고침</span>
                  </button>
                </div>
              ) : null}

              {isAdmin && slackInstallState.status === "error" ? (
                <SettingsNotice tone="danger" icon={<AlertCircle aria-hidden="true" />}>
                  {slackInstallState.message}
                </SettingsNotice>
              ) : null}

              {isAdmin && slackInstallState.status === "success" ? (
                <SettingsNotice tone="success" icon={<CheckCircle2 aria-hidden="true" />}>
                  {slackInstallState.message}
                </SettingsNotice>
              ) : null}

              {isAdmin ? (
                <form
                  className="workspace-settings-form workspace-gateway-form"
                  onSubmit={handleCreateSlackIntegration}
                >
                  <div className="workspace-gateway-form-grid">
                    <label className="form-field">
                      <span>
                        <strong>Team ID</strong>
                        <small>{slackTeamIdInput.length}/120</small>
                      </span>
                      <input
                        value={slackTeamIdInput}
                        onChange={(event) => {
                          setSlackTeamIdInput(event.target.value);
                          setSlackCreateState({ status: "idle", message: null });
                        }}
                        maxLength={120}
                        disabled={!isAdmin || isCreatingSlackIntegration}
                      />
                    </label>

                    <label className="form-field">
                      <span>
                        <strong>Channel ID</strong>
                        <small>{slackChannelIdInput.length}/120</small>
                      </span>
                      <input
                        value={slackChannelIdInput}
                        onChange={(event) => {
                          setSlackChannelIdInput(event.target.value);
                          setSlackCreateState({ status: "idle", message: null });
                        }}
                        maxLength={120}
                        disabled={!isAdmin || isCreatingSlackIntegration}
                      />
                    </label>
                  </div>

                  <div className="workspace-gateway-form-grid">
                    <label className="form-field">
                      <span>
                        <strong>Channel Name</strong>
                        <small>선택</small>
                      </span>
                      <input
                        value={slackChannelNameInput}
                        onChange={(event) => {
                          setSlackChannelNameInput(event.target.value);
                          setSlackCreateState({ status: "idle", message: null });
                        }}
                        maxLength={120}
                        disabled={!isAdmin || isCreatingSlackIntegration}
                      />
                    </label>

                    <label className="form-field">
                      <span>
                        <strong>Bot Token</strong>
                        <small>저장 후 masked token만 표시됩니다</small>
                      </span>
                      <input
                        value={slackBotTokenInput}
                        onChange={(event) => {
                          setSlackBotTokenInput(event.target.value);
                          setSlackCreateState({ status: "idle", message: null });
                        }}
                        type="password"
                        autoComplete="off"
                        disabled={!isAdmin || isCreatingSlackIntegration}
                      />
                    </label>
                  </div>

                  {slackCreateState.status === "error" ? (
                    <SettingsNotice tone="danger" icon={<AlertCircle aria-hidden="true" />}>
                      {slackCreateState.message}
                    </SettingsNotice>
                  ) : null}

                  {slackCreateState.status === "success" ? (
                    <SettingsNotice tone="success" icon={<CheckCircle2 aria-hidden="true" />}>
                      {slackCreateState.message}
                    </SettingsNotice>
                  ) : null}

                  <div className="workspace-settings-actions">
                    <button
                      className="primary-action primary-action-active"
                      type="submit"
                      disabled={!canCreateSlackIntegration}
                    >
                      {isCreatingSlackIntegration ? (
                        <Loader2 className="spin" aria-hidden="true" />
                      ) : (
                        <KeyRound aria-hidden="true" />
                      )}
                      <span>{isCreatingSlackIntegration ? "등록 중" : "등록"}</span>
                    </button>
                  </div>
                </form>
              ) : null}

              {isAdmin && isSlackIntegrationLoading ? (
                <div className="workspace-member-empty">
                  <Loader2 className="spin" aria-hidden="true" />
                  <strong>Slack 연동 확인 중</strong>
                  <p>워크스페이스에 등록된 Slack 연동을 불러오고 있습니다.</p>
                </div>
              ) : null}

              {isAdmin && slackIntegrationState.status === "error" ? (
                <SettingsNotice tone="danger" icon={<AlertCircle aria-hidden="true" />}>
                  {slackIntegrationState.error}
                </SettingsNotice>
              ) : null}

              {isAdmin && slackActionState.status === "error" ? (
                <SettingsNotice tone="danger" icon={<AlertCircle aria-hidden="true" />}>
                  {slackActionState.message}
                </SettingsNotice>
              ) : null}

              {isAdmin && slackActionState.status === "success" ? (
                <SettingsNotice tone="success" icon={<CheckCircle2 aria-hidden="true" />}>
                  {slackActionState.message}
                </SettingsNotice>
              ) : null}

              {isAdmin &&
              slackIntegrationState.status === "ready" &&
              slackIntegrationState.integrations.length > 0 ? (
                <div className="workspace-settings-member-list">
                  {slackIntegrationState.integrations.map((integration) => {
                    const isEditing = editingSlackIntegrationId === integration.id;
                    const updateBusy =
                      slackAction?.type === "update" &&
                      slackAction.integrationId === integration.id;
                    const deleteBusy =
                      slackAction?.type === "delete" &&
                      slackAction.integrationId === integration.id;

                    return (
                      <article className="workspace-settings-member-row" key={integration.id}>
                        <div className="workspace-member-avatar">
                          <Hash aria-hidden="true" />
                        </div>

                        {isEditing ? (
                          <form
                            className="workspace-github-edit-form"
                            onSubmit={(event) =>
                              void handleUpdateSlackIntegration(event, integration)
                            }
                          >
                            <label className="form-field workspace-github-edit-field">
                              <span>
                                <strong>Team ID</strong>
                              </span>
                              <input
                                value={slackEditTeamId}
                                onChange={(event) => {
                                  setSlackEditTeamId(event.target.value);
                                  setSlackActionState({ status: "idle", message: null });
                                }}
                                maxLength={120}
                                disabled={hasSlackIntegrationAction}
                              />
                            </label>
                            <label className="form-field workspace-github-edit-field">
                              <span>
                                <strong>Channel ID</strong>
                              </span>
                              <input
                                value={slackEditChannelId}
                                onChange={(event) => {
                                  setSlackEditChannelId(event.target.value);
                                  setSlackActionState({ status: "idle", message: null });
                                }}
                                maxLength={120}
                                disabled={hasSlackIntegrationAction}
                              />
                            </label>
                            <label className="form-field workspace-github-edit-field">
                              <span>
                                <strong>Channel Name</strong>
                              </span>
                              <input
                                value={slackEditChannelName}
                                onChange={(event) => {
                                  setSlackEditChannelName(event.target.value);
                                  setSlackActionState({ status: "idle", message: null });
                                }}
                                maxLength={120}
                                disabled={hasSlackIntegrationAction}
                              />
                            </label>
                            <label className="form-field workspace-github-edit-field">
                              <span>
                                <strong>새 bot token</strong>
                              </span>
                              <input
                                value={slackEditBotToken}
                                onChange={(event) => {
                                  setSlackEditBotToken(event.target.value);
                                  setSlackActionState({ status: "idle", message: null });
                                }}
                                type="password"
                                autoComplete="off"
                                disabled={hasSlackIntegrationAction}
                              />
                            </label>
                            <div className="workspace-github-row-actions">
                              <button
                                className="primary-action primary-action-active workspace-github-row-button"
                                type="submit"
                                disabled={
                                  hasSlackIntegrationAction ||
                                  !slackEditTeamId.trim() ||
                                  !slackEditChannelId.trim()
                                }
                              >
                                {updateBusy ? (
                                  <Loader2 className="spin" aria-hidden="true" />
                                ) : (
                                  <Save aria-hidden="true" />
                                )}
                                <span>{updateBusy ? "저장 중" : "저장"}</span>
                              </button>
                              <button
                                className="secondary-action workspace-github-row-button"
                                type="button"
                                onClick={cancelSlackIntegrationEdit}
                                disabled={hasSlackIntegrationAction}
                              >
                                취소
                              </button>
                            </div>
                          </form>
                        ) : (
                          <>
                            <div className="workspace-member-main">
                              <strong>{integration.channelName ?? integration.slackChannelId}</strong>
                              <p>{`${integration.slackTeamId} · ${integration.slackChannelId}`}</p>
                              <span>{`Token ${integration.maskedBotToken}`}</span>
                            </div>
                            <div className="workspace-settings-member-actions">
                              <div className="workspace-member-meta">
                                <span className="role-badge">
                                  {integration.active ? "활성" : "비활성"}
                                </span>
                                <span>{`Integration #${integration.id}`}</span>
                              </div>
                              <button
                                className="secondary-action workspace-github-row-button"
                                type="button"
                                onClick={() => startSlackIntegrationEdit(integration)}
                                disabled={hasSlackIntegrationAction}
                              >
                                수정
                              </button>
                              <button
                                className="danger-action workspace-github-row-button"
                                type="button"
                                onClick={() => void handleDeleteSlackIntegration(integration)}
                                disabled={hasSlackIntegrationAction}
                              >
                                {deleteBusy ? (
                                  <Loader2 className="spin" aria-hidden="true" />
                                ) : (
                                  <Trash2 aria-hidden="true" />
                                )}
                                <span>{deleteBusy ? "삭제 중" : "삭제"}</span>
                              </button>
                            </div>
                          </>
                        )}
                      </article>
                    );
                  })}
                </div>
              ) : null}

              {isAdmin &&
              slackIntegrationState.status === "ready" &&
              slackIntegrationState.integrations.length === 0 ? (
                <div className="workspace-member-empty">
                  <Hash aria-hidden="true" />
                  <strong>등록된 Slack 연동이 없습니다</strong>
                  <p>Slack 앱 설치 또는 수동 등록을 완료하면 이곳에 표시됩니다.</p>
                </div>
              ) : null}
            </section>

            <section
              className="workspace-settings-section"
              aria-label="Slack 채널 매핑 설정"
              hidden={!SHOW_CONNECTION_SETTINGS}
            >
              <div className="workspace-section-title">
                <div>
                  <span className="workspace-detail-label">Slack</span>
                  <h2>채널 매핑</h2>
                </div>
                <span className={isAdmin ? "workspace-count-badge" : "workspace-lock-badge"}>
                  {isAdmin ? `${slackChannelMappings.length}개` : "읽기 전용"}
                </span>
              </div>

              {!isAdmin ? (
                <p className="workspace-github-help">
                  관리자 역할만 Slack 채널 매핑을 등록하고 확인할 수 있습니다.
                </p>
              ) : null}

              {isAdmin ? (
                <div className="workspace-invite-management-toolbar">
                  <button
                    className="secondary-action workspace-invite-refresh"
                    type="button"
                    onClick={() => void reloadSlackChannelMappings()}
                    disabled={isSlackChannelMappingLoading}
                  >
                    <RefreshCw
                      className={isSlackChannelMappingLoading ? "spin" : undefined}
                      aria-hidden="true"
                    />
                    <span>목록 새로고침</span>
                  </button>
                </div>
              ) : null}

              {isAdmin ? (
                <form
                  className="workspace-settings-form workspace-gateway-form"
                  onSubmit={handleCreateSlackChannelMapping}
                >
                  <div className="workspace-gateway-form-grid">
                    <label className="form-field">
                      <span>
                        <strong>Team ID</strong>
                        <small>{slackMappingTeamIdInput.length}/120</small>
                      </span>
                      <input
                        value={slackMappingTeamIdInput}
                        onChange={(event) => {
                          setSlackMappingTeamIdInput(event.target.value);
                          setSlackMappingCreateState({ status: "idle", message: null });
                        }}
                        maxLength={120}
                        disabled={!isAdmin || isCreatingSlackChannelMapping}
                      />
                    </label>

                    <label className="form-field">
                      <span>
                        <strong>Channel ID</strong>
                        <small>{slackMappingChannelIdInput.length}/120</small>
                      </span>
                      <input
                        value={slackMappingChannelIdInput}
                        onChange={(event) => {
                          setSlackMappingChannelIdInput(event.target.value);
                          setSlackMappingCreateState({ status: "idle", message: null });
                        }}
                        maxLength={120}
                        disabled={!isAdmin || isCreatingSlackChannelMapping}
                      />
                    </label>
                  </div>

                  <div className="workspace-gateway-form-grid">
                    <label className="form-field">
                      <span>
                        <strong>Channel Name</strong>
                        <small>선택</small>
                      </span>
                      <input
                        value={slackMappingChannelNameInput}
                        onChange={(event) => {
                          setSlackMappingChannelNameInput(event.target.value);
                          setSlackMappingCreateState({ status: "idle", message: null });
                        }}
                        maxLength={120}
                        disabled={!isAdmin || isCreatingSlackChannelMapping}
                      />
                    </label>

                    <label className="form-field">
                      <span>
                        <strong>Bot Token</strong>
                        <small>저장 후 masked token만 표시됩니다</small>
                      </span>
                      <input
                        value={slackMappingBotTokenInput}
                        onChange={(event) => {
                          setSlackMappingBotTokenInput(event.target.value);
                          setSlackMappingCreateState({ status: "idle", message: null });
                        }}
                        type="password"
                        autoComplete="off"
                        disabled={!isAdmin || isCreatingSlackChannelMapping}
                      />
                    </label>
                  </div>

                  {slackMappingCreateState.status === "error" ? (
                    <SettingsNotice tone="danger" icon={<AlertCircle aria-hidden="true" />}>
                      {slackMappingCreateState.message}
                    </SettingsNotice>
                  ) : null}

                  {slackMappingCreateState.status === "success" ? (
                    <SettingsNotice tone="success" icon={<CheckCircle2 aria-hidden="true" />}>
                      {slackMappingCreateState.message}
                    </SettingsNotice>
                  ) : null}

                  <div className="workspace-settings-actions">
                    <button
                      className="primary-action primary-action-active"
                      type="submit"
                      disabled={!canCreateSlackChannelMapping}
                    >
                      {isCreatingSlackChannelMapping ? (
                        <Loader2 className="spin" aria-hidden="true" />
                      ) : (
                        <Hash aria-hidden="true" />
                      )}
                      <span>{isCreatingSlackChannelMapping ? "등록 중" : "등록"}</span>
                    </button>
                  </div>
                </form>
              ) : null}

              {isAdmin && isSlackChannelMappingLoading ? (
                <div className="workspace-member-empty">
                  <Loader2 className="spin" aria-hidden="true" />
                  <strong>Slack 채널 매핑 확인 중</strong>
                  <p>워크스페이스에 등록된 Slack 채널 매핑을 불러오고 있습니다.</p>
                </div>
              ) : null}

              {isAdmin && slackChannelMappingState.status === "error" ? (
                <SettingsNotice tone="danger" icon={<AlertCircle aria-hidden="true" />}>
                  {slackChannelMappingState.error}
                </SettingsNotice>
              ) : null}

              {isAdmin &&
              slackChannelMappingState.status === "ready" &&
              slackChannelMappings.length > 0 ? (
                <div className="workspace-settings-member-list">
                  {slackChannelMappings.map((mapping) => (
                    <article className="workspace-settings-member-row" key={mapping.mappingId}>
                      <div className="workspace-member-avatar">
                        <Hash aria-hidden="true" />
                      </div>
                      <div className="workspace-member-main">
                        <strong>{mapping.channelName ?? mapping.channelId}</strong>
                        <p>{`${mapping.teamId} · ${mapping.channelId}`}</p>
                        <span>{`Token ${mapping.maskedBotToken}`}</span>
                      </div>
                      <div className="workspace-settings-member-actions">
                        <div className="workspace-member-meta">
                          <span className="role-badge">
                            {mapping.active ? "활성" : "비활성"}
                          </span>
                          <span>{`Mapping #${mapping.mappingId}`}</span>
                        </div>
                      </div>
                    </article>
                  ))}
                </div>
              ) : null}

              {isAdmin &&
              slackChannelMappingState.status === "ready" &&
              slackChannelMappings.length === 0 ? (
                <div className="workspace-member-empty">
                  <Hash aria-hidden="true" />
                  <strong>등록된 Slack 채널 매핑이 없습니다</strong>
                  <p>팀과 채널을 연결하면 이곳에서 매핑 상태를 확인할 수 있습니다.</p>
                </div>
              ) : null}
            </section>

            <section
              className="workspace-settings-section"
              aria-label="Gateway 설정"
              hidden={!SHOW_CONNECTION_SETTINGS}
            >
              <div className="workspace-section-title">
                <div>
                  <span className="workspace-detail-label">Gateway</span>
                  <h2>연결 상태</h2>
                </div>
                <span className={isAdmin ? "workspace-count-badge" : "workspace-lock-badge"}>
                  {isAdmin ? "ADMIN" : "읽기 전용"}
                </span>
              </div>

              {!isAdmin ? (
                <p className="workspace-github-help">
                  관리자 역할만 gateway 연결 상태를 확인하고 등록할 수 있습니다.
                </p>
              ) : null}

              {isAdmin ? (
                <form
                  className="workspace-settings-form workspace-gateway-form"
                  onSubmit={handleCreateGatewayBinding}
                >
                  <div className="workspace-gateway-form-grid">
                    <label className="form-field">
                      <span>
                        <strong>Gateway URL</strong>
                        <small>http:// 또는 https://</small>
                      </span>
                      <input
                        value={gatewayUrlInput}
                        onChange={(event) => {
                          setGatewayUrlInput(event.target.value);
                          setGatewayConnectionTestState({
                            status: "idle",
                            result: null,
                            message: null,
                          });
                          setGatewayBindingState({ status: "idle", message: null });
                        }}
                        placeholder="https://gateway.example.com"
                        disabled={!isAdmin || isTestingGateway || isBindingGateway}
                      />
                    </label>

                    <label className="form-field">
                      <span>
                        <strong>Token</strong>
                        <small>저장 후 masked token만 표시됩니다</small>
                      </span>
                      <input
                        value={gatewayTokenInput}
                        onChange={(event) => {
                          setGatewayTokenInput(event.target.value);
                          setGatewayConnectionTestState({
                            status: "idle",
                            result: null,
                            message: null,
                          });
                          setGatewayBindingState({ status: "idle", message: null });
                        }}
                        type="password"
                        autoComplete="off"
                        disabled={!isAdmin || isTestingGateway || isBindingGateway}
                      />
                    </label>
                  </div>

                  {normalizedGatewayForm.urlError && gatewayUrlInput.trim().length > 0 ? (
                    <SettingsNotice tone="danger" icon={<AlertCircle aria-hidden="true" />}>
                      {normalizedGatewayForm.urlError}
                    </SettingsNotice>
                  ) : null}

                  {gatewayConnectionTestState.status === "error" ? (
                    <SettingsNotice tone="danger" icon={<AlertCircle aria-hidden="true" />}>
                      {gatewayConnectionTestState.message}
                    </SettingsNotice>
                  ) : null}

                  {gatewayConnectionTestState.status === "success" ? (
                    <SettingsNotice tone="success" icon={<CheckCircle2 aria-hidden="true" />}>
                      {`${gatewayConnectionStatusLabel(gatewayConnectionTestState.result.status)} · ${
                        gatewayConnectionTestState.message
                      } · agent ${gatewayConnectionTestState.result.agentCount}`}
                    </SettingsNotice>
                  ) : null}

                  {gatewayBindingState.status === "error" ? (
                    <SettingsNotice tone="danger" icon={<AlertCircle aria-hidden="true" />}>
                      {gatewayBindingState.message}
                    </SettingsNotice>
                  ) : null}

                  {gatewayBindingState.status === "success" ? (
                    <SettingsNotice tone="success" icon={<CheckCircle2 aria-hidden="true" />}>
                      {gatewayBindingState.message}
                    </SettingsNotice>
                  ) : null}

                  <div className="workspace-settings-actions workspace-gateway-actions">
                    <button
                      className="secondary-action"
                      type="button"
                      onClick={() => void handleTestGatewayConnection()}
                      disabled={!canSubmitGateway}
                    >
                      {isTestingGateway ? (
                        <Loader2 className="spin" aria-hidden="true" />
                      ) : (
                        <RefreshCw aria-hidden="true" />
                      )}
                      <span>{isTestingGateway ? "테스트 중" : "연결 테스트"}</span>
                    </button>
                    <button
                      className="primary-action primary-action-active"
                      type="submit"
                      disabled={!canSubmitGateway}
                    >
                      {isBindingGateway ? (
                        <Loader2 className="spin" aria-hidden="true" />
                      ) : (
                        <KeyRound aria-hidden="true" />
                      )}
                      <span>{isBindingGateway ? "등록 중" : "Binding 등록"}</span>
                    </button>
                  </div>
                </form>
              ) : null}

              {isAdmin ? (
                <div className="workspace-invite-management-toolbar">
                  <button
                    className="secondary-action workspace-invite-refresh"
                    type="button"
                    onClick={() => void reloadGatewayStatus()}
                    disabled={isGatewayStatusLoading}
                  >
                    <RefreshCw
                      className={isGatewayStatusLoading ? "spin" : undefined}
                      aria-hidden="true"
                    />
                    <span>상태 새로고침</span>
                  </button>
                </div>
              ) : null}

              {isAdmin && gatewayStatusState.status === "loading" ? (
                <div className="workspace-member-empty">
                  <Loader2 className="spin" aria-hidden="true" />
                  <strong>gateway 상태 확인 중</strong>
                  <p>워크스페이스 gateway binding 상태를 불러오고 있습니다.</p>
                </div>
              ) : null}

              {isAdmin && gatewayStatusState.status === "error" ? (
                <SettingsNotice tone="danger" icon={<AlertCircle aria-hidden="true" />}>
                  {gatewayStatusState.error}
                </SettingsNotice>
              ) : null}

              {isAdmin && gatewayStatusState.status === "ready" ? (
                <div className="workspace-settings-member-list">
                  <article className="workspace-settings-member-row">
                    <div className="workspace-member-avatar">
                      <KeyRound aria-hidden="true" />
                    </div>
                    <div className="workspace-member-main">
                      <strong>{gatewayStatusLabel(gatewayStatusState.gateway.status)}</strong>
                      <p>{gatewayStatusState.gateway.gatewayUrl ?? "등록된 gateway URL이 없습니다"}</p>
                      <span>
                        {gatewayStatusState.gateway.maskedToken
                          ? `Token ${gatewayStatusState.gateway.maskedToken}`
                          : "Token 미등록"}
                      </span>
                    </div>
                    <div className="workspace-settings-member-actions">
                      <div className="workspace-member-meta">
                        <span className="role-badge">
                          {gatewayStatusState.gateway.mode ?? "UNBOUND"}
                        </span>
                        <span>
                          {gatewayStatusState.gateway.updatedAt
                            ? `수정 ${formatDateTime(gatewayStatusState.gateway.updatedAt)}`
                            : "수정 이력 없음"}
                        </span>
                      </div>
                    </div>
                  </article>
                </div>
              ) : null}
            </section>

            <section
              className="workspace-settings-section"
              aria-label="GitHub 자격 증명 관리"
              hidden={!SHOW_CONNECTION_SETTINGS}
            >
              <div className="workspace-section-title">
                <div>
                  <span className="workspace-detail-label">GitHub</span>
                  <h2>자격 증명 관리</h2>
                </div>
                <span className={isAdmin ? "workspace-count-badge" : "workspace-lock-badge"}>
                  {isAdmin ? "ADMIN" : "읽기 전용"}
                </span>
              </div>

              {!isAdmin ? (
                <p className="workspace-github-help">
                  관리자 역할만 GitHub 자격 증명을 관리할 수 있습니다.
                </p>
              ) : null}

              {isAdmin ? (
                <form
                  className="workspace-settings-form"
                  onSubmit={handleCreateGithubCredential}
                >
                  <label className="form-field">
                    <span>
                      <strong>표시 이름</strong>
                      <small>{githubDisplayNameInput.length}/120</small>
                    </span>
                    <input
                      value={githubDisplayNameInput}
                      onChange={(event) => {
                        setGithubDisplayNameInput(event.target.value);
                        setGithubCreateState({ status: "idle", message: null });
                      }}
                      maxLength={120}
                      disabled={!isAdmin || isCreatingGithubCredential}
                    />
                  </label>

                  <label className="form-field">
                    <span>
                      <strong>Token</strong>
                      <small>저장 후 masked token만 표시됩니다</small>
                    </span>
                    <input
                      value={githubTokenInput}
                      onChange={(event) => {
                        setGithubTokenInput(event.target.value);
                        setGithubCreateState({ status: "idle", message: null });
                      }}
                      type="password"
                      autoComplete="off"
                      disabled={!isAdmin || isCreatingGithubCredential}
                    />
                  </label>

                  {githubCreateState.status === "error" ? (
                    <SettingsNotice tone="danger" icon={<AlertCircle aria-hidden="true" />}>
                      {githubCreateState.message}
                    </SettingsNotice>
                  ) : null}

                  {githubCreateState.status === "success" ? (
                    <SettingsNotice tone="success" icon={<CheckCircle2 aria-hidden="true" />}>
                      {githubCreateState.message}
                    </SettingsNotice>
                  ) : null}

                  <div className="workspace-settings-actions">
                    <button
                      className="primary-action primary-action-active"
                      type="submit"
                      disabled={!canCreateGithubCredential}
                    >
                      {isCreatingGithubCredential ? (
                        <Loader2 className="spin" aria-hidden="true" />
                      ) : (
                        <KeyRound aria-hidden="true" />
                      )}
                      <span>{isCreatingGithubCredential ? "등록 중" : "등록"}</span>
                    </button>
                  </div>
                </form>
              ) : null}

              {isAdmin && isGithubCredentialLoading ? (
                <div className="workspace-member-empty">
                  <Loader2 className="spin" aria-hidden="true" />
                  <strong>자격 증명 확인 중</strong>
                  <p>워크스페이스에 등록된 GitHub 자격 증명을 불러오고 있습니다.</p>
                </div>
              ) : null}

              {isAdmin && githubCredentialState.status === "error" ? (
                <SettingsNotice tone="danger" icon={<AlertCircle aria-hidden="true" />}>
                  {githubCredentialState.error}
                </SettingsNotice>
              ) : null}

              {isAdmin && githubActionState.status === "error" ? (
                <SettingsNotice tone="danger" icon={<AlertCircle aria-hidden="true" />}>
                  {githubActionState.message}
                </SettingsNotice>
              ) : null}

              {isAdmin && githubActionState.status === "success" ? (
                <SettingsNotice tone="success" icon={<CheckCircle2 aria-hidden="true" />}>
                  {githubActionState.message}
                </SettingsNotice>
              ) : null}

              {isAdmin &&
              githubCredentialState.status === "ready" &&
              githubCredentialState.credentials.length > 0 ? (
                <div className="workspace-settings-member-list">
                  {githubCredentialState.credentials.map((credential) => {
                    const isEditing = editingGithubCredentialId === credential.credentialId;
                    const updateBusy =
                      githubAction?.type === "update" &&
                      githubAction.credentialId === credential.credentialId;
                    const deleteBusy =
                      githubAction?.type === "delete" &&
                      githubAction.credentialId === credential.credentialId;

                    return (
                      <article
                        className="workspace-settings-member-row"
                        key={credential.credentialId}
                      >
                        <div className="workspace-member-avatar">
                          <GitBranch aria-hidden="true" />
                        </div>

                        {isEditing ? (
                          <form
                            className="workspace-github-edit-form"
                            onSubmit={(event) =>
                              void handleUpdateGithubCredential(event, credential)
                            }
                          >
                            <label className="form-field workspace-github-edit-field">
                              <span>
                                <strong>표시 이름</strong>
                              </span>
                              <input
                                value={githubEditDisplayName}
                                onChange={(event) => {
                                  setGithubEditDisplayName(event.target.value);
                                  setGithubActionState({ status: "idle", message: null });
                                }}
                                maxLength={120}
                                disabled={hasGithubCredentialAction}
                              />
                            </label>
                            <label className="form-field workspace-github-edit-field">
                              <span>
                                <strong>새 token</strong>
                              </span>
                              <input
                                value={githubEditToken}
                                onChange={(event) => {
                                  setGithubEditToken(event.target.value);
                                  setGithubActionState({ status: "idle", message: null });
                                }}
                                type="password"
                                autoComplete="off"
                                disabled={hasGithubCredentialAction}
                              />
                            </label>
                            <div className="workspace-github-row-actions">
                              <button
                                className="primary-action primary-action-active workspace-github-row-button"
                                type="submit"
                                disabled={hasGithubCredentialAction || !githubEditDisplayName.trim()}
                              >
                                {updateBusy ? (
                                  <Loader2 className="spin" aria-hidden="true" />
                                ) : (
                                  <Save aria-hidden="true" />
                                )}
                                <span>{updateBusy ? "저장 중" : "저장"}</span>
                              </button>
                              <button
                                className="secondary-action workspace-github-row-button"
                                type="button"
                                onClick={cancelGithubCredentialEdit}
                                disabled={hasGithubCredentialAction}
                              >
                                취소
                              </button>
                            </div>
                          </form>
                        ) : (
                          <>
                            <div className="workspace-member-main">
                              <strong>{credential.displayName}</strong>
                              <p className="workspace-github-token">{credential.maskedToken}</p>
                              <span>{`Credential #${credential.credentialId}`}</span>
                            </div>
                            <div className="workspace-settings-member-actions">
                              <div className="workspace-member-meta">
                                <span className="role-badge">등록됨</span>
                                <span>{`Workspace #${credential.workspaceId}`}</span>
                              </div>
                              <button
                                className="secondary-action workspace-github-row-button"
                                type="button"
                                onClick={() => startGithubCredentialEdit(credential)}
                                disabled={hasGithubCredentialAction}
                              >
                                수정
                              </button>
                              <button
                                className="danger-action workspace-github-row-button"
                                type="button"
                                onClick={() => void handleDeleteGithubCredential(credential)}
                                disabled={hasGithubCredentialAction}
                              >
                                {deleteBusy ? (
                                  <Loader2 className="spin" aria-hidden="true" />
                                ) : (
                                  <Trash2 aria-hidden="true" />
                                )}
                                <span>{deleteBusy ? "삭제 중" : "삭제"}</span>
                              </button>
                            </div>
                          </>
                        )}
                      </article>
                    );
                  })}
                </div>
              ) : null}

              {isAdmin &&
              githubCredentialState.status === "ready" &&
              githubCredentialState.credentials.length === 0 ? (
                <div className="workspace-member-empty">
                  <KeyRound aria-hidden="true" />
                  <strong>등록된 자격 증명이 없습니다</strong>
                  <p>GitHub token을 등록하면 이곳에 masked token으로 표시됩니다.</p>
                </div>
              ) : null}
            </section>
          </section>
        ) : null}
      </section>

      <aside className="workspace-settings-side" aria-label="설정 조회 정보">
        <div>
          <UserRound aria-hidden="true" />
          <strong>{resolveAccessToken() ? "설정됨" : "미설정"}</strong>
          <span>개발 access token</span>
        </div>
        <div>
          <Hash aria-hidden="true" />
          <strong>{isValidWorkspaceId(numericWorkspaceId) ? numericWorkspaceId : "-"}</strong>
          <span>요청 워크스페이스 ID</span>
        </div>
        <div>
          <ShieldCheck aria-hidden="true" />
          <strong>{isAdmin ? "관리 가능" : "읽기 전용"}</strong>
          <span>멤버 {members.length}명 · 관리자 {adminCount}명</span>
        </div>
      </aside>
    </main>
  );
}

function WorkspaceSettingsStateMessage({
  tone,
  icon,
  title,
  message,
  action,
}: {
  tone: "neutral" | "danger" | "offline";
  icon: ReactNode;
  title: string;
  message: string;
  action?: ReactNode;
}) {
  return (
    <div className={`workspace-detail-state workspace-detail-state-${tone}`}>
      {icon}
      <strong>{title}</strong>
      <p>{message}</p>
      {action}
    </div>
  );
}

function SettingsMetaItem({
  icon,
  label,
  value,
}: {
  icon: ReactNode;
  label: string;
  value: ReactNode;
}) {
  return (
    <div>
      <dt>
        {icon}
        <span>{label}</span>
      </dt>
      <dd>{value}</dd>
    </div>
  );
}

function SettingsNotice({
  tone,
  icon,
  children,
}: {
  tone: "success" | "danger";
  icon: ReactNode;
  children: ReactNode;
}) {
  return (
    <div className={`workspace-settings-notice workspace-settings-notice-${tone}`} role="status">
      {icon}
      <span>{children}</span>
    </div>
  );
}

function IntegrationSettingsActionCard({ action }: { action: IntegrationSettingsAction }) {
  return (
    <article className="workspace-integration-action-card">
      <div>
        <strong>{action.label}</strong>
        <p>{action.detail}</p>
      </div>
      <span
        className={`workspace-integration-action-state workspace-integration-action-state-${action.state}`}
      >
        {formatIntegrationSettingsActionState(action)}
      </span>
    </article>
  );
}

function RoleSegmentedControl({
  role,
  busy,
  disabled,
  disableAdminDemotion,
  onChange,
}: {
  role: WorkspaceMemberRole;
  busy: boolean;
  disabled: boolean;
  disableAdminDemotion: boolean;
  onChange: (role: WorkspaceMemberRole) => void;
}) {
  const roles: WorkspaceMemberRole[] = ["ADMIN", "MEMBER"];

  return (
    <div className="workspace-role-control" role="group" aria-label="멤버 역할 변경">
      {roles.map((candidateRole) => {
        const active = candidateRole === role;
        const blockedLastAdmin = disableAdminDemotion && role === "ADMIN" && candidateRole === "MEMBER";
        return (
          <button
            className={active ? "workspace-role-option workspace-role-option-active" : "workspace-role-option"}
            type="button"
            key={candidateRole}
            aria-pressed={active}
            onClick={() => {
              if (!active && !blockedLastAdmin) {
                onChange(candidateRole);
              }
            }}
            disabled={disabled || active || blockedLastAdmin}
          >
            {busy && active ? "..." : formatRole(candidateRole)}
          </button>
        );
      })}
    </div>
  );
}

function formatIntegrationSettingsReadiness(readiness: string): string {
  switch (readiness) {
    case "ready":
      return "준비 완료";
    case "partial":
      return "일부 준비";
    case "empty":
      return "설정 필요";
    default:
      return "확인 필요";
  }
}

function formatIntegrationSettingsActionState(action: IntegrationSettingsAction): string {
  if (action.state === "ready") {
    return action.target === "oauth" ? "연결됨" : "준비됨";
  }
  if (action.state === "attention") {
    return "확인 필요";
  }
  return "대기";
}

function toFailureState(error: unknown, fallback: string): WorkspaceSettingsState {
  if (error instanceof ApiError) {
    return {
      status: "error",
      workspace: null,
      members: [],
      error: error.message,
    };
  }

  return {
    status: "offline",
    workspace: null,
    members: [],
    error: fallback,
  };
}

function messageFromError(error: unknown, fallback: string): string {
  if (error instanceof ApiError) {
    return error.message;
  }

  return fallback;
}

function isValidWorkspaceId(workspaceId: number): boolean {
  return Number.isInteger(workspaceId) && workspaceId > 0;
}

function formatRole(role: WorkspaceMemberRole): string {
  return role === "ADMIN" ? "관리자" : "멤버";
}

function formatInviteStatus(status: WorkspaceInviteManagementStatus): string {
  switch (status) {
    case "PENDING":
      return "대기";
    case "ACCEPTED":
      return "수락";
    case "EXPIRED":
      return "만료";
    case "REVOKED":
      return "취소";
  }
}

function toInviteStatusParam(
  filter: InviteStatusFilter,
): WorkspaceInviteManagementStatus | undefined {
  return filter === "ALL" ? undefined : filter;
}

function updateInviteLogAfterRevoke(
  invites: WorkspaceInviteManagement[],
  inviteId: number,
  filter: InviteStatusFilter,
): WorkspaceInviteManagement[] {
  if (filter === "PENDING") {
    return invites.filter((invite) => invite.inviteId !== inviteId);
  }

  const revokedAt = new Date().toISOString();
  return invites.map((invite) =>
    invite.inviteId === inviteId
      ? { ...invite, status: "REVOKED", revokedAt }
    : invite,
  );
}

function mergeInviteLog(
  invites: WorkspaceInviteManagement[],
  invite: WorkspaceInviteManagement,
): WorkspaceInviteManagement[] {
  const exists = invites.some((currentInvite) => currentInvite.inviteId === invite.inviteId);
  if (!exists) {
    return [invite, ...invites];
  }

  return invites.map((currentInvite) =>
    currentInvite.inviteId === invite.inviteId ? invite : currentInvite,
  );
}

function parseInviteExpiresInDays(value: string): number | null {
  const parsed = Number(value);
  if (!Number.isInteger(parsed) || parsed < 1 || parsed > 30) {
    return null;
  }

  return parsed;
}

function createInvitePath(invite: Pick<WorkspaceInvite, "token">): string {
  return `/invites/${invite.token}`;
}

function createInviteUrl(invite: Pick<WorkspaceInvite, "token">): string {
  if (typeof window === "undefined") {
    return createInvitePath(invite);
  }

  return new URL(createInvitePath(invite), window.location.origin).toString();
}

async function writeClipboardText(value: string): Promise<void> {
  if (!navigator.clipboard) {
    throw new Error("clipboard unavailable");
  }

  await navigator.clipboard.writeText(value);
}

function isLastAdmin(
  member: WorkspaceMember,
  adminCount: number,
  nextRole?: WorkspaceMemberRole,
): boolean {
  if (member.role !== "ADMIN" || adminCount > 1) {
    return false;
  }

  return nextRole === undefined || nextRole !== "ADMIN";
}

function formatDateTime(value: string): string {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return "-";
  }

  return new Intl.DateTimeFormat("ko-KR", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(date);
}
