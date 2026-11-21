create index if not exists idx_workspaces_id_deleted_at
    on workspaces (id, deleted_at);

create index if not exists idx_workspace_members_workspace_role_joined
    on workspace_members (workspace_id, role, joined_at);

create index if not exists idx_workspace_invites_workspace_status_id
    on workspace_invites (workspace_id, status, id desc);

create index if not exists idx_workspace_agents_workspace_status_id
    on workspace_agents (workspace_id, status, id desc);

create index if not exists idx_workspace_agent_skill_files_agent_id_id
    on workspace_agent_skill_files (agent_id, id);

create index if not exists idx_tasks_workspace_assigned_status_id
    on tasks (workspace_id, assigned_agent_id, status, id desc);

create index if not exists idx_task_executions_workspace_task_created_at
    on task_executions (workspace_id, task_id, created_at desc);

alter table workspaces
    add column if not exists daily_token_limit integer;

alter table task_executions
    add column if not exists duration_ms bigint;

alter table task_executions
    add column if not exists prompt_tokens integer not null default 0;

alter table task_executions
    add column if not exists completion_tokens integer not null default 0;

alter table task_executions
    add column if not exists model_name varchar(120);
