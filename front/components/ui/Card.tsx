import type { HTMLAttributes } from "react";
import { cn } from "./utils";

export type CardProps = HTMLAttributes<HTMLElement> & {
  as?: "article" | "section" | "div";
  selected?: boolean;
};

export default function Card({
  as: Component = "article",
  selected = false,
  className,
  ...props
}: CardProps) {
  return (
    <Component
      {...props}
      data-selected={selected ? "true" : undefined}
      className={cn("ao-card", selected && "ao-card-selected", className)}
    />
  );
}
