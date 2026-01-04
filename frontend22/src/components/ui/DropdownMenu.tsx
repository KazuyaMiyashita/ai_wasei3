import type React from "react";
import { useCallback, useEffect, useRef, useState } from "react";

interface DropdownMenuProps {
  trigger: React.ReactNode;
  children: React.ReactNode;
  isOpen?: boolean;
  onOpenChange?: (open: boolean) => void;
  className?: string;
  align?: "left" | "right";
}

export const DropdownMenu: React.FC<DropdownMenuProps> = ({
  trigger,
  children,
  isOpen: controlledIsOpen,
  onOpenChange,
  className = "",
  align = "left",
}) => {
  const [uncontrolledIsOpen, setUncontrolledIsOpen] = useState(false);
  const menuRef = useRef<HTMLDivElement>(null);

  const isControlled = controlledIsOpen !== undefined;
  const isOpen = isControlled ? controlledIsOpen : uncontrolledIsOpen;

  const setIsOpen = useCallback(
    (open: boolean) => {
      if (!isControlled) {
        setUncontrolledIsOpen(open);
      }
      onOpenChange?.(open);
    },
    [isControlled, onOpenChange],
  );

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (menuRef.current && !menuRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    };
    if (isOpen) {
      document.addEventListener("mousedown", handleClickOutside);
    }
    return () => {
      document.removeEventListener("mousedown", handleClickOutside);
    };
  }, [isOpen, setIsOpen]);

  return (
    <div className={`relative ${className}`} ref={menuRef}>
      {/* biome-ignore lint/a11y/useKeyWithClickEvents: Trigger is usually an interactive element */}
      {/* biome-ignore lint/a11y/noStaticElementInteractions: Trigger wrapper */}
      <div onClick={() => setIsOpen(!isOpen)}>{trigger}</div>
      {isOpen && (
        <div
          className={`bg-surface border-border-main absolute top-full z-50 mt-1 min-w-[200px] rounded-md border py-1 shadow-xl ${
            align === "right" ? "right-0" : "left-0"
          }`}
        >
          {children}
        </div>
      )}
    </div>
  );
};
