import { forwardRef, type ButtonHTMLAttributes, type ReactNode } from "react";
import { cn } from "./utils";

export type ButtonVariant = "primary" | "secondary" | "danger" | "ghost";
export type ButtonSize = "sm" | "md" | "lg";

export type ButtonProps = ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: ButtonVariant;
  size?: ButtonSize;
  icon?: ReactNode;
  loading?: boolean;
};

const iconSizeClass: Record<ButtonSize, string> = {
  sm: "ao-button-icon-sm",
  md: "ao-button-icon-md",
  lg: "ao-button-icon-lg",
};

const Button = forwardRef<HTMLButtonElement, ButtonProps>(
  (
    {
      variant = "primary",
      size = "md",
      className,
      type = "button",
      icon,
      loading = false,
      disabled,
      children,
      ...props
    },
    ref,
  ) => {
    const isDisabled = disabled || loading;

    return (
      <button
        {...props}
        ref={ref}
        type={type}
        disabled={isDisabled}
        aria-busy={loading ? "true" : undefined}
        className={cn(
          "ao-button",
          `ao-button-${variant}`,
          `ao-button-${size}`,
          isDisabled && "ao-button-disabled",
          className,
        )}
      >
        {loading ? (
          <span className={cn("ao-button-spinner", iconSizeClass[size])} aria-hidden="true" />
        ) : icon ? (
          <span className={cn("ao-button-icon", iconSizeClass[size])} aria-hidden="true">
            {icon}
          </span>
        ) : null}
        {children ? <span className="ao-button-label">{children}</span> : null}
      </button>
    );
  },
);

Button.displayName = "Button";

export default Button;
