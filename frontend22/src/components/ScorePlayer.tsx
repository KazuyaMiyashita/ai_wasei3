import { CirclePause, CirclePlay, CircleStop } from "lucide-react";
import type React from "react";
import { useEffect, useRef, useState } from "react";
import { usePerformanceAudio } from "../hooks/usePerformanceAudio";
import { parsePerformanceData } from "../lib/performance-parser";
import PerformanceVisualizer from "./PerformanceVisualizer";

interface ScorePlayerProps {
  isPlaying: boolean;
  onTogglePlayback: () => void;
  onStopPlayback: () => void;
  meiXML?: Document;
}

const ScorePlayer: React.FC<ScorePlayerProps> = ({
  isPlaying,
  onTogglePlayback,
  onStopPlayback,
  meiXML,
}) => {
  const {
    play: playAudio,
    stop: stopAudio,
    visualData,
    activeIds,
  } = usePerformanceAudio(onStopPlayback);
  const prevActiveIdsRef = useRef<string[]>([]);
  const [error, setError] = useState<string | null>(null);

  // Cache for performance data
  const performanceCacheRef = useRef<{
    xmlString: string;
    data: ReturnType<typeof parsePerformanceData>;
  } | null>(null);

  // Highlighting logic
  useEffect(() => {
    const currentSet = new Set(activeIds);
    prevActiveIdsRef.current.forEach((id) => {
      if (!currentSet.has(id)) {
        const hitbox = document.querySelector(
          `.hitbox[corresp="#${CSS.escape(id)}"]`,
        );
        if (hitbox) {
          hitbox.classList.remove("is-selected");
        }
      }
    });

    activeIds.forEach((id) => {
      const hitbox = document.querySelector(
        `.hitbox[corresp="#${CSS.escape(id)}"]`,
      );
      if (hitbox) {
        hitbox.classList.add("is-selected");
      }
    });

    prevActiveIdsRef.current = activeIds;
  }, [activeIds]);

  // Stop audio if global isPlaying becomes false
  useEffect(() => {
    if (!isPlaying) {
      stopAudio();
    }
  }, [isPlaying, stopAudio]);

  const handlePlayClick = async () => {
    if (isPlaying) {
      stopAudio();
      onTogglePlayback();
    } else if (meiXML) {
      setError(null);
      try {
        const serializer = new XMLSerializer();
        const xmlString = serializer.serializeToString(meiXML);

        let partData: ReturnType<typeof parsePerformanceData>;

        if (
          performanceCacheRef.current &&
          performanceCacheRef.current.xmlString === xmlString
        ) {
          partData = performanceCacheRef.current.data;
        } else {
          const formData = new FormData();
          const blob = new Blob([xmlString], { type: "application/xml" });
          formData.append("file", blob, "score.mei");

          const resPerform = await fetch("/perform", {
            method: "POST",
            body: formData,
          });

          if (!resPerform.ok) {
            const errData = await resPerform.json().catch(() => ({}));
            throw new Error(
              errData.message || "演奏データの生成に失敗しました",
            );
          }

          const text = await resPerform.text();
          partData = parsePerformanceData(text);

          performanceCacheRef.current = {
            xmlString: xmlString,
            data: partData,
          };
        }

        playAudio(partData);
        onTogglePlayback();
      } catch (err) {
        console.error("Playback failed:", err);
        setError(err instanceof Error ? err.message : String(err));
      }
    } else {
      setError("楽譜データが読み込まれていません");
    }
  };

  const handleStopClick = () => {
    if (!isPlaying) return;
    stopAudio();
    onStopPlayback();
  };

  return (
    <div className="flex w-full items-center justify-between gap-4">
      {/* Buttons Group */}
      <div className="bg-surface-header border-border-main flex shrink-0 rounded-lg border p-1">
        <button
          type="button"
          onClick={handlePlayClick}
          className={`cursor-pointer rounded p-1.5 transition-all active:scale-95 ${
            isPlaying
              ? "bg-surface text-warning shadow-sm"
              : "text-success hover:bg-surface-hover"
          } `}
          title={isPlaying ? "Pause" : "Play"}
        >
          {isPlaying ? (
            <CirclePause className="h-4 w-4" />
          ) : (
            <CirclePlay className="h-4 w-4" />
          )}
        </button>
        <button
          type="button"
          onClick={handleStopClick}
          className={`rounded p-1.5 transition-all ${
            isPlaying
              ? "text-error hover:bg-surface-hover cursor-pointer active:scale-95"
              : "text-text-muted cursor-default"
          } `}
          title="Stop"
        >
          <CircleStop className="h-4 w-4" />
        </button>
      </div>

      {/* Visualizer on the Right */}
      <div className="bg-surface border-border-main relative h-9 flex-1 overflow-hidden rounded border">
        <PerformanceVisualizer data={visualData} />
      </div>

      {/* Error Snackbar */}
      {error && (
        <div className="bg-text-main text-surface animate-in fade-in slide-in-from-top-4 fixed top-20 left-1/2 z-50 flex -translate-x-1/2 items-center gap-4 rounded-lg px-6 py-3 shadow-2xl duration-300">
          <span className="text-sm font-medium">{error}</span>
          <button
            type="button"
            onClick={() => setError(null)}
            className="hover:text-text-muted text-xl leading-none transition-colors"
          >
            ×
          </button>
        </div>
      )}
    </div>
  );
};

export default ScorePlayer;
