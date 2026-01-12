import { Subscribable } from "../shared/subscribable";

export type LogLevel = "info" | "warn" | "error";

export interface LogEntry {
  message: string;
  level: LogLevel;
  sender: string;
  timestamp: number;
}

export class Logger extends Subscribable<{ lastLog: LogEntry | null }> {
  private _lastLog: LogEntry | null = null;

  constructor() {
    super();
    this.updateState();
  }

  notify(message: string, level: LogLevel, sender = "System") {
    const entry: LogEntry = {
      message,
      level,
      sender,
      timestamp: Date.now(),
    };
    this._lastLog = entry;
    this.updateState();
  }

  get lastLog() {
    return this._lastLog;
  }

  private updateState() {
    this.emit({ lastLog: this._lastLog });
  }
}
