import type React from "react";
import type { ScorePosition } from "./ScoreDisplay";

interface StatusBarProps {
  latestPosition?: ScorePosition;
  loading: boolean;
}

const StatusBar: React.FC<StatusBarProps> = ({ latestPosition, loading }) => {
  return (
    <footer className="border-border-main bg-footer text-text-sub relative z-30 flex h-6 items-center justify-between border-t px-4 text-[10px] font-medium">
      <div className="flex items-center gap-4">
        {loading ? (
          <div className="flex items-center gap-2">
            <div className="bg-brand h-2 w-2 animate-pulse rounded-full" />
            <span className="text-brand">Loading score...</span>
          </div>
        ) : (
          <>
            <div className="flex items-center gap-1">
              <span className="text-text-muted">Measure:</span>
              <span className="text-text-main font-bold">
                {latestPosition?.measure || "--"}
              </span>
            </div>
            <div className="flex items-center gap-1">
              <span className="text-text-muted">Beat:</span>
              <span className="text-text-main font-bold">
                {latestPosition?.beat || "--"}
              </span>
            </div>
            <div className="flex items-center gap-1">
              <span className="text-text-muted">Staff:</span>
              <span className="text-text-main font-bold">
                {latestPosition?.part || "--"}
              </span>
            </div>
          </>
        )}
      </div>
      <div className="flex items-center gap-4">
        <span className="text-text-muted">Version 0.1.0</span>
        <div className="flex items-center gap-1">
          <div className="bg-success h-2 w-2 rounded-full" />
          <span className="text-success">Connected</span>
        </div>
      </div>
    </footer>
  );
};

export default StatusBar;
