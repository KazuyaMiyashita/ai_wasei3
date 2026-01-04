import type React from "react";
import { useEffect, useRef, useState } from "react";

interface ResizableSidebarProps {
  side: "left" | "right" | "bottom";
  isOpen: boolean;
  initialWidth?: number;
  minWidth?: number;
  maxWidth?: number;
  initialHeight?: number;
  minHeight?: number;
  maxHeight?: number;
  children: React.ReactNode;
}

const ResizableSidebar: React.FC<ResizableSidebarProps> = ({
  side,
  isOpen,
  initialWidth,
  minWidth = 150,
  maxWidth = 600,
  initialHeight,
  minHeight = 150,
  maxHeight = 600,
  children,
}) => {
  const [width, setWidth] = useState(() => {
    if (initialWidth !== undefined) return initialWidth;
    const calculated =
      typeof window !== "undefined" ? window.innerWidth / 6 : 256;
    return Math.max(calculated, minWidth);
  });
  const [height, setHeight] = useState(() => {
    if (initialHeight !== undefined) return initialHeight;
    const calculated =
      typeof window !== "undefined" ? window.innerHeight / 4 : 200;
    return Math.max(calculated, minHeight);
  });
  const [isResizing, setIsResizing] = useState(false);
  const sidebarRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const handleMouseMove = (e: MouseEvent) => {
      if (!isResizing) return;

      if (side === "bottom") {
        const newHeight = window.innerHeight - e.clientY;
        if (newHeight >= minHeight && newHeight <= maxHeight) {
          setHeight(newHeight);
        }
      } else {
        let newWidth = 0;
        if (side === "left") {
          newWidth = e.clientX;
        } else {
          newWidth = window.innerWidth - e.clientX;
        }

        if (newWidth >= minWidth && newWidth <= maxWidth) {
          setWidth(newWidth);
        }
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
      document.body.style.cursor =
        side === "bottom" ? "row-resize" : "col-resize";
      document.body.style.userSelect = "none";
    }

    return () => {
      document.removeEventListener("mousemove", handleMouseMove);
      document.removeEventListener("mouseup", handleMouseUp);
    };
  }, [isResizing, side, minWidth, maxWidth, minHeight, maxHeight]);

  const startResizing = (e: React.MouseEvent) => {
    e.preventDefault();
    setIsResizing(true);
  };

  const isVertical = side === "left" || side === "right";

  return (
    <div
      ref={sidebarRef}
      className={`bg-sidebar relative flex overflow-hidden transition-[width,height] ease-in-out ${
        isResizing ? "duration-0" : "duration-300"
      } ${isVertical ? "h-full flex-col" : "w-full flex-col"}`}
      style={{
        width: isVertical ? (isOpen ? width : 0) : "100%",
        height: !isVertical ? (isOpen ? height : 0) : "100%",

        borderRight:
          side === "left" && isOpen
            ? "1px solid var(--color-border-main)"
            : "none",

        borderLeft:
          side === "right" && isOpen
            ? "1px solid var(--color-border-main)"
            : "none",

        borderTop:
          side === "bottom" && isOpen
            ? "1px solid var(--color-border-main)"
            : "none",
      }}
    >
      <div className="bg-sidebar relative h-full w-full flex-1 overflow-hidden">
        {/* Content Container */}
        <div style={{ width: isVertical ? width : "100%", height: "100%" }}>
          {children}
        </div>
      </div>

      {/* Resize Handle */}
      {isOpen && (
        // biome-ignore lint/a11y/useSemanticElements: This is an interactive resize handle
        <div
          role="separator"
          aria-orientation={isVertical ? "vertical" : "horizontal"}
          aria-valuenow={isVertical ? width : height}
          aria-valuemin={isVertical ? minWidth : minHeight}
          aria-valuemax={isVertical ? maxWidth : maxHeight}
          tabIndex={0}
          className={`hover:bg-brand/50 absolute z-10 transition-colors ${isResizing ? "bg-brand" : "bg-transparent"} ${
            side === "left"
              ? "top-0 right-0 bottom-0 w-1 cursor-col-resize"
              : side === "right"
                ? "top-0 bottom-0 left-0 w-1 cursor-col-resize"
                : "top-0 left-0 right-0 h-1 cursor-row-resize"
          }`}
          onMouseDown={startResizing}
        />
      )}
    </div>
  );
};

export default ResizableSidebar;
