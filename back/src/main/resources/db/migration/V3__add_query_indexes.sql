create index if not exists idx_workspaces_created_by_member_id
    on workspaces (created_by_member_id);

create index if not exists idx_workspace_members_member_id
    on workspace_members (member_id);

create index if not exists idx_workspace_invites_workspace_id
    on workspace_invites (workspace_id);

create index if not exists idx_workspace_gateway_bindings_workspace_id
    on workspace_gateway_bindings (workspace_id);

create index if not exists idx_github_credentials_workspace_id
    on github_credentials (workspace_id);

create index if not exists idx_slack_channel_mappings_workspace_id
    on slack_channel_mappings (workspace_id);

create index if not exists idx_workspace_agents_workspace_id
    on workspace_agents (workspace_id);

create index if not exists idx_workspace_agent_skill_files_agent_id
    on workspace_agent_skill_files (agent_id);

create index if not exists idx_tasks_workspace_status
    on tasks (workspace_id, status);

create index if not exists idx_task_executions_task_id
    on task_executions (task_id);

create index if not exists idx_task_executions_workspace_status
    on task_executions (workspace_id, status);

create index if not exists idx_task_execution_logs_execution_created_at
    on task_execution_logs (execution_id, created_at);

create index if not exists idx_chat_sessions_workspace_id
    on chat_sessions (workspace_id);

create index if not exists idx_chat_messages_session_created_at
    on chat_messages (chat_session_id, created_at);

create index if not exists idx_chat_messages_workspace_id
    on chat_messages (workspace_id);

create index if not exists idx_orchestration_sessions_workspace_id
    on orchestration_sessions (workspace_id);

create index if not exists idx_orchestration_steps_session_id
    on orchestration_steps (session_id);
