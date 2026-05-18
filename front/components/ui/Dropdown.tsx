"use client";

import type { ReactNode } from "react";
import { cn } from "./utils";

export type DropdownItem = {
  key: string;
  label: string;
  disabled?: boolean;
};

export type DropdownProps = {
  label: ReactNode;
  items: DropdownItem[];
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onSelect: (key: string) => void;
};

export default function Dropdown({
  label,
  items,
  open,
  onOpenChange,
  onSelect,
}: DropdownProps) {
  return (
    <div className="ao-dropdown">
      <button
        type="button"
        aria-expanded={open}
        className="ao-dropdown-trigger"
        onClick={() => onOpenChange(!open)}
      >
        {label}
      </button>
      {open ? (
        <div className="ao-dropdown-menu" role="menu">
          {items.map((item) => (
            <button
              type="button"
              role="menuitem"
              key={item.key}
              disabled={item.disabled}
              className={cn("ao-dropdown-item", item.disabled && "ao-dropdown-item-disabled")}
              onClick={() => {
                if (!item.disabled) {
                  onSelect(item.key);
                  onOpenChange(false);
                }
              }}
            >
              {item.label}
            </button>
          ))}
        </div>
      ) : null}
    </div>
  );
}
