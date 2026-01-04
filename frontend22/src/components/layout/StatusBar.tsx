import { Info, OctagonX, TriangleAlert } from "lucide-react";
import type React from "react";
import type { UseScoreInteraction } from "../../hooks/score/useScoreInteraction";
import type { UseScoreView } from "../../hooks/score/useScoreView";
import type { LogEntry } from "../../hooks/useNotification";
import { cn } from "../../lib/utils";

interface StatusBarProps {
  latestPosition: UseScoreInteraction["latestPosition"];
  loading: UseScoreView["loading"];
  lastLog: LogEntry | null;
  isConnected: boolean;
}

const StatusBar: React.FC<StatusBarProps> = ({
  latestPosition,
  loading,
  lastLog,
  isConnected,
}) => {
  return (
    <footer className="border-border-main bg-footer text-text-sub relative z-30 flex h-6 items-center justify-between border-t px-4 text-[10px] font-medium">
      <div className="flex items-center gap-4 flex-1 overflow-hidden">
        {loading ? (
          <div className="flex items-center gap-2 shrink-0">
            <div className="bg-brand h-2 w-2 animate-pulse rounded-full" />
            <span className="text-brand">Loading score...</span>
          </div>
        ) : (
          <>
            <div className="flex items-center gap-1 shrink-0">
              <span className="text-text-muted">Measure:</span>
              <span className="text-text-main font-bold">
                {latestPosition?.measure || "--"}
              </span>
            </div>
            <div className="flex items-center gap-1 shrink-0">
              <span className="text-text-muted">Beat:</span>
              <span className="text-text-main font-bold">
                {latestPosition?.beat || "--"}
              </span>
            </div>
            <div className="flex items-center gap-1 shrink-0">
              <span className="text-text-muted">Staff:</span>
              <span className="text-text-main font-bold">
                {latestPosition?.part || "--"}
              </span>
            </div>
          </>
        )}

        {/* Notification Area */}
        {lastLog && (
          <div className="flex items-center border-l border-border-main pl-4 overflow-hidden">
            <span>
              {(() => {
                switch (lastLog.level) {
                  case "info":
                    return (
                      <Info
                        className="mr-1 w-[1.1em] h-[1.1em] text-text-muted inline-block align-text-bottom"
                        strokeWidth={2}
                      />
                    );
                  case "warn":
                    return (
                      <TriangleAlert
                        className="mr-1 w-[1.1em] h-[1.1em] text-warning inline-block align-text-bottom"
                        strokeWidth={2.5}
                      />
                    );
                  case "error":
                    return (
                      <OctagonX
                        className="mr-1 w-[1.1em] h-[1.1em] text-error inline-block align-text-bottom"
                        strokeWidth={2.5}
                      />
                    );
                }
              })()}
            </span>
            <div
              className={cn(
                "truncate",
                lastLog.level === "info" && "text-text-muted",
                lastLog.level === "warn" && "text-warning font-bold",
                lastLog.level === "error" && "text-error font-bold",
              )}
            >
              <span className="mr-1">[{lastLog.sender}]</span>
              <span>{lastLog.message}</span>
            </div>
          </div>
        )}
      </div>

      <div className="flex items-center gap-4 shrink-0">
        <span className="text-text-muted">Version 0.1.0</span>
        <div className="flex items-center gap-1">
          <div
            className={cn(
              "h-2 w-2 rounded-full",
              isConnected ? "bg-success" : "bg-error",
            )}
          />
          <span
            className={cn(
              isConnected ? "text-success" : "text-error font-bold",
            )}
          >
            {isConnected ? "Connected" : "Disconnected"}
          </span>
        </div>
      </div>
    </footer>
  );
};

export default StatusBar;
