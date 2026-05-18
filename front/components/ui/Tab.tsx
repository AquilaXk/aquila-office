import type { ReactNode } from "react";
import { cn } from "./utils";

export type TabItem = {
  key: string;
  label: ReactNode;
  badge?: number;
};

export type TabProps = {
  tabs: TabItem[];
  activeKey: string;
  onChange: (key: string) => void;
};

export default function Tab({ tabs, activeKey, onChange }: TabProps) {
  return (
    <div className="ao-tabs" role="tablist">
      {tabs.map((tab) => (
        <button
          type="button"
          role="tab"
          key={tab.key}
          aria-selected={tab.key === activeKey}
          className={cn("ao-tab", tab.key === activeKey && "ao-tab-active")}
          onClick={() => onChange(tab.key)}
        >
          <span>{tab.label}</span>
          {tab.badge ? <small>{tab.badge}</small> : null}
        </button>
      ))}
    </div>
  );
}
