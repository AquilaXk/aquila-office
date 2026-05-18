import type { ReactNode } from "react";
import { cn } from "./utils";

export type ToastTone = "info" | "success" | "warning" | "danger";

export type ToastProps = {
  children: ReactNode;
  tone?: ToastTone;
  visible?: boolean;
};

export default function Toast({ children, tone = "info", visible = true }: ToastProps) {
  return (
    <div className={cn("ao-toast", `ao-toast-${tone}`, !visible && "ao-toast-hidden")} role="status">
      {children}
    </div>
  );
}
