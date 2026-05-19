import { forwardRef, type InputHTMLAttributes, type ReactNode, type TextareaHTMLAttributes } from "react";
import { cn } from "./utils";

export type InputProps = InputHTMLAttributes<HTMLInputElement> & {
  icon?: ReactNode;
  error?: string;
};

export const Input = forwardRef<HTMLInputElement, InputProps>(function Input(
  { icon, error, className, ...props },
  ref,
) {
  const control = (
    <input
      {...props}
      ref={ref}
      aria-invalid={error ? "true" : undefined}
      className={cn("ao-input", icon ? "ao-input-with-icon" : undefined, className)}
    />
  );

  if (!icon && !error) {
    return control;
  }

  return (
    <label className="ao-input-frame">
      {icon ? (
        <span className="ao-input-icon" aria-hidden="true">
          {icon}
        </span>
      ) : null}
      {control}
      {error ? <span className="ao-input-error">{error}</span> : null}
    </label>
  );
});

export type TextareaProps = TextareaHTMLAttributes<HTMLTextAreaElement> & {
  error?: string;
};

export const Textarea = forwardRef<HTMLTextAreaElement, TextareaProps>(function Textarea(
  { error, className, ...props },
  ref,
) {
  return (
    <label className="ao-input-frame">
      <textarea
        {...props}
        ref={ref}
        aria-invalid={error ? "true" : undefined}
        className={cn("ao-input ao-textarea", className)}
      />
      {error ? <span className="ao-input-error">{error}</span> : null}
    </label>
  );
});

export default Input;
