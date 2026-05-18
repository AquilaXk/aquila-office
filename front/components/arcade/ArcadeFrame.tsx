import type { ReactNode } from "react";
import { Panel, cn } from "../ui";

export type ArcadeFrameProps = {
  title: string;
  eyebrow?: string;
  actions?: ReactNode;
  children: ReactNode;
  className?: string;
};

export default function ArcadeFrame({
  title,
  eyebrow,
  actions,
  children,
  className,
}: ArcadeFrameProps) {
  return (
    <Panel tone="accent" className={cn("ao-arcade-frame", className)}>
      <div className="ao-arcade-frame-header">
        <div>
          {eyebrow ? <p className="ao-arcade-eyebrow">{eyebrow}</p> : null}
          <h2>{title}</h2>
        </div>
        {actions ? <div className="ao-arcade-frame-actions">{actions}</div> : null}
      </div>
      <div className="ao-arcade-frame-body">{children}</div>
    </Panel>
  );
}
