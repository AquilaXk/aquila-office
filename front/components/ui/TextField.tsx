import type { InputHTMLAttributes } from "react";
import { cn } from "./utils";

export type TextFieldProps = Omit<InputHTMLAttributes<HTMLInputElement>, "id"> & {
  id: string;
  label: string;
  hint?: string;
  error?: string;
};

export default function TextField({
  id,
  label,
  hint,
  error,
  className,
  ...props
}: TextFieldProps) {
  const descriptionId = error ? `${id}-error` : hint ? `${id}-hint` : undefined;

  return (
    <label className="ao-field" htmlFor={id}>
      <span className="ao-field-label">{label}</span>
      <input
        {...props}
        id={id}
        aria-describedby={descriptionId}
        aria-invalid={error ? "true" : undefined}
        className={cn("ao-input", className)}
      />
      {hint && !error ? (
        <span id={`${id}-hint`} className="ao-field-hint">
          {hint}
        </span>
      ) : null}
      {error ? (
        <span id={`${id}-error`} className="ao-field-error">
          {error}
        </span>
      ) : null}
    </label>
  );
}
