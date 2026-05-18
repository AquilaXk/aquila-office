import type { InputHTMLAttributes } from "react";
import { cn } from "./utils";

export type InputProps = InputHTMLAttributes<HTMLInputElement> & {
  error?: string;
};

export default function Input({ error, className, ...props }: InputProps) {
  return (
    <input
      {...props}
      aria-invalid={error ? "true" : undefined}
      className={cn("ao-input", className)}
    />
  );
}
