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

export function parsePerformanceData(text: string): PartData[] {
  const lines = text.split("\n");
  const parts = new Map<number, PartData>();

  // Helper to get or create part data
  const getPart = (id: number) => {
    let part = parts.get(id);
    if (!part) {
      part = { id, pitch: [], intensity: [], metadata: [] };
      parts.set(id, part);
    }
    return part;
  };

  for (const line of lines) {
    const trimmed = line.trim();
    if (!trimmed) continue;

    // Example: /performer/2/intensity 0.0 -96.0
    // Example: /metadata/1/noteon 0.0 u1no7fe3
    const [addr, arg1, arg2] = trimmed.split(" ");
    if (!addr || arg1 === undefined) continue;

    // Handle /performer/begin or /performer/end
    if (addr === "/performer/begin" || addr === "/performer/end") {
      continue;
    }

    const segments = addr.split("/");
    // segments: ["", "performer", "2", "intensity"] or ["", "metadata", "1", "noteon"]
    if (segments.length < 4) continue;

    const category = segments[1]; // "performer" or "metadata"
    const partId = parseInt(segments[2], 10);
    const type = segments[3];
    const timeMs = parseFloat(arg1);

    if (Number.isNaN(partId) || Number.isNaN(timeMs)) continue;

    const part = getPart(partId);

    if (category === "performer") {
      const value = arg2 ? parseFloat(arg2) : 0;
      if (Number.isNaN(value)) continue;

      if (type === "pitch") {
        part.pitch.push({ time: timeMs, value });
      } else if (type === "intensity") {
        part.intensity.push({ time: timeMs, value });
      }
    } else if (category === "metadata") {
      const elementId = arg2;
      if (!elementId) continue;

      if (type === "noteon" || type === "noteoff") {
        part.metadata.push({ time: timeMs, type: type, elementId });
      }
    }
  }

  // Sort events by time
  for (const part of parts.values()) {
    part.pitch.sort((a, b) => a.time - b.time);
    part.intensity.sort((a, b) => a.time - b.time);
    part.metadata.sort((a, b) => a.time - b.time);
  }

  // Return as array, sorted by ID
  return Array.from(parts.values()).sort((a, b) => a.id - b.id);
}
