"use client";

import { useEffect, useMemo, useState } from "react";
import {
  AlertCircle,
  Eye,
  FileCode2,
  FileText,
  Folder,
  FolderOpen,
  GitCompare,
  Loader2,
  RefreshCw,
  Search,
} from "lucide-react";
import MarkdownContent from "@/components/ui/MarkdownContent";
import WorkspaceGitDiffViewer from "@/components/workspace/WorkspaceGitDiffViewer";
import {
  defaultExpandedArtifactPaths,
  formatFileSize,
  isMarkdownArtifactFile,
  MAX_LOCAL_DIFF_CHARS,
  MAX_WATCH_EVENTS,
  messageFromError,
  toggleExpandedArtifactPath,
  type ModalLoadState,
} from "@/components/workspace/workspace-ui-helpers";
import type { WorkspaceAgent } from "@/lib/agents";
import {
  flattenArtifactNodes,
  type ArtifactFileContent,
  type ArtifactNode,
  type ArtifactTree,
} from "@/lib/artifacts";
import { searchBitapItems } from "@/lib/bitap-fuzzy-search";
import { isDesktop } from "@/lib/desktop/env";
import {
  getWorkspaceGitDiff,
  getWorkspaceGitStatus,
  listenDesktop,
  startWatchingWorkspace,
  stopWatchingWorkspace,
  type GitStatusResult,
  type WorkspaceFileChangeEvent,
} from "@/lib/desktop/ipc";

export type WorkspaceArtifactsAndDiffViewProps = {
  tree: ArtifactTree | null;
  treeState: ModalLoadState;
  selectedPath: string | null;
  selectedFile: ArtifactFileContent | null;
  fileState: ModalLoadState;
  agents: WorkspaceAgent[];
  onReloadTree: () => void;
  onSelectFile: (path: string) => void;
};

type SubTab = "artifacts" | "git";

export function WorkspaceArtifactsAndDiffView({
  tree,
  treeState,
  selectedPath,
  selectedFile,
  fileState,
  agents,
  onReloadTree,
  onSelectFile,
}: WorkspaceArtifactsAndDiffViewProps) {
  const [subTab, setSubTab] = useState<SubTab>("artifacts");
  const [fileQuery, setFileQuery] = useState("");
  const [expandedOverride, setExpandedOverride] = useState<{
    tree: ArtifactTree | null;
    paths: Set<string>;
  } | null>(null);

  const defaultRepoPath = useMemo(() => {
    const withPath = agents.find((agent) => agent.workspacePath?.trim());
    return withPath?.workspacePath?.trim() ?? "";
  }, [agents]);

  const [repoPathInput, setRepoPathInput] = useState(defaultRepoPath);
  const [gitStatus, setGitStatus] = useState<GitStatusResult | null>(null);
  const [gitLoading, setGitLoading] = useState(false);
  const [gitError, setGitError] = useState<string | null>(null);
  const [selectedGitFile, setSelectedGitFile] = useState<string | null>(null);
  const [diffText, setDiffText] = useState("");
  const [diffLoading, setDiffLoading] = useState(false);
  const [watchingPath, setWatchingPath] = useState<string | null>(null);
  const [watchEvents, setWatchEvents] = useState<string[]>([]);
  const [manualDiffInput, setManualDiffInput] = useState("");

  const desktopRuntime = isDesktop();

  const expandedPaths = useMemo(() => {
    if (expandedOverride && expandedOverride.tree === tree) {
      return expandedOverride.paths;
    }
    return tree ? defaultExpandedArtifactPaths(tree) : new Set<string>();
  }, [expandedOverride, tree]);

  const matchedFiles = useMemo(() => {
    if (!tree || !fileQuery.trim()) {
      return null;
    }
    const flatFiles = flattenArtifactNodes(tree.children)
      .map((entry) => entry.node)
      .filter((node) => node.type === "FILE");
    return searchBitapItems(flatFiles, fileQuery.trim(), (node) => `${node.name} ${node.path}`, {
      maxDistance: 1,
    }).map((hit) => hit.item);
  }, [fileQuery, tree]);

  useEffect(() => {
    if (!watchingPath || !desktopRuntime) {
      return;
    }
    let active = true;
    let unlistenFn: (() => void) | null = null;

    void listenDesktop<WorkspaceFileChangeEvent>("workspace:file-changed", (payload) => {
      if (!active || !payload?.paths?.length) {
        return;
      }
      setWatchEvents((current) => [...payload.paths, ...current].slice(0, MAX_WATCH_EVENTS));
    }).then((unlisten) => {
      if (active) {
        unlistenFn = unlisten;
      } else {
        unlisten();
      }
    });

    return () => {
      active = false;
      unlistenFn?.();
    };
  }, [desktopRuntime, watchingPath]);

  const effectiveRepoPath = (repoPathInput || defaultRepoPath).trim();

  async function loadGitStatusAndDiff(filePath?: string) {
    if (!effectiveRepoPath) {
      setGitError("로컬 저장소 경로를 입력해 주세요.");
      return;
    }
    if (effectiveRepoPath.length > 512) {
      setGitError("저장소 경로 길이가 허용 상한(512자)을 초과했습니다.");
      return;
    }

    setGitLoading(true);
    setGitError(null);
    try {
      const nextStatus = await getWorkspaceGitStatus(effectiveRepoPath);
      setGitStatus(nextStatus);
      const targetFile = filePath ?? nextStatus.files[0]?.path;
      setSelectedGitFile(targetFile ?? null);
      if (targetFile) {
        setDiffLoading(true);
        const rawDiff = await getWorkspaceGitDiff(effectiveRepoPath, targetFile);
        if (rawDiff.length > MAX_LOCAL_DIFF_CHARS) {
          setDiffText("");
          setGitError(`Diff 크기가 상한(${MAX_LOCAL_DIFF_CHARS.toLocaleString("ko-KR")}자)을 초과해 차단되었습니다.`);
        } else {
          setDiffText(rawDiff);
        }
      } else {
        setDiffText("");
      }
    } catch (error) {
      setGitError(messageFromError(error, "로컬 Git 상태를 조회하지 못했습니다."));
    } finally {
      setGitLoading(false);
      setDiffLoading(false);
    }
  }

  async function selectGitDiffFile(filePath: string) {
    if (!effectiveRepoPath) {
      return;
    }
    setSelectedGitFile(filePath);
    setDiffLoading(true);
    setGitError(null);
    try {
      const rawDiff = await getWorkspaceGitDiff(effectiveRepoPath, filePath);
      if (rawDiff.length > MAX_LOCAL_DIFF_CHARS) {
        setDiffText("");
        setGitError(`Diff 크기가 상한(${MAX_LOCAL_DIFF_CHARS.toLocaleString("ko-KR")}자)을 초과했습니다.`);
      } else {
        setDiffText(rawDiff);
      }
    } catch (error) {
      setGitError(messageFromError(error, "파일 Diff를 불러오지 못했습니다."));
    } finally {
      setDiffLoading(false);
    }
  }

  async function toggleWatcher() {
    if (!effectiveRepoPath) {
      setGitError("감시할 로컬 저장소 경로를 입력해 주세요.");
      return;
    }
    setGitError(null);
    try {
      if (watchingPath) {
        await stopWatchingWorkspace(watchingPath);
        setWatchingPath(null);
      } else {
        await startWatchingWorkspace(effectiveRepoPath);
        setWatchingPath(effectiveRepoPath);
      }
    } catch (error) {
      setGitError(messageFromError(error, "파일 변경 감시 전환에 실패했습니다."));
    }
  }

  function renderTreeNodes(nodes: ArtifactNode[], depth = 0) {
    return (
      <ul className="workspace-artifact-tree-list">
        {nodes.map((node) => {
          const isDirectory = node.type === "DIRECTORY";
          const expanded = isDirectory && expandedPaths.has(node.path);
          const active = !isDirectory && selectedPath === node.path;

          return (
            <li key={node.path}>
              <button
                type="button"
                aria-expanded={isDirectory ? expanded : undefined}
                className={`workspace-artifact-node${active ? " workspace-artifact-node-active" : ""}`}
                style={{ paddingLeft: `${12 + depth * 14}px` }}
                onClick={() => {
                  if (isDirectory) {
                    setExpandedOverride({
                      tree,
                      paths: toggleExpandedArtifactPath(expandedPaths, node.path),
                    });
                  } else {
                    onSelectFile(node.path);
                  }
                }}
              >
                {isDirectory ? (
                  expanded ? (
                    <FolderOpen size={14} aria-hidden="true" />
                  ) : (
                    <Folder size={14} aria-hidden="true" />
                  )
                ) : (
                  <FileText size={14} aria-hidden="true" />
                )}
                <span>{node.name}</span>
                {node.sizeBytes != null ? <small>{formatFileSize(node.sizeBytes)}</small> : null}
              </button>
              {isDirectory && expanded && node.children && node.children.length > 0
                ? renderTreeNodes(node.children, depth + 1)
                : null}
            </li>
          );
        })}
      </ul>
    );
  }

  const isMarkdown = isMarkdownArtifactFile(selectedPath, selectedFile);

  return (
    <div className="chrome-view-scroll" aria-label="산출물 및 변경 뷰">
      <div className="ao-page-header">
        <div>
          <p className="ao-page-eyebrow">파일 및 변경사항</p>
          <h2 className="ao-page-title">산출물 및 로컬 Git 변경사항</h2>
          <p className="ao-page-subtitle">
            에이전트가 생성한 결과 파일 트리와 데스크톱 로컬 저장소 변경 내역을 확인합니다.
          </p>
        </div>

        <div className="flex items-center gap-2">
          <button
            type="button"
            className={`tb-btn${subTab === "artifacts" ? " tb-btn-primary" : ""}`}
            onClick={() => setSubTab("artifacts")}
          >
            <FileCode2 size={14} aria-hidden="true" />
            <span>산출물 파일 트리</span>
          </button>
          <button
            type="button"
            className={`tb-btn${subTab === "git" ? " tb-btn-primary" : ""}`}
            onClick={() => setSubTab("git")}
          >
            <GitCompare size={14} aria-hidden="true" />
            <span>로컬 Git 변경사항</span>
          </button>
        </div>
      </div>

      {subTab === "artifacts" ? (
        <div className="chrome-split-pane">
          <section className="ao-surface-panel flex flex-col gap-3" aria-label="Workspace files">
            <div className="ao-surface-panel-header">
              <h3>작업 산출물 파일</h3>
              <button
                type="button"
                className="tb-btn"
                onClick={onReloadTree}
                disabled={treeState.status === "loading"}
              >
                <RefreshCw
                  size={13}
                  className={treeState.status === "loading" ? "spin" : undefined}
                  aria-hidden="true"
                />
                <span>새로고침</span>
              </button>
            </div>

            <label className="relative">
              <Search
                size={14}
                aria-hidden="true"
                className="absolute left-2.5 top-1/2 -translate-y-1/2 text-[var(--ui-text-muted)]"
              />
              <input
                type="search"
                aria-label="파일 검색"
                placeholder="파일명 또는 경로 검색"
                value={fileQuery}
                onChange={(event) => setFileQuery(event.target.value)}
                className="w-full h-8 pl-8 pr-3 rounded-[var(--radius-md)] border border-[var(--ui-border)] bg-[var(--ui-bg)] text-[13px] text-[var(--ui-text)]"
              />
            </label>

            {treeState.status === "loading" ? (
              <p className="m-0 text-[13px] text-[var(--ui-text-secondary)]">
                산출물 목록을 불러오는 중입니다.
              </p>
            ) : null}
            {treeState.status === "error" ? (
              <div className="ao-inline-notice ao-inline-notice-danger" role="alert">
                <AlertCircle size={14} aria-hidden="true" />
                <span>{treeState.error}</span>
              </div>
            ) : null}

            {matchedFiles ? (
              matchedFiles.length > 0 ? (
                <ul className="workspace-artifact-tree-list">
                  {matchedFiles.map((node) => (
                    <li key={node.path}>
                      <button
                        type="button"
                        className={`workspace-artifact-node${
                          selectedPath === node.path ? " workspace-artifact-node-active" : ""
                        }`}
                        onClick={() => onSelectFile(node.path)}
                      >
                        <FileText size={14} aria-hidden="true" />
                        <span>{node.path}</span>
                      </button>
                    </li>
                  ))}
                </ul>
              ) : (
                <p className="m-0 text-[13px] text-[var(--ui-text-muted)]">검색된 파일이 없습니다.</p>
              )
            ) : tree && tree.children.length > 0 ? (
              <div className="overflow-auto max-h-[520px]">{renderTreeNodes(tree.children)}</div>
            ) : treeState.status === "ready" ? (
              <p className="m-0 text-[13px] text-[var(--ui-text-muted)]">
                생성된 산출물 파일이 없습니다.
              </p>
            ) : null}
          </section>

          <section className="ao-surface-panel flex flex-col gap-3 min-w-0" aria-label="산출물 미리보기">
            <div className="ao-surface-panel-header">
              <div>
                <h3>{selectedFile?.name ?? selectedPath ?? "파일 미리보기"}</h3>
                {selectedFile ? (
                  <p className="m-0 text-[12px] text-[var(--ui-text-muted)]">
                    {selectedFile.path}
                    {selectedFile.sizeBytes != null ? ` · ${formatFileSize(selectedFile.sizeBytes)}` : ""}
                  </p>
                ) : null}
              </div>
            </div>

            {!selectedPath ? (
              <p className="m-0 py-8 text-center text-[13px] text-[var(--ui-text-muted)]">
                왼쪽 목록에서 파일을 선택하면 내용이 표시됩니다.
              </p>
            ) : null}
            {fileState.status === "loading" ? (
              <div className="inline-flex items-center gap-2 text-[13px] text-[var(--ui-text-secondary)]">
                <Loader2 size={14} className="spin" aria-hidden="true" />
                <span>파일 내용을 불러오는 중입니다.</span>
              </div>
            ) : null}
            {fileState.status === "error" ? (
              <div className="ao-inline-notice ao-inline-notice-danger" role="alert">
                <AlertCircle size={14} aria-hidden="true" />
                <span>{fileState.error}</span>
              </div>
            ) : null}
            {fileState.status === "ready" && selectedFile ? (
              <>
                {isMarkdown ? (
                  <div className="workspace-artifact-markdown overflow-auto max-h-[520px]">
                    <MarkdownContent content={selectedFile.content} />
                  </div>
                ) : (
                  <pre className="workspace-artifact-file-content overflow-auto max-h-[520px]">
                    {selectedFile.content}
                  </pre>
                )}
              </>
            ) : null}
          </section>
        </div>
      ) : (
        <div className="flex flex-col gap-3">
          {desktopRuntime ? (
            <>
              <div className="ao-surface-panel flex flex-wrap items-end gap-3">
                <label className="form-field flex-1 min-w-[260px]">
                  <span>
                    <strong>로컬 저장소 경로</strong>
                    <small>Tauri IPC</small>
                  </span>
                  <input
                    maxLength={512}
                    placeholder="/path/to/workspace/repo"
                    value={repoPathInput || defaultRepoPath}
                    onChange={(event) => setRepoPathInput(event.target.value)}
                  />
                </label>

                <button
                  type="button"
                  className="primary-action primary-action-active"
                  disabled={gitLoading}
                  onClick={() => void loadGitStatusAndDiff()}
                >
                  {gitLoading ? (
                    <Loader2 size={14} className="spin" aria-hidden="true" />
                  ) : (
                    <GitCompare size={14} aria-hidden="true" />
                  )}
                  <span>Git 상태 조회</span>
                </button>

                <button
                  type="button"
                  className="secondary-action"
                  onClick={() => void toggleWatcher()}
                >
                  <Eye size={14} aria-hidden="true" />
                  <span>{watchingPath ? "변경 감시 중지" : "변경 감시 시작"}</span>
                </button>
              </div>

              {gitError ? (
                <div className="ao-inline-notice ao-inline-notice-danger" role="alert">
                  <AlertCircle size={14} aria-hidden="true" />
                  <span>{gitError}</span>
                </div>
              ) : null}

              {watchEvents.length > 0 ? (
                <div className="ao-surface-panel">
                  <strong className="text-[12px] text-[var(--ui-text-strong)]">
                    최근 파일 변경 감지 ({watchEvents.length})
                  </strong>
                  <div className="flex flex-wrap gap-1.5 mt-2">
                    {watchEvents.map((path, index) => (
                      <span key={`${path}-${index}`} className="role-badge font-mono">
                        {path}
                      </span>
                    ))}
                  </div>
                </div>
              ) : null}

              <div className="chrome-split-pane">
                <section className="ao-surface-panel" aria-label="변경된 파일 목록">
                  <div className="ao-surface-panel-header">
                    <h3>변경 파일 ({gitStatus?.files.length ?? 0})</h3>
                    {gitStatus ? <span className="role-badge">{gitStatus.branch}</span> : null}
                  </div>
                  {!gitStatus ? (
                    <p className="m-0 text-[13px] text-[var(--ui-text-muted)]">
                      경로를 입력하고 Git 상태 조회를 누르세요.
                    </p>
                  ) : gitStatus.files.length === 0 ? (
                    <p className="m-0 text-[13px] text-[var(--ui-text-secondary)]">
                      작업 트리가 깨끗합니다.
                    </p>
                  ) : (
                    <div className="flex flex-col gap-1">
                      {gitStatus.files.map((file) => (
                        <button
                          key={file.path}
                          type="button"
                          onClick={() => void selectGitDiffFile(file.path)}
                          className={`chrome-sidebar-item${
                            selectedGitFile === file.path ? " chrome-sidebar-item-active" : ""
                          }`}
                        >
                          <span className="truncate font-mono">{file.path}</span>
                          <span className="role-badge">{file.status}</span>
                        </button>
                      ))}
                    </div>
                  )}
                </section>

                <section className="min-w-0">
                  {diffLoading ? (
                    <div className="ao-surface-panel">
                      <span>Diff를 불러오는 중입니다.</span>
                    </div>
                  ) : (
                    <WorkspaceGitDiffViewer
                      diff={diffText}
                      filePath={selectedGitFile ?? "Git Diff"}
                    />
                  )}
                </section>
              </div>
            </>
          ) : (
            <div className="flex flex-col gap-3">
              <div className="ao-inline-notice">
                <AlertCircle size={15} aria-hidden="true" />
                <span>
                  로컬 파일 시스템 감시 및 Git 저장소 직접 조회는 Aquila Office 데스크톱 앱에서
                  제공됩니다. 웹 환경에서는 아래에 Unified Diff 텍스트를 붙여넣어 미리볼 수 있습니다.
                </span>
              </div>
              <label className="ao-surface-panel form-field">
                <span>
                  <strong>Unified Diff 미리보기 입력</strong>
                  <small>
                    {manualDiffInput.length}/{MAX_LOCAL_DIFF_CHARS.toLocaleString("ko-KR")}
                  </small>
                </span>
                <textarea
                  rows={5}
                  maxLength={MAX_LOCAL_DIFF_CHARS}
                  placeholder="--- a/file.ts&#10;+++ b/file.ts&#10;@@ -1,2 +1,2 @@"
                  value={manualDiffInput}
                  onChange={(event) => setManualDiffInput(event.target.value)}
                />
              </label>
              <WorkspaceGitDiffViewer
                diff={manualDiffInput.slice(0, MAX_LOCAL_DIFF_CHARS)}
                filePath="Unified Diff 미리보기"
              />
            </div>
          )}
        </div>
      )}
    </div>
  );
}
