import { CirclePause, CirclePlay, CircleStop } from "lucide-react";
import type React from "react";
import { useEffect, useRef } from "react";
import { useAudioContext } from "../../../context/AudioContext";
import { useApi } from "../../../hooks/api/useApi";
import { useScoreHighlighter } from "../../../hooks/audio/useScoreHighlighter";
import type { UseScoreView } from "../../../hooks/score/useScoreView";
import { useNotification } from "../../../hooks/useNotification";
import type { PartData } from "../../../lib/audio/performer";
import { ButtonGroup } from "../../ui/ButtonGroup";
import { IconButton } from "../../ui/IconButton";
import PerformanceVisualizer from "./PerformanceVisualizer";

interface ScorePlayerProps {
  meiXML: UseScoreView["meiXML"];
}

const ScorePlayer: React.FC<ScorePlayerProps> = ({ meiXML }) => {
  const { performer, isPlaying } = useAudioContext();
  const { getPerformanceData } = useApi();
  const { notify } = useNotification();

  // Enable score highlighting
  useScoreHighlighter();

  // Cache for performance data
  const performanceCacheRef = useRef<{
    xmlString: string;
    data: PartData[];
  } | null>(null);

  // Register onEnd callback to performer
  useEffect(() => {
    performer.setOnEnd(() => {
      // Playback ended naturally
    });
  }, [performer]);

  const handlePlayClick = async () => {
    if (isPlaying) {
      performer.stop();
    } else if (meiXML) {
      try {
        const serializer = new XMLSerializer();
        const xmlString = serializer.serializeToString(meiXML);
        let partData: PartData[];

        if (
          performanceCacheRef.current &&
          performanceCacheRef.current.xmlString === xmlString
        ) {
          partData = performanceCacheRef.current.data;
        } else {
          partData = await getPerformanceData(meiXML);
          performanceCacheRef.current = {
            xmlString: xmlString,
            data: partData,
          };
        }

        performer.play(partData);
      } catch (err) {
        console.error("Playback failed:", err);
        // Error notification handled by useApi
      }
    } else {
      notify("楽譜データが読み込まれていません", "warn", "Player");
    }
  };

  const handleStopClick = () => {
    if (!isPlaying) return;
    performer.stop();
  };

  return (
    <div className="flex w-full items-center justify-between gap-4">
      {/* Buttons Group */}
      <ButtonGroup>
        <IconButton
          onClick={handlePlayClick}
          className={
            isPlaying
              ? "bg-surface text-warning shadow-sm"
              : "text-success hover:bg-surface-hover"
          }
          title={isPlaying ? "Pause" : "Play"}
          icon={
            isPlaying ? (
              <CirclePause className="h-4 w-4" />
            ) : (
              <CirclePlay className="h-4 w-4" />
            )
          }
        />
        <IconButton
          onClick={handleStopClick}
          className={
            isPlaying
              ? "text-error hover:bg-surface-hover cursor-pointer active:scale-95"
              : "text-text-muted cursor-default"
          }
          title="Stop"
          icon={<CircleStop className="h-4 w-4" />}
        />
      </ButtonGroup>

      {/* Visualizer on the Right */}
      <div className="bg-surface border-border-main relative h-10 flex-1 overflow-hidden rounded-lg border">
        <PerformanceVisualizer />
      </div>
    </div>
  );
};

export default ScorePlayer;
