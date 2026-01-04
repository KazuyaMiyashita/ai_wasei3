import { useContext } from "react";
import {
  type LogEntry,
  type LogLevel,
  NotificationContext,
} from "../context/NotificationContext";

export type { LogEntry, LogLevel };

export interface UseNotification {
  notify: (message: string, level: LogLevel, sender?: string) => void;
  lastLog: LogEntry | null;
}

export function useNotification(): UseNotification {
  const context = useContext(NotificationContext);
  if (context === undefined) {
    throw new Error(
      "useNotification must be used within a NotificationProvider",
    );
  }
  return context;
}
