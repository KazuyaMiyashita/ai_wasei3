import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import type { ScorePosition } from "../components/ScoreDisplay";
import { generateAudioForNote } from "../lib/audio-generator";
import { usePerformanceAudio } from "./usePerformanceAudio";
import { useVerovio } from "./useVerovio";

export interface ScoreEntry {
  path: string;
  name: string;
}

export interface AnnotationEntry {
  id: string;
  menuId: string;
  inputValue: string;
  selectedIds: string[];
  latestPosition?: ScorePosition;
  createdAt: number;
}

export interface Tab {
  path: string;
  name: string;
  state: "semi-open" | "open" | "editing";
  annotations: AnnotationEntry[];
}

export function useScoreEditor() {
  const [scoreList, setScoreList] = useState<ScoreEntry[]>([]);
  const [localScores, setLocalScores] = useState<ScoreEntry[]>([]);
  const [loadingFile, setLoadingFile] = useState<boolean>(false);
  const [scale, setScale] = useState<number>(100);
  const [isPlaying, setIsPlaying] = useState(false);
  const [interactionMode, setInteractionMode] = useState<"select" | "edit">(
    "select",
  );

  // Tab State
  const [tabs, setTabs] = useState<Tab[]>([]);
  const [activeTabPath, setActiveTabPath] = useState<string | null>(null);

  const activeTab = useMemo(
    () => tabs.find((t) => t.path === activeTabPath),
    [tabs, activeTabPath],
  );

  const annotations = activeTab?.annotations || [];

  // Selection State
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

  // Audio Hook
  const { play } = usePerformanceAudio();

  // Score Data
  const [meiXML, setMeiXML] = useState<Document | undefined>();
  const [svgData, setSvgData] = useState<string>("");

  const stopPlayback = useCallback(() => setIsPlaying(false), []);

  // Tab Management Functions
  const handleFileOpen = useCallback(
    (path: string, name: string, isDoubleClick: boolean) => {
      setTabs((prev) => {
        const existingTabIndex = prev.findIndex((t) => t.path === path);

        if (existingTabIndex !== -1) {
          // Tab exists
          const existingTab = prev[existingTabIndex];
          if (isDoubleClick && existingTab.state === "semi-open") {
            // Promote to open
            const newTabs = [...prev];
            newTabs[existingTabIndex] = { ...existingTab, state: "open" };
            return newTabs;
          }
          return prev;
        }

        // Tab doesn't exist
        const newTab: Tab = {
          path,
          name,
          state: isDoubleClick ? "open" : "semi-open",
          annotations: [],
        };

        // If adding a new tab (single click), check if there is an existing semi-open tab to replace
        if (!isDoubleClick) {
          const semiOpenIndex = prev.findIndex((t) => t.state === "semi-open");
          if (semiOpenIndex !== -1) {
            const newTabs = [...prev];
            newTabs[semiOpenIndex] = newTab;
            return newTabs;
          }
        }

        return [...prev, newTab];
      });
      setActiveTabPath(path);
    },
    [],
  );

  const handleTabClose = useCallback(
    (path: string) => {
      // Find tab in current state (need to use refs or functional update logic carefully)
      // Since we need to update activeTabPath as well, we'll do it in a way that depends on current state.
      // However, to keep it simple, we'll access state directly in the closure (tabs).
      // This means we need 'tabs' and 'activeTabPath' in dependency.

      // Wait, we can't use 'tabs' in dependency of handleTabClose if we pass it to child components that memoize?
      // Actually, standard practice is to use state from closure if it changes.

      const tab = tabs.find((t) => t.path === path);
      if (!tab) return;

      if (tab.state === "editing") {
        if (
          !window.confirm(`Close ${tab.name}? Unsaved changes will be lost.`)
        ) {
          return;
        }
      }

      const newTabs = tabs.filter((t) => t.path !== path);
      setTabs(newTabs);

      if (activeTabPath === path) {
        const index = tabs.findIndex((t) => t.path === path);
        if (newTabs.length > 0) {
          const newIndex = Math.min(index, newTabs.length - 1);
          setActiveTabPath(newTabs[newIndex].path);
        } else {
          setActiveTabPath(null);
        }
      }
    },
    [tabs, activeTabPath],
  );

  const handleTabSelect = useCallback((path: string) => {
    setActiveTabPath(path);
  }, []);

  const handleTabSave = useCallback((path: string) => {
    setTabs((prev) =>
      prev.map((t) => (t.path === path ? { ...t, state: "open" } : t)),
    );
  }, []);

  // Fetch score list on mount
  useEffect(() => {
    fetch("/score")
      .then((res) => res.json())
      .then((data: ScoreEntry[]) => {
        setScoreList(data);

        // Handle initial file selection based on URL or default
        const params = new URLSearchParams(window.location.search);
        const fileParam = params.get("file");
        if (fileParam) {
          const found = data.find(
            (s) => s.path.endsWith(fileParam) || s.name === fileParam,
          );
          if (found) {
            handleFileOpen(found.path, found.name, false);
          } else if (data.length > 0) {
            // Default load
            handleFileOpen(data[0].path, data[0].name, false);
          }
        } else if (data.length > 0) {
          handleFileOpen(data[0].path, data[0].name, false);
        }
      })
      .catch((err) => console.error("Failed to fetch score list:", err));
  }, [handleFileOpen]); // Empty dependency, runs once. handleFileOpen is stable (if deps are correct or ignored).
  // Actually handleFileOpen has no deps so it's stable.

  // Sync URL with state
  useEffect(() => {
    if (!activeTabPath) return;
    const params = new URLSearchParams();
    // Only set URL for server files (not blob:)
    if (!activeTabPath.startsWith("blob:")) {
      const fileName = activeTabPath.split("/").pop();
      if (fileName) {
        params.set("file", fileName);
        const newUrl = `${window.location.pathname}?${params.toString()}`;
        window.history.replaceState(null, "", newUrl);
      }
    }
  }, [activeTabPath]);

  // Hooks
  const {
    loading: verovioLoading,
    toolkitReady,
    loadData: loadVerovioData,
    renderPageToSVG,
    layoutVersion,
  } = useVerovio({ scale });

  const togglePlayback = useCallback(() => setIsPlaying((p) => !p), []);

  // Load Data Effect
  useEffect(() => {
    if (!toolkitReady || !activeTabPath) {
      if (!activeTabPath) {
        setMeiXML(undefined);
        setSvgData("");
      }
      return;
    }

    const load = async () => {
      setLoadingFile(true);
      stopPlayback();

      try {
        const response = await fetch(activeTabPath);
        if (!response.ok) {
          throw new Error(
            `Failed to load ${activeTabPath}: ${response.statusText}`,
          );
        }
        const data = await response.text();

        const parser = new DOMParser();
        const meiXML = parser.parseFromString(data, "application/xml");

        setMeiXML(meiXML);
        loadVerovioData(data);
      } catch (error) {
        console.error("Error loading MEI data:", error);
        setLoadingFile(false);
      }
    };
    load();
  }, [activeTabPath, toolkitReady, loadVerovioData, stopPlayback]);

  // Render Page Effect
  useEffect(() => {
    if (toolkitReady && !verovioLoading) {
      void layoutVersion;

      if (loadingFile) {
        try {
          const data = renderPageToSVG();
          if (data) {
            setSvgData(data);
          }
        } finally {
          setLoadingFile(false);
        }
      } else if (svgData) {
        const data = renderPageToSVG();
        if (data) setSvgData(data);
      }
    }
  }, [
    loadingFile,
    verovioLoading,
    toolkitReady,
    renderPageToSVG,
    layoutVersion,
    svgData,
  ]);

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
          play(audioData);
        }
      }
      prevSelectedIdsRef.current = ids;
    },
    [play, meiXML],
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

      setTabs((prev) =>
        prev.map((t) => {
          if (t.path === activeTabPath) {
            return {
              ...t,
              state: "editing",
              annotations: [...t.annotations, newEntry],
            };
          }
          return t;
        }),
      );

      closeContextMenu();
    }
  };

  const handleContextMenuInputChange = (value: string) => {
    setContextMenu((prev) => (prev ? { ...prev, inputValue: value } : null));
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

  // Cleanup object URLs on unmount
  useEffect(() => {
    return () => {
      for (const score of localScores) {
        URL.revokeObjectURL(score.path);
      }
    };
  }, [localScores]);

  // Deprecated handlers kept for compatibility if needed, or we can remove them.
  // Explorer uses handleFileSelect -> we should replace this with handleFileOpen

  const handleFileSelect = (path: string) => {
    const name =
      scoreList.find((s) => s.path === path)?.name ||
      localScores.find((s) => s.path === path)?.name ||
      "Unknown";
    handleFileOpen(path, name, false);
  };

  const handleFileDoubleClick = (path: string) => {
    const name =
      scoreList.find((s) => s.path === path)?.name ||
      localScores.find((s) => s.path === path)?.name ||
      "Unknown";
    handleFileOpen(path, name, true);
  };

  const handleLocalFileAdd = (file: File) => {
    const objectUrl = URL.createObjectURL(file);
    const newEntry: ScoreEntry = {
      path: objectUrl,
      name: file.name,
    };
    setLocalScores((prev) => [...prev, newEntry]);
    handleFileOpen(objectUrl, file.name, false);
  };

  const handleZoomIn = () => {
    setScale((prev) => Math.min(prev + 5, 200));
  };

  const handleZoomOut = () => {
    setScale((prev) => Math.max(prev - 5, 10));
  };

  const handlePartwise = useCallback(async () => {
    if (!meiXML) return;

    try {
      setLoadingFile(true);
      const serializer = new XMLSerializer();
      const xmlString = serializer.serializeToString(meiXML);

      const formData = new FormData();
      const blob = new Blob([xmlString], { type: "text/xml" });
      formData.append("file", blob, "score.mei");

      const response = await fetch("/partwise", {
        method: "POST",
        body: formData,
      });

      if (!response.ok) {
        throw new Error(`Partwise conversion failed: ${response.statusText}`);
      }

      const convertedXmlString = await response.text();
      const parser = new DOMParser();
      const newMeiXML = parser.parseFromString(
        convertedXmlString,
        "application/xml",
      );

      setMeiXML(newMeiXML);
      loadVerovioData(convertedXmlString);
    } catch (error) {
      console.error("Error during partwise conversion:", error);
    } finally {
      setLoadingFile(false);
    }
  }, [meiXML, loadVerovioData]);

  const loading = verovioLoading || loadingFile;

  return {
    scoreList,
    localScores,
    currentFile: activeTabPath || "", // Compatible mapping
    tabs,
    activeTabPath,
    handleFileOpen,
    handleTabClose,
    handleTabSelect,
    handleTabSave,
    handleFileDoubleClick,

    // Legacy compatible
    handleFileSelect,
    handleLocalFileAdd,

    meiXML,
    svgData,
    scale,
    loading,
    isPlaying,
    setIsPlaying,
    togglePlayback,
    stopPlayback,
    selectionMode,
    selectedIds,
    latestPosition,
    handleSelectionChange,
    handleZoomIn,
    handleZoomOut,
    handlePartwise,
    annotations,
    setAnnotations: () => {}, // No-op, managed internally
    contextMenu,
    handleContextMenu,
    closeContextMenu,
    handleMenuSelect,
    handleAnnotationSubmit,
    handleContextMenuInputChange,
    interactionMode,
    setInteractionMode,
  };
}
