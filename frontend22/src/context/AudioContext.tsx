import type React from "react";
import { createContext, useContext, useEffect, useRef, useState } from "react";
import { AudioEngine } from "../lib/audio/engine";
import { Performer } from "../lib/audio/performer";

interface AudioContextValue {
  engine: AudioEngine;
  performer: Performer;
  isPlaying: boolean;
}

const AudioContext = createContext<AudioContextValue | null>(null);

export const AudioProvider: React.FC<{ children: React.ReactNode }> = ({
  children,
}) => {
  // Use useRef to maintain stable instances across re-renders
  const engineRef = useRef<AudioEngine | null>(null);
  const performerRef = useRef<Performer | null>(null);
  const [isPlaying, setIsPlaying] = useState(false);

  // Initialize once
  if (!engineRef.current) {
    engineRef.current = new AudioEngine();
    performerRef.current = new Performer(engineRef.current);
  }

  useEffect(() => {
    const performer = performerRef.current;
    if (!performer) return;

    // Sync React state with Performer state
    const unsubscribe = performer.subscribePlayback((playing) => {
      setIsPlaying(playing);
    });

    return () => {
      unsubscribe();
      // Cleanup on unmount
      if (engineRef.current) {
        engineRef.current.close();
      }
    };
  }, []);

  // Ensure instances exist (though they should be created above)
  const engine = engineRef.current;
  const performer = performerRef.current;

  if (!engine || !performer) {
    return null; // Should not happen
  }

  const value: AudioContextValue = {
    engine,
    performer,
    isPlaying,
  };

  return (
    <AudioContext.Provider value={value}>{children}</AudioContext.Provider>
  );
};

export function useAudioContext(): AudioContextValue {
  const context = useContext(AudioContext);
  if (!context) {
    throw new Error("useAudioContext must be used within an AudioProvider");
  }
  return context;
}
