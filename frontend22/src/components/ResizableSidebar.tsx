import type React from "react";
import { useEffect, useRef, useState } from "react";

interface ResizableSidebarProps {
  side: "left" | "right";
  isOpen: boolean;
  initialWidth?: number;
  minWidth?: number;
  maxWidth?: number;
  children: React.ReactNode;
}

const ResizableSidebar: React.FC<ResizableSidebarProps> = ({
  side,
  isOpen,
  initialWidth = 256,
  minWidth = 150,
  maxWidth = 600,
  children,
}) => {
  const [width, setWidth] = useState(initialWidth);
  const [isResizing, setIsResizing] = useState(false);
  const sidebarRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const handleMouseMove = (e: MouseEvent) => {
      if (!isResizing) return;

      let newWidth = width;
      if (side === "left") {
        newWidth = e.clientX;
      } else {
        newWidth = window.innerWidth - e.clientX;
      }

      if (newWidth >= minWidth && newWidth <= maxWidth) {
        setWidth(newWidth);
      }
    };

    const handleMouseUp = () => {
      setIsResizing(false);
      document.body.style.cursor = "default";
      document.body.style.userSelect = "auto";
    };

    if (isResizing) {
      document.addEventListener("mousemove", handleMouseMove);
      document.addEventListener("mouseup", handleMouseUp);
      document.body.style.cursor = "col-resize";
      document.body.style.userSelect = "none";
    }

    return () => {
      document.removeEventListener("mousemove", handleMouseMove);
      document.removeEventListener("mouseup", handleMouseUp);
    };
  }, [isResizing, width, side, minWidth, maxWidth]);

  const startResizing = (e: React.MouseEvent) => {
    e.preventDefault();
    setIsResizing(true);
  };

  return (
    <div
      ref={sidebarRef}
      className={`bg-sidebar relative flex h-full flex-col overflow-hidden transition-[width] ease-in-out ${
        isResizing ? "duration-0" : "duration-300"
      }`}
      style={{
        width: isOpen ? width : 0,

        borderRight:
          side === "left" && isOpen
            ? "1px solid var(--color-border-main)"
            : "none",

        borderLeft:
          side === "right" && isOpen
            ? "1px solid var(--color-border-main)"
            : "none",
      }}
    >
      <div className="bg-sidebar relative h-full flex-1 overflow-hidden">
        {/* Content Container */}

        <div style={{ width: width, height: "100%" }}>{children}</div>
      </div>

      {/* Resize Handle */}
      {isOpen && (
        // biome-ignore lint/a11y/useSemanticElements: Resize handle is an interactive element
        <div
          role="separator"
          aria-orientation="vertical"
          aria-valuenow={width}
          aria-valuemin={minWidth}
          aria-valuemax={maxWidth}
          tabIndex={0}
          className={`hover:bg-brand/50 absolute top-0 bottom-0 z-10 w-1 cursor-col-resize transition-colors ${
            side === "left" ? "right-0" : "left-0"
          } ${isResizing ? "bg-brand" : "bg-transparent"}`}
          onMouseDown={startResizing}
        />
      )}
    </div>
  );
};

export default ResizableSidebar;
