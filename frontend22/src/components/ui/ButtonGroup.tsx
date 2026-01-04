import type { ReactNode } from "react";
import { cn } from "../../lib/utils";

interface ButtonGroupProps {
  children: ReactNode;
  className?: string;
}

export function ButtonGroup({ children, className }: ButtonGroupProps) {
  return (
    <div
      className={cn(
        "bg-surface-header border-border-main flex items-center gap-1 rounded-lg border p-1",
        className,
      )}
    >
      {children}
    </div>
  );
}
