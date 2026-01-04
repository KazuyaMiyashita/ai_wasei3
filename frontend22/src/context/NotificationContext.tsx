import { createContext, type ReactNode, useCallback, useState } from "react";
import { Snackbar } from "../components/ui/Snackbar";

export type LogLevel = "info" | "warn" | "error";

export interface LogEntry {
  message: string;
  level: LogLevel;
  sender: string;
  timestamp: number;
}

interface NotificationContextType {
  notify: (message: string, level: LogLevel, sender?: string) => void;
  lastLog: LogEntry | null;
}

export const NotificationContext = createContext<
  NotificationContextType | undefined
>(undefined);

export function NotificationProvider({ children }: { children: ReactNode }) {
  const [lastLog, setLastLog] = useState<LogEntry | null>(null);
  const [snackbarState, setSnackbarState] = useState<{
    message: string;
    isOpen: boolean;
  }>({ message: "", isOpen: false });

  const notify = useCallback(
    (message: string, level: LogLevel, sender = "System") => {
      const entry: LogEntry = {
        message,
        level,
        sender,
        timestamp: Date.now(),
      };
      setLastLog(entry);
      const msg = `[${sender}] ${message}`;
      console.trace(msg);
      if (level === "error") {
        setSnackbarState({ message: msg, isOpen: true });
      }
    },
    [],
  );

  const closeSnackbar = useCallback(() => {
    setSnackbarState((prev) => ({ ...prev, isOpen: false }));
  }, []);

  return (
    <NotificationContext.Provider value={{ notify, lastLog }}>
      {children}
      <Snackbar
        message={snackbarState.message}
        isOpen={snackbarState.isOpen}
        onClose={closeSnackbar}
      />
    </NotificationContext.Provider>
  );
}
