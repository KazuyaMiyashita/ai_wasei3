import type React from "react";
import ResizableSidebar from "./ResizableSidebar";

interface LayoutProps {
  header: React.ReactNode;
  activityBar: React.ReactNode;
  leftSidebar: React.ReactNode;
  rightSidebar: React.ReactNode;
  tabBar: React.ReactNode;
  mainContent: React.ReactNode;
  statusBar: React.ReactNode;
  contextMenu: React.ReactNode;
  isLeftSidebarOpen: boolean;
  isRightSidebarOpen: boolean;
  inspectorPosition: "right" | "bottom";
}

export const Layout: React.FC<LayoutProps> = ({
  header,
  activityBar,
  leftSidebar,
  rightSidebar,
  tabBar,
  mainContent,
  statusBar,
  contextMenu,
  isLeftSidebarOpen,
  isRightSidebarOpen,
  inspectorPosition,
}) => {
  return (
    <div className="bg-background text-text-main flex h-screen w-screen flex-col overflow-hidden font-sans">
      {/* 1. Header */}
      {header}

      <div className="flex flex-1 overflow-hidden">
        {/* 2. Activity Bar */}
        <div className="border-border-main bg-surface w-12 shrink-0 flex-col items-center border-r py-2">
          {activityBar}
        </div>

        {/* 3. Left Sidebar */}
        <ResizableSidebar side="left" isOpen={isLeftSidebarOpen} minWidth={200}>
          {leftSidebar}
        </ResizableSidebar>

        {/* 4. Main Area (Tabs + Score) + Inspector (if bottom) */}
        <div className="bg-background relative flex min-w-0 flex-1 flex-col">
          {/* Main Content Wrapper: includes TabBar, Score, and potentially Bottom Inspector */}
          <div className="flex flex-1 overflow-hidden">
            {/* Score Area */}
            <div className="flex flex-1 flex-col overflow-hidden">
              {tabBar}
              <main className="bg-surface items-[safe_center] relative flex flex-1 flex-col overflow-auto p-0">
                <div className="bg-surface mx-auto min-w-min">
                  {mainContent}
                </div>
              </main>
            </div>

            {/* Right Sidebar (if positioned right) */}
            {inspectorPosition === "right" && (
              <ResizableSidebar
                side="right"
                isOpen={isRightSidebarOpen}
                minWidth={200}
              >
                {rightSidebar}
              </ResizableSidebar>
            )}
          </div>

          {/* Bottom Sidebar (if positioned bottom) */}
          {inspectorPosition === "bottom" && (
            <ResizableSidebar
              side="bottom"
              isOpen={isRightSidebarOpen}
              minHeight={150}
            >
              {rightSidebar}
            </ResizableSidebar>
          )}
        </div>
      </div>

      {/* 5. StatusBar */}
      {statusBar}

      {/* Overlays */}
      {contextMenu}
    </div>
  );
};
