import type { HTMLAttributes, ReactNode } from "react";
import { cn } from "./utils";

export type ChatBubbleRole = "user" | "assistant" | "system";

export type ChatBubbleProps = HTMLAttributes<HTMLDivElement> & {
  roleTone: ChatBubbleRole;
  author: string;
  children: ReactNode;
};

export default function ChatBubble({
  roleTone,
  author,
  children,
  className,
  ...props
}: ChatBubbleProps) {
  return (
    <div
      {...props}
      aria-label={`${author} chat message`}
      className={cn("ao-chat-bubble", `ao-chat-bubble-${roleTone}`, className)}
    >
      <strong>{author}</strong>
      <div>{children}</div>
    </div>
  );
}
