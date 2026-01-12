import { Info, OctagonX, TriangleAlert } from "lucide-react";
import { useEffect, useState } from "react";
import { useApplication } from "../../context/ApplicationContext";
import type { LogEntry } from "../../lib/infrastructure/logger";

const Notification = () => {
  const application = useApplication();
  const [lastLog, setLastLog] = useState<LogEntry | null>(null);

  useEffect(() => {
    const unsubscribe = application.logger.subscribe((state) => {
      if (state.lastLog) {
        setLastLog(state.lastLog);
      }
    });
    return unsubscribe;
  }, [application]);

  if (!lastLog) return null;

  const { level, message, sender } = lastLog;

  return (
    <div className="flex min-w-0 items-center gap-2 overflow-hidden text-ellipsis whitespace-nowrap">
      {level === "info" && (
        <div className="flex min-w-0 items-center gap-2">
          <Info
            className="inline-block h-[1.1em] w-[1.1em] shrink-0 align-text-bottom"
            strokeWidth={2}
          />{" "}
          <span className="truncate">
            [{sender}] {message}
          </span>
        </div>
      )}
      {level === "warn" && (
        <div className="flex min-w-0 items-center gap-2 text-amber-500">
          <TriangleAlert
            className="inline-block h-[1.1em] w-[1.1em] shrink-0 align-text-bottom"
            strokeWidth={2}
          />{" "}
          <span className="truncate">
            [{sender}] {message}
          </span>
        </div>
      )}
      {level === "error" && (
        <div className="flex min-w-0 items-center gap-2 text-red-500">
          <OctagonX
            className="inline-block h-[1.1em] w-[1.1em] shrink-0 align-text-bottom"
            strokeWidth={2}
          />{" "}
          <span className="truncate">
            [{sender}] {message}
          </span>
        </div>
      )}
    </div>
  );
};

export default Notification;
