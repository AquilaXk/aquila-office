import type { HTMLAttributes } from "react";
import { cn } from "../ui";

export type StatusPipStatus = "online" | "idle" | "warning" | "danger";

export type StatusPipProps = HTMLAttributes<HTMLSpanElement> & {
  status: StatusPipStatus;
  label: string;
};

export default function StatusPip({
  status,
  label,
  className,
  ...props
}: StatusPipProps) {
  return (
    <span {...props} className={cn("ao-status-pip", `ao-status-pip-${status}`, className)}>
      <span aria-hidden="true" className="ao-status-pip-dot" />
      {label}
    </span>
  );
}
