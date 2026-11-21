import type { HTMLAttributes } from "react";
import { cn } from "./utils";

export type BadgeTone = "neutral" | "success" | "warning" | "danger";

export type BadgeProps = HTMLAttributes<HTMLSpanElement> & {
  tone?: BadgeTone;
};

export default function Badge({ tone = "neutral", className, ...props }: BadgeProps) {
  return <span {...props} className={cn("ao-badge", `ao-badge-${tone}`, className)} />;
}
