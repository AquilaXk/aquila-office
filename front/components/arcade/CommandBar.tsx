import type { HTMLAttributes } from "react";
import { cn } from "../ui";

export type CommandBarProps = HTMLAttributes<HTMLDivElement> & {
  align?: "start" | "end" | "between";
};

export default function CommandBar({
  align = "end",
  className,
  ...props
}: CommandBarProps) {
  return (
    <div
      {...props}
      role="toolbar"
      className={cn("ao-command-bar", `ao-command-bar-${align}`, className)}
    />
  );
}
