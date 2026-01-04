import type { AudioEngine } from "./engine";

export interface MetadataEvent {
  time: number;
  type: "noteon" | "noteoff";
  elementId: string;
}

export interface PartData {
  id: number;
  pitch: { time: number; value: number }[];
  intensity: { time: number; value: number }[];
  metadata: MetadataEvent[];
}

export interface VisualData {
  [id: number]: { pitch: number; intensity: number };
}

type PlaybackListener = (isPlaying: boolean) => void;
type ActiveIdsListener = (activeIds: string[]) => void;

export class Performer {
  private engine: AudioEngine;
  private _isPlaying = false;
  private startTime = 0;
  private listeners: Set<PlaybackListener> = new Set();
  private activeIdsListeners: Set<ActiveIdsListener> = new Set();
  private onEndCallback: (() => void) | null = null;

  // State
  private currentData: PartData[] = [];
  private metadataCursors: Map<number, number> = new Map();
  private activeNoteIds: Set<string> = new Set();
  private maxTimeMs = 0;

  // Current frame data for polling
  private currentVisualData: VisualData = {};
  private prevActiveNoteIds: Set<string> = new Set();

  constructor(engine: AudioEngine) {
    this.engine = engine;
  }

  get isPlaying() {
    return this._isPlaying;
  }

  // Subscribe to playback state changes (play/stop)
  subscribePlayback(listener: PlaybackListener) {
    this.listeners.add(listener);
    // Initial call
    listener(this._isPlaying);
    return () => this.listeners.delete(listener);
  }

  // Subscribe to active note changes (sparse updates)
  subscribeActiveIds(listener: ActiveIdsListener) {
    this.activeIdsListeners.add(listener);
    return () => this.activeIdsListeners.delete(listener);
  }

  setOnEnd(callback: () => void) {
    this.onEndCallback = callback;
  }

  // Polling method for high-frequency visualizer
  getVisualData(): VisualData {
    if (!this._isPlaying) return {};
    this.updateLoopState(); // Calculate state for the current moment
    return this.currentVisualData;
  }

  play(data: PartData[]) {
    const ctx = this.engine.init();
    this.stop(); // Ensure clean slate

    this.currentData = data;
    this.startTime = ctx.currentTime + 0.1; // Start slightly in future
    this._isPlaying = true;
    this.notifyPlayback();

    // Reset metadata cursors
    this.metadataCursors.clear();
    this.activeNoteIds.clear();
    this.prevActiveNoteIds.clear();
    data.forEach((p) => {
      this.metadataCursors.set(p.id, 0);
    });

    // Calculate duration
    this.maxTimeMs = 0;
    data.forEach((part) => {
      const lastPitch = part.pitch[part.pitch.length - 1]?.time || 0;
      const lastInt = part.intensity[part.intensity.length - 1]?.time || 0;
      const lastMeta = part.metadata[part.metadata.length - 1]?.time || 0;
      this.maxTimeMs = Math.max(this.maxTimeMs, lastPitch, lastInt, lastMeta);
    });

    // Schedule parts
    const stopTime = this.startTime + this.maxTimeMs / 1000 + 0.1;
    data.forEach((part) => {
      this.engine.schedulePart(
        part.id,
        this.startTime,
        part.pitch,
        part.intensity,
        stopTime,
      );
    });
  }

  stop() {
    if (!this._isPlaying) return;

    this._isPlaying = false;
    this.engine.stopAll();

    this.activeNoteIds.clear();
    this.currentVisualData = {};

    this.notifyPlayback();
    this.notifyActiveIds();
  }

  // Calculate current state based on time
  // This is called by getVisualData() typically inside rAF
  private updateLoopState() {
    if (!this._isPlaying) return;

    const currentTime = this.engine.currentTime - this.startTime;
    const currentTimeMs = currentTime * 1000;

    if (currentTimeMs > this.maxTimeMs + 50) {
      this.stop();
      if (this.onEndCallback) this.onEndCallback();
      return;
    }

    const currentVals: VisualData = {};

    // Update Pitch/Intensity visuals
    this.currentData.forEach((part) => {
      const pVal = this.interpolate(part.pitch, currentTimeMs);
      const iVal = this.interpolate(part.intensity, currentTimeMs);
      currentVals[part.id] = { pitch: pVal, intensity: iVal };

      // Process Metadata
      const cursor = this.metadataCursors.get(part.id) || 0;
      let newCursor = cursor;

      while (newCursor < part.metadata.length) {
        const event = part.metadata[newCursor];
        if (event.time <= currentTimeMs) {
          if (event.type === "noteon") {
            this.activeNoteIds.add(event.elementId);
          } else if (event.type === "noteoff") {
            this.activeNoteIds.delete(event.elementId);
          }
          newCursor++;
        } else {
          break;
        }
      }
      this.metadataCursors.set(part.id, newCursor);
    });

    this.currentVisualData = currentVals;

    // Check if activeIds changed
    if (this.hasActiveIdsChanged()) {
      this.notifyActiveIds();
      this.prevActiveNoteIds = new Set(this.activeNoteIds);
    }
  }

  private hasActiveIdsChanged(): boolean {
    if (this.activeNoteIds.size !== this.prevActiveNoteIds.size) return true;
    for (const id of this.activeNoteIds) {
      if (!this.prevActiveNoteIds.has(id)) return true;
    }
    return false;
  }

  private notifyPlayback() {
    this.listeners.forEach((l) => {
      l(this._isPlaying);
    });
  }

  private notifyActiveIds() {
    const ids = Array.from(this.activeNoteIds);
    this.activeIdsListeners.forEach((l) => {
      l(ids);
    });
  }

  private interpolate(
    points: { time: number; value: number }[],
    timeMs: number,
  ): number {
    if (points.length === 0) return 0;
    if (timeMs <= points[0].time) return points[0].value;
    if (timeMs >= points[points.length - 1].time)
      return points[points.length - 1].value;

    const index = points.findIndex((p) => p.time > timeMs);
    if (index === -1) return points[points.length - 1].value;

    const p1 = points[index - 1];
    const p2 = points[index];

    // Match audio behavior: maintain value until 1ms before target
    if (p2.time - p1.time > 1) {
      if (timeMs < p2.time - 1) {
        return p1.value;
      }
      // Ramp in last 1ms
      const t = (timeMs - (p2.time - 1)) / 1;
      return p1.value + (p2.value - p1.value) * t;
    }

    const t = (timeMs - p1.time) / (p2.time - p1.time);
    return p1.value + (p2.value - p1.value) * t;
  }
}
