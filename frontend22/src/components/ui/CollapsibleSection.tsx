import { ChevronDown } from "lucide-react";
import type React from "react";
import { useState } from "react";

interface CollapsibleSectionProps {
  title: string;
  children: React.ReactNode;
  defaultOpen?: boolean;
  contentClassName?: string;
}

export const CollapsibleSection: React.FC<CollapsibleSectionProps> = ({
  title,
  children,
  defaultOpen = true,
  contentClassName = "",
}) => {
  const [isOpen, setIsOpen] = useState(defaultOpen);

  return (
    <div className="border-border-main border-b last:border-0">
      <button
        type="button"
        className="hover:bg-surface-header flex w-full items-center justify-between p-3 transition-colors"
        onClick={() => setIsOpen(!isOpen)}
      >
        <span className="text-text-muted text-[10px] font-bold tracking-wider uppercase">
          {title}
        </span>
        <ChevronDown
          className={`text-text-muted h-3 w-3 transition-transform ${
            isOpen ? "rotate-180" : ""
          }`}
        />
      </button>
      {isOpen && <div className={contentClassName}>{children}</div>}
    </div>
  );
};
