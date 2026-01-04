import type React from "react";

interface HeaderProps {
  workspaceControls: React.ReactNode;
  scoreTools?: React.ReactNode;
  mainContent: React.ReactNode;
  viewControls: React.ReactNode;
}

const Header: React.FC<HeaderProps> = ({
  workspaceControls,
  scoreTools,
  mainContent,
  viewControls,
}) => {
  return (
    <header className="border-border-main bg-header text-text-main relative z-20 flex h-14 items-center justify-between border-b px-4">
      {/* Left Area */}
      <div className="flex items-center">
        {workspaceControls}
        {scoreTools && <div className="ml-2">{scoreTools}</div>}
      </div>

      {/* Center Area */}
      <div className="mx-4 flex flex-1 items-center justify-center gap-6">
        {mainContent}
      </div>

      {/* Right Area */}
      <div className="flex items-center gap-4">{viewControls}</div>
    </header>
  );
};

export default Header;
