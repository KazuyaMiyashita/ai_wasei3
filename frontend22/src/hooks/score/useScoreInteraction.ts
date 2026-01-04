import { useCallback, useEffect, useRef, useState } from "react";
import { useAudioContext } from "../../context/AudioContext";
import { generateAudioForNote } from "../../lib/audio/generator";
import type { AnnotationEntry, ScorePosition } from "../../types";

export interface UseScoreInteraction {
  interactionMode: "edit" | "select";
  setInteractionMode: React.Dispatch<React.SetStateAction<"edit" | "select">>;
  selectionMode: "none" | "note" | "staff";
  selectedIds: string[];
  latestPosition: ScorePosition | undefined;
  handleSelectionChange: (
    mode: "none" | "note" | "staff",
    ids: string[],
    position?: ScorePosition,
  ) => void;
  contextMenu: {
    x: number;
    y: number;
    step: "menu" | "input";
    activeMenuId?: string;
    inputValue: string;
  } | null;
  handleContextMenu: (x: number, y: number) => void;
  closeContextMenu: () => void;
  handleMenuSelect: (menuId: string) => void;
  handleContextMenuInputChange: (value: string) => void;
  handleAnnotationSubmit: () => void;
}

export function useScoreInteraction(
  meiXML: Document | undefined,
  activeTabPath: string | null,
  onAddAnnotation: (path: string, annotation: AnnotationEntry) => void,
): UseScoreInteraction {
  const [interactionMode, setInteractionMode] = useState<"select" | "edit">(
    "select",
  );
  const [selectionMode, setSelectionMode] = useState<"none" | "note" | "staff">(
    "none",
  );
  const [selectedIds, setSelectedIds] = useState<string[]>([]);
  const prevSelectedIdsRef = useRef<string[]>([]);
  const [latestPosition, setLatestPosition] = useState<
    ScorePosition | undefined
  >();

  // Context Menu State
  const [contextMenu, setContextMenu] = useState<{
    x: number;
    y: number;
    step: "menu" | "input";
    activeMenuId?: string;
    inputValue: string;
  } | null>(null);

  const { performer } = useAudioContext();

  // Selection Handler
  const handleSelectionChange = useCallback(
    (
      mode: "none" | "note" | "staff",
      ids: string[],
      position?: ScorePosition,
    ) => {
      setSelectionMode(mode);
      setSelectedIds(ids);
      setLatestPosition(position);
      setContextMenu(null);

      if (mode === "note" && meiXML) {
        const newIds = ids.filter(
          (id) => !prevSelectedIdsRef.current.includes(id),
        );
        if (newIds.length > 0) {
          const audioData = generateAudioForNote(meiXML, newIds[0]);
          performer.play(audioData);
        }
      }
      prevSelectedIdsRef.current = ids;
    },
    [performer, meiXML],
  );

  // Context Menu Handlers
  const handleContextMenu = useCallback((x: number, y: number) => {
    setContextMenu({
      x,
      y,
      step: "menu",
      inputValue: "",
    });
  }, []);

  const closeContextMenu = useCallback(() => {
    setContextMenu(null);
  }, []);

  const handleMenuSelect = (menuId: string) => {
    setContextMenu((prev) =>
      prev ? { ...prev, step: "input", activeMenuId: menuId } : null,
    );
  };

  const handleContextMenuInputChange = (value: string) => {
    setContextMenu((prev) => (prev ? { ...prev, inputValue: value } : null));
  };

  const handleAnnotationSubmit = () => {
    if (contextMenu?.activeMenuId && contextMenu.inputValue && activeTabPath) {
      const newEntry: AnnotationEntry = {
        id: crypto.randomUUID(),
        menuId: contextMenu.activeMenuId,
        inputValue: contextMenu.inputValue,
        selectedIds: [...selectedIds],
        latestPosition: latestPosition,
        createdAt: Date.now(),
      };

      onAddAnnotation(activeTabPath, newEntry);
      closeContextMenu();
    }
  };

  // Close context menu on global click (outside)
  useEffect(() => {
    const handleClickOutside = () => closeContextMenu();
    if (contextMenu) {
      document.addEventListener("click", handleClickOutside);
    }
    return () => {
      document.removeEventListener("click", handleClickOutside);
    };
  }, [contextMenu, closeContextMenu]);

  return {
    interactionMode,
    setInteractionMode,
    selectionMode,
    selectedIds,
    latestPosition,
    handleSelectionChange,
    contextMenu,
    handleContextMenu,
    closeContextMenu,
    handleMenuSelect,
    handleContextMenuInputChange,
    handleAnnotationSubmit,
  };
}
