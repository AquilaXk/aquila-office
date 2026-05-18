import type { HTMLAttributes, ReactNode } from "react";
import { cn } from "../ui";

export type MetricTileTone = "neutral" | "success" | "warning" | "danger";

export type MetricTileProps = HTMLAttributes<HTMLDivElement> & {
  label: string;
  value: ReactNode;
  detail?: ReactNode;
  tone?: MetricTileTone;
};

export default function MetricTile({
  label,
  value,
  detail,
  tone = "neutral",
  className,
  ...props
}: MetricTileProps) {
  return (
    <div {...props} className={cn("ao-metric-tile", `ao-metric-tile-${tone}`, className)}>
      <span className="ao-metric-tile-label">{label}</span>
      <strong>{value}</strong>
      {detail ? <span className="ao-metric-tile-detail">{detail}</span> : null}
    </div>
  );
}
