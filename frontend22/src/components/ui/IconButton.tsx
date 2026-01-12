import type { ButtonHTMLAttributes, ReactNode } from "react";
import { cn } from "../../utils";

interface IconButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  icon: ReactNode;
  isActive?: boolean;
  variant?: "ghost" | "toggle";
}

export function IconButton({
  className,
  icon,
  isActive,
  variant = "ghost",
  ...props
}: IconButtonProps) {
  return (
    <button
      type="button"
      className={cn(
        "flex h-8 w-8 items-center justify-center rounded transition-all active:scale-95",
        "cursor-pointer",
        variant === "toggle" && isActive
          ? "bg-surface text-brand shadow-sm"
          : "text-text-sub hover:bg-surface-hover hover:text-text-main",
        variant === "ghost" && isActive ? "text-brand bg-surface-muted" : "",
        className,
      )}
      {...props}
    >
      {icon}
    </button>
  );
}
