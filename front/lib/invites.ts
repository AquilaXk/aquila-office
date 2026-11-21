// API module alias for Next consumers: @/lib/api/invites
// Delegated workspace mutations keep the accessToken contract in the domain API module.
export {
  acceptWorkspaceInvite,
  createWorkspaceInvite,
  deleteWorkspaceInvite,
  extendWorkspaceInvite,
  fetchWorkspaceInvite,
  fetchWorkspaceInvites,
  revokeWorkspaceInvite,
} from "./api/invites.ts";
export type {
  AcceptedWorkspaceInvite,
  CreateWorkspaceInviteInput,
  ExtendWorkspaceInviteInput,
  WorkspaceInvite,
  WorkspaceInviteManagement,
  WorkspaceInviteManagementStatus,
  WorkspaceInviteStatus,
} from "./api/invites.ts";
