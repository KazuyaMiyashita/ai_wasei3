import { Pause, Play, Volume, Volume2, VolumeX } from "lucide-react";
import { Slider, Toolbar } from "radix-ui";
import React, { useCallback, useEffect, useRef, useState } from "react";
import {
  useApplication,
  useApplicationState,
} from "../../context/ApplicationContext";
import { cn } from "../../utils";

const PlayControl = ({
  isPlaying,
  onTogglePlay,
  disabled = false,
}: {
  isPlaying: boolean;
  onTogglePlay: () => void;
  disabled?: boolean;
}) => {
  return (
    <div className="flex shrink-0 items-center gap-0.5">
      {!isPlaying ? (
        <>
          {/* 再生ボタン */}
          <Toolbar.Button
            className="ui-action-button text-brand-primary hover:text-brand-primary p-1.5 disabled:opacity-50"
            onClick={onTogglePlay}
            aria-label="Play"
            disabled={disabled}
          >
            <Play size={14} fill="currentColor" />
          </Toolbar.Button>
        </>
      ) : (
        <>
          {/* 停止ボタン (Pause icon used for stop/pause behavior) */}
          <Toolbar.Button
            className="ui-action-button p-1.5 text-amber-500 hover:text-amber-500"
            onClick={onTogglePlay}
            aria-label="Stop"
          >
            <Pause size={14} fill="currentColor" />
          </Toolbar.Button>
        </>
      )}
    </div>
  );
};

const MonitorRow = ({
  label,
  levelDb,
  isPlaying,
}: {
  label: string;
  levelDb: number;
  isPlaying: boolean;
}) => {
  // Map -60dB to 0%, 0dB to 100%
  // Clamp between 0 and 100
  let percent = 0;
  if (levelDb > -90) {
    const minDb = -60;
    const maxDb = 0;
    const ratio = (levelDb - minDb) / (maxDb - minDb);
    percent = Math.max(0, Math.min(1, ratio)) * 100;
  }

  if (!isPlaying) percent = 0;

  return (
    <div className="text-ui-text-muted flex items-center gap-2 text-[9px] font-bold">
      <span className="hidden w-6 leading-none lg:block">{label}</span>
      <div className="bg-ui-bg-subtle border-ui-border/30 h-1 w-16 overflow-hidden rounded-full border">
        <div
          className={cn(
            "transition-width h-full duration-50 ease-out",
            isPlaying ? "bg-green-500/80" : "bg-ui-text-muted/40",
          )}
          style={{ width: `${percent}%` }}
        />
      </div>
    </div>
  );
};

const VolumeControl = () => {
  const [volume, setVolume] = React.useState(80);
  const isMuted = volume === 0;

  return (
    <div className="hidden items-center gap-2 px-2 sm:flex">
      <button
        type="button"
        onClick={() => setVolume(0)}
        className="ui-action-button"
        aria-label={isMuted ? "Muted" : "Mute (0%)"}
      >
        {isMuted ? <VolumeX size={14} /> : <Volume size={14} />}
      </button>

      <Slider.Root
        className="group relative flex h-5 w-20 cursor-pointer touch-none items-center select-none"
        value={[volume]}
        onValueChange={([v]) => setVolume(v)}
        max={100}
        step={1}
      >
        <Slider.Track className="bg-ui-bg-subtle border-ui-border/30 relative h-0.75 grow overflow-hidden rounded-full border">
          <Slider.Range className="bg-brand-primary/80 absolute h-full transition-colors" />
        </Slider.Track>
        <Slider.Thumb
          className="bg-ui-text-muted focus:ring-brand-primary/30 block h-2.5 w-2.5 rounded-full shadow-sm"
          aria-label="Master Volume"
        />
      </Slider.Root>

      <button
        type="button"
        onClick={() => setVolume(100)}
        className="ui-action-button"
        aria-label="Max Volume (100%)"
      >
        <Volume2 size={14} />
      </button>
    </div>
  );
};

export default function AudioControlBar() {
  const application = useApplication();
  const isPlaying = useApplicationState((state) => state.isPlaying);
  const currentDocumentId = useApplicationState(
    (state) => state.currentDocumentId,
  );

  const [levels, setLevels] = useState<{ [id: number]: number }>({});
  const rafRef = useRef<number>();

  useEffect(() => {
    if (!isPlaying) {
      setLevels({});
      if (rafRef.current) cancelAnimationFrame(rafRef.current);
      return;
    }

    const loop = () => {
      const data = application.performer.getVisualData();
      // Extract intensity from data
      const newLevels: { [id: number]: number } = {};
      Object.entries(data).forEach(([idStr, val]) => {
        const id = parseInt(idStr, 10);
        newLevels[id] = val.intensity;
      });
      setLevels(newLevels);
      rafRef.current = requestAnimationFrame(loop);
    };

    loop();

    return () => {
      if (rafRef.current) cancelAnimationFrame(rafRef.current);
    };
  }, [isPlaying, application]);

  const handleTogglePlay = useCallback(() => {
    if (isPlaying) {
      application.stopAudio();
    } else {
      application.playAudio();
    }
  }, [isPlaying, application]);

  return (
    <Toolbar.Root className="bg-ui-bg-base border-ui-border flex h-9 items-center gap-1 rounded-lg border px-2 shadow-sm">
      <PlayControl
        isPlaying={isPlaying}
        onTogglePlay={handleTogglePlay}
        disabled={!currentDocumentId}
      />

      <Toolbar.Separator className="bg-ui-border mx-1 hidden h-5 w-px shrink-0 lg:block" />

      {/* Visualizer Placeholder / Simple Monitor */}
      <div className="hidden min-w-max flex-col gap-1 px-2 lg:flex">
        <MonitorRow
          label="CH1"
          levelDb={levels[1] ?? -100}
          isPlaying={isPlaying}
        />
        <MonitorRow
          label="CH2"
          levelDb={levels[2] ?? -100}
          isPlaying={isPlaying}
        />
      </div>

      <Toolbar.Separator className="bg-ui-border mx-1 hidden h-5 w-px shrink-0 sm:block" />

      <VolumeControl />
    </Toolbar.Root>
  );
}
