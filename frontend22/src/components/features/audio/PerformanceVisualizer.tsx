import type React from "react";
import { useCallback, useEffect, useRef } from "react";
import { useAudioContext } from "../../../context/AudioContext";

const PerformanceVisualizer: React.FC = () => {
  const { performer, isPlaying } = useAudioContext();
  const requestRef = useRef<number>();

  // Refs for direct DOM manipulation
  const barsRef = useRef<{ [key: number]: HTMLDivElement | null }>({});
  const textsRef = useRef<{ [key: number]: HTMLSpanElement | null }>({});

  const channels = [1, 2, 3, 4];

  const animate = useCallback(() => {
    if (!isPlaying) return;

    // Pull data from performer
    const data = performer.getVisualData();

    channels.forEach((id) => {
      const info = data[id] || { pitch: 0, intensity: -96 };
      const normalizedVol = Math.max(0, (info.intensity + 96) / 96);

      const bar = barsRef.current[id];
      const text = textsRef.current[id];

      if (bar) {
        bar.style.width = `${normalizedVol * 100}%`;
      }
      if (text) {
        text.textContent = info.pitch > 0 ? `${Math.round(info.pitch)}Hz` : "-";
      }
    });

    requestRef.current = requestAnimationFrame(animate);
  }, [isPlaying, performer]); // Removed 'channels' as it is constant inside component (though ideally define outside or memoize)

  useEffect(() => {
    if (isPlaying) {
      requestRef.current = requestAnimationFrame(animate);
    } else {
      // Reset visuals when stopped
      channels.forEach((id) => {
        const bar = barsRef.current[id];
        const text = textsRef.current[id];
        if (bar) bar.style.width = "0%";
        if (text) text.textContent = "-";
      });
      if (requestRef.current) {
        cancelAnimationFrame(requestRef.current);
      }
    }

    return () => {
      if (requestRef.current) {
        cancelAnimationFrame(requestRef.current);
      }
    };
  }, [isPlaying, animate]); // 'performer' is implicitly included in animate

  return (
    <div className="flex w-full max-w-sm flex-col gap-1.5 px-4">
      {channels.map((id) => (
        <div key={id} className="flex items-center gap-3">
          <span className="w-6 text-[10px] font-bold text-gray-500">
            CH{id}
          </span>
          <div className="h-1.5 flex-1 overflow-hidden rounded-full border border-gray-200 bg-gray-100">
            <div
              ref={(el) => {
                barsRef.current[id] = el;
              }}
              className="h-full bg-indigo-500 transition-none ease-linear"
              style={{ width: "0%" }}
            />
          </div>
          <span
            ref={(el) => {
              textsRef.current[id] = el;
            }}
            className="w-12 text-right font-mono text-[10px] text-gray-400"
          >
            -
          </span>
        </div>
      ))}
    </div>
  );
};

export default PerformanceVisualizer;
