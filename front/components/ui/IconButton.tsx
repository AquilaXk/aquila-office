import type { ButtonHTMLAttributes, ReactNode } from "react";
import { cn } from "./utils";

export type IconButtonProps = Omit<ButtonHTMLAttributes<HTMLButtonElement>, "aria-label"> & {
  icon: ReactNode;
  label: string;
  tooltip?: string;
};

export default function IconButton({
  icon,
  label,
  tooltip,
  className,
  type = "button",
  ...props
}: IconButtonProps) {
  return (
    <button
      {...props}
      type={type}
      aria-label={label}
      title={tooltip ?? label}
      className={cn("ao-icon-button", className)}
    >
      {icon}
    </button>
  );
}
