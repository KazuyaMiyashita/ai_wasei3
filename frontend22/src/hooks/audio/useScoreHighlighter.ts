import { useEffect } from "react";
import { useAudioContext } from "../../context/AudioContext";

export function useScoreHighlighter() {
  const { performer } = useAudioContext();

  useEffect(() => {
    // Subscribe to active ID changes from the performer
    const unsubscribe = performer.subscribeActiveIds((activeIds) => {
      // Clear previous highlights not in the new set
      const currentSet = new Set(activeIds);

      // Cleanup previous highlights
      const currentlyPlaying = document.querySelectorAll(".hitbox.is-playing");
      currentlyPlaying.forEach((el) => {
        const id = el.getAttribute("corresp")?.replace("#", "");
        if (id && !currentSet.has(id)) {
          el.classList.remove("is-playing");
        }
      });

      // Add highlight to new items
      activeIds.forEach((id) => {
        const hitbox = document.querySelector(
          `.hitbox[corresp="#${CSS.escape(id)}"]`,
        );
        if (hitbox && !hitbox.classList.contains("is-playing")) {
          hitbox.classList.add("is-playing");
        }
      });
    });

    return () => {
      unsubscribe();
      // Cleanup highlights on unmount
      const currentlyPlaying = document.querySelectorAll(".hitbox.is-playing");
      currentlyPlaying.forEach((el) => {
        el.classList.remove("is-playing");
      });
    };
  }, [performer]);
}
