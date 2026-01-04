import type React from "react";

interface SidebarPanelProps {
  title: string;
  headerActions?: React.ReactNode;
  children: React.ReactNode;
  className?: string;
}

export const SidebarPanel: React.FC<SidebarPanelProps> = ({
  title,
  headerActions,
  children,
  className = "",
}) => {
  return (
    <div
      className={`bg-sidebar flex h-full w-full flex-col overflow-hidden ${className}`}
    >
      <div className="flex shrink-0 items-center justify-between p-3 pb-2">
        <span className="text-text-sub text-[10px] font-bold tracking-wider uppercase">
          {title}
        </span>
        {headerActions && <div>{headerActions}</div>}
      </div>
      <div className="flex-1 overflow-y-auto">{children}</div>
    </div>
  );
};
