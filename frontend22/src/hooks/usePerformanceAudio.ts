import { useCallback, useEffect, useRef, useState } from "react";
import type { PartData } from "../lib/performance-parser";

export function usePerformanceAudio(onEnd?: () => void) {
  const audioContextRef = useRef<AudioContext | null>(null);
  const nodesRef = useRef<{
    [id: number]: { osc: OscillatorNode; gain: GainNode };
  }>({});
  const masterGainRef = useRef<GainNode | null>(null);
  const [isPlaying, setIsPlaying] = useState(false);
  const isPlayingRef = useRef(false);
  const startTimeRef = useRef<number>(0);
  const animationFrameRef = useRef<number | null>(null);

  // Keep track of the latest data for visualization
  const currentDataRef = useRef<PartData[]>([]);

  // Metadata processing state
  const metadataCursorsRef = useRef<Map<number, number>>(new Map());
  const activeNoteIdsRef = useRef<Set<string>>(new Set());

  // Visualization state
  const [visualData, setVisualData] = useState<{
    [id: number]: { pitch: number; intensity: number };
  }>({});
  const [activeIds, setActiveIds] = useState<string[]>([]);

  const stop = useCallback(() => {
    setIsPlaying(false);
    isPlayingRef.current = false;
    if (animationFrameRef.current) {
      cancelAnimationFrame(animationFrameRef.current);
      animationFrameRef.current = null;
    }

    const ctx = audioContextRef.current;
    if (ctx) {
      const now = ctx.currentTime;
      Object.values(nodesRef.current).forEach(({ osc, gain }) => {
        try {
          gain.gain.cancelScheduledValues(now);
          gain.gain.setValueAtTime(gain.gain.value, now);
          gain.gain.linearRampToValueAtTime(0, now + 0.05);
          osc.stop(now + 0.05);
        } catch (_e) {
          // Ignore
        }
      });
      nodesRef.current = {};
    }
    setVisualData({});
    setActiveIds([]);
    activeNoteIdsRef.current.clear();
  }, []);

  const initAudio = useCallback(() => {
    if (!audioContextRef.current) {
      const AudioCtor =
        window.AudioContext ||
        (window as unknown as { webkitAudioContext: typeof AudioContext })
          .webkitAudioContext;
      audioContextRef.current = new AudioCtor();

      masterGainRef.current = audioContextRef.current.createGain();
      masterGainRef.current.connect(audioContextRef.current.destination);
      masterGainRef.current.gain.value = 0.5; // Overall volume
    } else if (audioContextRef.current.state === "suspended") {
      audioContextRef.current.resume();
    }
    return audioContextRef.current;
  }, []);

  useEffect(() => {
    return () => {
      stop();
      if (audioContextRef.current) {
        audioContextRef.current.close();
      }
    };
  }, [stop]);

  const play = useCallback(
    (data: PartData[]) => {
      const ctx = initAudio();
      stop(); // Ensure clean slate

      currentDataRef.current = data;
      startTimeRef.current = ctx.currentTime + 0.1; // Start slightly in future
      setIsPlaying(true);
      isPlayingRef.current = true;

      // Reset metadata cursors
      metadataCursorsRef.current.clear();
      activeNoteIdsRef.current.clear();
      data.forEach((p) => {
        metadataCursorsRef.current.set(p.id, 0);
      });

      // Calculate duration
      let maxTimeMs = 0;
      data.forEach((part) => {
        const lastPitch = part.pitch[part.pitch.length - 1]?.time || 0;
        const lastInt = part.intensity[part.intensity.length - 1]?.time || 0;
        const lastMeta = part.metadata[part.metadata.length - 1]?.time || 0;
        maxTimeMs = Math.max(maxTimeMs, lastPitch, lastInt, lastMeta);
      });

      // Create nodes for each part
      data.forEach((part) => {
        const osc = ctx.createOscillator();
        const gain = ctx.createGain();

        // Create a custom periodic wave for additive synthesis
        // Fundamental + 4 harmonics
        const real = new Float32Array([0, 1, 0.1, 0.05, 0.05, 0.01]);
        // const real = new Float32Array([0, 1, 0, 0, 0, 0]);
        const imag = new Float32Array([0, 0, 0, 0, 0, 0]);
        const wave = ctx.createPeriodicWave(real, imag);

        osc.setPeriodicWave(wave);
        osc.connect(gain);
        if (masterGainRef.current) {
          gain.connect(masterGainRef.current);
        }

        // Schedule Pitch
        if (part.pitch.length > 0) {
          osc.frequency.setValueAtTime(
            part.pitch[0].value,
            startTimeRef.current + part.pitch[0].time / 1000,
          );
          for (let i = 1; i < part.pitch.length; i++) {
            const point = part.pitch[i];
            const prevPoint = part.pitch[i - 1];
            const time = startTimeRef.current + point.time / 1000;

            // Maintain previous value until 1ms before the target time
            if (point.time - prevPoint.time > 1) {
              osc.frequency.setValueAtTime(prevPoint.value, time - 0.001);
            }
            osc.frequency.linearRampToValueAtTime(point.value, time);
          }
        }

        // Schedule Intensity
        gain.gain.setValueAtTime(0, startTimeRef.current);

        if (part.intensity.length > 0) {
          part.intensity.forEach((point, i) => {
            const time = startTimeRef.current + point.time / 1000;
            const linear = point.value <= -90 ? 0 : 10 ** (point.value / 20);

            if (i > 0) {
              const prevPoint = part.intensity[i - 1];
              const prevLinear =
                prevPoint.value <= -90 ? 0 : 10 ** (prevPoint.value / 20);

              // Maintain previous value until 1ms before the target time
              if (point.time - prevPoint.time > 1) {
                gain.gain.setValueAtTime(prevLinear, time - 0.001);
              }
            }

            gain.gain.linearRampToValueAtTime(linear, time);
          });
        }

        osc.start(startTimeRef.current);

        // Stop osc after the last event
        osc.stop(startTimeRef.current + maxTimeMs / 1000 + 0.1);

        nodesRef.current[part.id] = { osc, gain };
      });

      // Start visualization loop
      const updateVisuals = () => {
        if (!ctx) return;
        const currentTime = ctx.currentTime - startTimeRef.current;
        const currentTimeMs = currentTime * 1000;

        if (currentTimeMs > maxTimeMs + 50) {
          stop();
          if (onEnd) onEnd();
          return;
        }

        const currentVals: {
          [id: number]: { pitch: number; intensity: number };
        } = {};

        // Update Pitch/Intensity visuals
        currentDataRef.current.forEach((part) => {
          const pVal = interpolate(part.pitch, currentTimeMs);
          const iVal = interpolate(part.intensity, currentTimeMs);
          currentVals[part.id] = { pitch: pVal, intensity: iVal };

          // Process Metadata
          const cursor = metadataCursorsRef.current.get(part.id) || 0;
          let newCursor = cursor;

          while (newCursor < part.metadata.length) {
            const event = part.metadata[newCursor];
            if (event.time <= currentTimeMs) {
              if (event.type === "noteon") {
                activeNoteIdsRef.current.add(event.elementId);
              } else if (event.type === "noteoff") {
                activeNoteIdsRef.current.delete(event.elementId);
              }
              newCursor++;
            } else {
              break;
            }
          }
          metadataCursorsRef.current.set(part.id, newCursor);
        });

        setVisualData(currentVals);
        setActiveIds(Array.from(activeNoteIdsRef.current));

        if (isPlayingRef.current) {
          animationFrameRef.current = requestAnimationFrame(updateVisuals);
        }
      };

      animationFrameRef.current = requestAnimationFrame(updateVisuals);
    },
    [onEnd, initAudio, stop],
  );

  return {
    play,
    stop,
    isPlaying,
    visualData,
    activeIds,
  };
}

// Helper for UI interpolation
function interpolate(
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
