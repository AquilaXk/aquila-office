"use client";

import { findCatalogModel, formatTokenCountShort, type ModelCatalogPreset, type ModelProviderKind } from "@/lib/model-catalog";

export function ModelCapabilityBadges({
  modelId,
  presets,
  preferredKind,
}: {
  modelId: string | null | undefined;
  presets?: readonly ModelCatalogPreset[];
  preferredKind?: ModelProviderKind | null;
}) {
  const entry = findCatalogModel(modelId, presets, preferredKind);
  if (!entry) {
    return null;
  }

  return (
    <div className="ao-capability-row" aria-label="모델 지원 기능">
      <span className="ao-capability-badge ao-capability-badge-accent">
        컨텍스트 {formatTokenCountShort(entry.contextWindow)}
      </span>
      <span className="ao-capability-badge">
        최대 출력 {formatTokenCountShort(entry.maxOutputTokens)}
      </span>
      {entry.supportsReasoning ? <span className="ao-capability-badge">추론</span> : null}
      {entry.supportsTools ? <span className="ao-capability-badge">도구 호출</span> : null}
      {entry.supportsVision ? <span className="ao-capability-badge">비전</span> : null}
    </div>
  );
}
