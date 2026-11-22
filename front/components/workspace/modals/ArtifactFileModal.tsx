"use client";

import { useRef } from "react";
import { AlertCircle, Download, Loader2, X } from "lucide-react";
import CodeBlockViewer from "@/components/ui/CodeBlockViewer";
import MarkdownContent from "@/components/ui/MarkdownContent";
import { useDialogA11y } from "@/components/ui/useDialogA11y";
import { ModalState } from "@/components/workspace/modals/TaskDetailModals";
import {
  formatFileSize,
  isMarkdownArtifactFile,
  type ModalLoadState,
} from "@/components/workspace/workspace-ui-helpers";
import type { ArtifactFileContent } from "@/lib/artifacts";
import { downloadTextArtifact } from "@/lib/code-highlight";

export type ArtifactFileModalProps = {
  path: string | null;
  nameHint: string | null;
  file: ArtifactFileContent | null;
  state: ModalLoadState;
  onClose: () => void;
};

export function ArtifactFileModal({
  path,
  nameHint,
  file,
  state,
  onClose,
}: ArtifactFileModalProps) {
  const dialogRef = useRef<HTMLElement | null>(null);
  const open = Boolean(path || state.status !== "idle");
  const { onBackdropPointerDown, onBackdropClick } = useDialogA11y(open, dialogRef, onClose);

  if (!open) {
    return null;
  }

  const isMarkdown = isMarkdownArtifactFile(path, file);

  return (
    <div
      className="workspace-modal-backdrop"
      role="presentation"
      onPointerDown={(event) => onBackdropPointerDown(event.target === event.currentTarget)}
      onClick={(event) => onBackdropClick(event.target === event.currentTarget)}
    >
      <section
        ref={dialogRef}
        className="workspace-modal workspace-artifact-file-modal"
        role="dialog"
        aria-modal="true"
        aria-label="산출물 파일 미리보기"
        tabIndex={-1}
      >
        <header className="workspace-modal-header">
          <div>
            <span className="workspace-detail-label">산출물 파일</span>
            <h2>{file?.name ?? nameHint ?? path ?? "파일"}</h2>
            {path ? <p className="m-0 text-[12px] text-[var(--ui-text-muted)]">{path}</p> : null}
          </div>
          <div className="flex items-center gap-1.5">
            {file ? (
              <button
                type="button"
                className="tb-btn"
                onClick={() => downloadTextArtifact(file.name || file.path, file.content)}
              >
                <Download size={12} aria-hidden="true" />
                <span>다운로드</span>
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
          {state.status === "loading" ? (
            <ModalState
              icon={<Loader2 className="spin" aria-hidden="true" />}
              text="파일을 불러오는 중입니다."
            />
          ) : null}
          {state.status === "error" ? (
            <ModalState icon={<AlertCircle aria-hidden="true" />} text={state.error} tone="danger" />
          ) : null}

          {state.status === "ready" && file ? (
            <>
              <div className="workspace-artifact-file-meta">
                <span>{file.contentType || "text/plain"}</span>
                {file.sizeBytes != null ? <span>{formatFileSize(file.sizeBytes)}</span> : null}
              </div>
              {isMarkdown ? (
                <div className="workspace-artifact-markdown">
                  <MarkdownContent content={file.content} />
                </div>
              ) : (
                <CodeBlockViewer
                  className="workspace-artifact-file-content"
                  code={file.content}
                  fileName={file.name || file.path}
                  contentType={file.contentType}
                />
              )}
            </>
          ) : null}
        </div>
      </section>
    </div>
  );
}
