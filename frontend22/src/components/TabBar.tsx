import type React from "react";
import { useEffect, useRef } from "react";
import type { Tab } from "../hooks/useScoreEditor";

interface TabBarProps {
  tabs: Tab[];
  activeTabPath: string | null;
  onTabSelect: (path: string) => void;
  onTabClose: (path: string) => void;
}

const TabBar: React.FC<TabBarProps> = ({
  tabs,
  activeTabPath,
  onTabSelect,
  onTabClose,
}) => {
  const scrollContainerRef = useRef<HTMLDivElement>(null);
  const activeTabRef = useRef<HTMLDivElement>(null);

  // Auto-scroll to active tab
  // biome-ignore lint/correctness/useExhaustiveDependencies: activeTabPath is the correct trigger
  useEffect(() => {
    if (activeTabRef.current && scrollContainerRef.current) {
      activeTabRef.current.scrollIntoView({
        behavior: "smooth",
        block: "nearest",
        inline: "nearest",
      });
    }
  }, [activeTabPath]);

  return (
    <div
      ref={scrollContainerRef}
      className="bg-background border-border-main no-scrollbar flex w-full shrink-0 overflow-x-auto border-b"
    >
      {tabs.map((tab) => {
        const isActive = tab.path === activeTabPath;
        const isEditing = tab.state === "editing";

        return (
          <div
            key={tab.path}
            ref={isActive ? activeTabRef : null}
            role="tab"
            aria-selected={isActive}
            tabIndex={0}
            className={`group border-border-main flex max-w-50 min-w-30 cursor-pointer items-center border-r py-2 pr-2 pl-3 outline-none select-none ${
              isActive
                ? "bg-background border-t-brand text-text-main -mt-px border-t-2 pt-2.25"
                : "bg-sidebar hover:bg-header text-text-muted"
            } `}
            onClick={() => onTabSelect(tab.path)}
            onKeyDown={(e) => {
              if (e.key === "Enter" || e.key === " ") {
                onTabSelect(tab.path);
              }
            }}
            title={tab.name}
          >
            <span
              className={`mr-2 flex-1 truncate text-xs ${
                tab.state === "semi-open" ? "italic" : ""
              } ${isActive ? "font-medium" : ""}`}
            >
              {tab.name}
            </span>

            {/* Icon Container (Close Button or Editing Dot) */}
            <div className="relative flex h-5 w-5 shrink-0 items-center justify-center">
              {/* Editing Dot */}
              {isEditing && (
                <div
                  className={`bg-brand h-2 w-2 rounded-full transition-opacity ${
                    isActive ? "group-hover:opacity-0" : "group-hover:opacity-0"
                  }`}
                />
              )}

              {/* Close Button */}
              <button
                type="button"
                className={`hover:bg-surface-muted absolute inset-0 flex items-center justify-center rounded-sm transition-opacity ${
                  isEditing
                    ? "opacity-0 group-hover:opacity-100"
                    : isActive
                      ? "opacity-100"
                      : "opacity-0 group-hover:opacity-100"
                } `}
                onClick={(e) => {
                  e.stopPropagation();
                  onTabClose(tab.path);
                }}
                aria-label={`Close ${tab.name}`}
              >
                <svg
                  className={`h-3 w-3 ${isActive ? "text-text-main" : "text-text-muted hover:text-text-main"}`}
                  fill="none"
                  stroke="currentColor"
                  viewBox="0 0 24 24"
                  aria-hidden="true"
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth="2"
                    d="M6 18L18 6M6 6l12 12"
                  />
                </svg>
              </button>
            </div>
          </div>
        );
      })}
    </div>
  );
};

export default TabBar;
