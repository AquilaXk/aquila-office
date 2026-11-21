import type { HTMLAttributes } from "react";
import { cn } from "./utils";

export type PanelTone = "default" | "accent" | "danger";

export type PanelProps = HTMLAttributes<HTMLElement> & {
  as?: "article" | "aside" | "section";
  tone?: PanelTone;
};

export default function Panel({
  as: Component = "section",
  tone = "default",
  className,
  ...props
}: PanelProps) {
  return <Component {...props} className={cn("ao-panel", `ao-panel-${tone}`, className)} />;
}
