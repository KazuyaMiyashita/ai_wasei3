import { useCallback, useEffect, useMemo, useState } from "react";
import {
  clearWorkspaceDB,
  getScoreFromWorkspace,
  getWorkspaceState,
  listWorkspaceScores,
  saveScoreToWorkspace,
  saveWorkspaceState,
} from "../../lib/workspace/workspace-storage";
import type { AnnotationEntry, ScoreEntry, Tab } from "../../types";
import { useApi } from "../api/useApi";
import { useConfirm } from "../useConfirm";
import { useNotification } from "../useNotification";

export interface UseWorkspace {
  scoreList: ScoreEntry[];
  localScores: ScoreEntry[];
  tabs: Tab[];
  activeTab: Tab | undefined;
  activeTabPath: string | null;
  handleTabClose: (path: string) => void;
  handleTabSelect: (path: string) => void;
  handleTabSave: (path: string) => void;
  handleFileSelect: (path: string) => void;
  handleFileDoubleClick: (path: string) => void;
  handleLocalFileAdd: (file: File) => void;
  // Editing
  updateScore: (path: string, newMei: string) => void;
  addAnnotation: (path: string, annotation: AnnotationEntry) => void;
  undo: () => void;
  redo: () => void;
  canUndo: boolean;
  canRedo: boolean;
  // Debug
  clearWorkspace: () => void;
}

export function useWorkspace(): UseWorkspace {
  const [scoreList, setScoreList] = useState<ScoreEntry[]>([]);
  const [localScores, setLocalScores] = useState<ScoreEntry[]>([]);
  const { notify } = useNotification();
  const { getScoreList } = useApi();
  const { confirm } = useConfirm();

  const [tabs, setTabs] = useState<Tab[]>([]);
  const [activeTabPath, setActiveTabPath] = useState<string | null>(null);
  const [isInitialized, setIsInitialized] = useState(false);

  // Load Initial State from IndexedDB
  useEffect(() => {
    const init = async () => {
      try {
        // Load Workspace State (Tabs)
        const state = await getWorkspaceState();
        if (state) {
          setTabs(
            state.tabs.map((t) => ({
              ...t,
              isDirty: false, // Reset dirty on reload
              history: [],
              future: [],
            })),
          );
          setActiveTabPath(state.activeTabPath);
        }

        // Load Local Scores
        const scores = await listWorkspaceScores();
        setLocalScores(
          scores.map((s) => ({
            path: s.path,
            name: s.name,
          })),
        );
      } catch (e) {
        console.error("Failed to load workspace from DB", e);
      } finally {
        setIsInitialized(true);
      }
    };
    init();
  }, []);

  // Persist Workspace State (Tabs)
  useEffect(() => {
    if (!isInitialized) return;
    saveWorkspaceState({
      tabs,
      activeTabPath,
    }).catch((e) => console.error("Failed to save workspace state", e));
  }, [tabs, activeTabPath, isInitialized]);

  // Load Score List (Server)
  useEffect(() => {
    getScoreList()
      .then((data) => {
        setScoreList(data);
      })
      .catch((e) => console.error("Failed to fetch server scores", e));
  }, [getScoreList]);

  const activeTab = useMemo(
    () => tabs.find((t) => t.path === activeTabPath),
    [tabs, activeTabPath],
  );

  const loadFileContent = useCallback(
    async (path: string): Promise<string> => {
      // 1. Check Workspace DB
      try {
        const local = await getScoreFromWorkspace(path);
        if (local) return local.content;
      } catch (e) {
        console.warn("DB read error", e);
      }

      // 2. Fetch from URL (Server or Blob)
      try {
        const res = await fetch(path);
        if (!res.ok) throw new Error("Failed to fetch");
        return await res.text();
      } catch (e) {
        console.error(e);
        notify(`Failed to load ${path}`, "error");
        return "";
      }
    },
    [notify],
  );

  const handleFileOpen = useCallback(
    async (path: string, name: string, isDoubleClick: boolean) => {
      const existingTab = tabs.find((t) => t.path === path);

      if (existingTab) {
        if (isDoubleClick && existingTab.state === "semi-open") {
          setTabs((prev) =>
            prev.map((t) => (t.path === path ? { ...t, state: "open" } : t)),
          );
        }
        setActiveTabPath(path);
        return;
      }

      const content = await loadFileContent(path);

      const newTab: Tab = {
        path,
        name,
        state: isDoubleClick ? "open" : "semi-open",
        isDirty: false,
        current: {
          mei: content,
          annotations: [],
        },
        history: [],
        future: [],
      };

      setTabs((prev) => {
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
    [tabs, loadFileContent],
  );

  // Initial Open Logic (Moved AFTER handleFileOpen)
  useEffect(() => {
    if (!isInitialized) return;
    if (tabs.length === 0) {
      const params = new URLSearchParams(window.location.search);
      const fileParam = params.get("file");
      if (fileParam) {
        // Try to find in local scores first, then server
        const localFound = localScores.find(
          (s) => s.path.endsWith(fileParam) || s.name === fileParam,
        );
        if (localFound) {
          handleFileOpen(localFound.path, localFound.name, false);
          return;
        }

        const serverFound = scoreList.find(
          (s) => s.path.endsWith(fileParam) || s.name === fileParam,
        );
        if (serverFound) {
          handleFileOpen(serverFound.path, serverFound.name, false);
        }
      } else if (scoreList.length > 0) {
        // Default open first server score if workspace is empty
        handleFileOpen(scoreList[0].path, scoreList[0].name, false);
      }
    }
  }, [isInitialized, scoreList, localScores, tabs.length, handleFileOpen]);

  const handleTabSave = useCallback(
    async (path: string) => {
      const tab = tabs.find((t) => t.path === path);
      if (!tab) return;

      try {
        // Save to IndexedDB
        await saveScoreToWorkspace(path, tab.name, tab.current);

        setTabs((prev) =>
          prev.map((t) => {
            if (t.path === path) {
              return { ...t, isDirty: false, state: "open" };
            }
            return t;
          }),
        );

        // Refresh local scores list
        const scores = await listWorkspaceScores();
        setLocalScores(
          scores.map((s) => ({
            path: s.path,
            name: s.name,
          })),
        );

        notify("Saved to Workspace", "info");
      } catch (e) {
        console.error("Save failed", e);
        notify("Failed to save to workspace", "error");
      }
    },
    [tabs, notify],
  );

  const handleTabClose = useCallback(
    async (path: string) => {
      const tab = tabs.find((t) => t.path === path);
      if (!tab) return;

      if (tab.isDirty) {
        const result = await confirm({
          title: "Unsaved Changes",
          message: `Save changes to "${tab.name}" before closing?`,
          confirmLabel: "Save",
          cancelLabel: "Cancel",
          discardLabel: "Discard Changes",
          variant: "warning",
        });

        if (result === "cancel") return;
        if (result === "confirm") {
          await handleTabSave(path);
        }
        // if discard, proceed
      }

      setTabs((prev) => {
        return prev.filter((t) => t.path !== path);
      });

      if (activeTabPath === path) {
        const newTabs = tabs.filter((t) => t.path !== path);
        if (newTabs.length > 0) {
          const index = tabs.findIndex((t) => t.path === path);
          const newIndex = Math.min(index, newTabs.length - 1);
          setActiveTabPath(newTabs[newIndex].path);
        } else {
          setActiveTabPath(null);
        }
      }
    },
    [tabs, activeTabPath, handleTabSave, confirm],
  );

  const handleTabSelect = useCallback((path: string) => {
    setActiveTabPath(path);
  }, []);

  const updateScore = useCallback((path: string, newMei: string) => {
    setTabs((prev) =>
      prev.map((t) => {
        if (t.path !== path) return t;
        if (t.current.mei === newMei) return t;

        return {
          ...t,
          isDirty: true,
          history: [...t.history, t.current],
          current: { ...t.current, mei: newMei },
          future: [],
          state: "editing",
        };
      }),
    );
  }, []);

  const addAnnotation = useCallback(
    (path: string, annotation: AnnotationEntry) => {
      setTabs((prev) =>
        prev.map((t) => {
          if (t.path !== path) return t;

          const newAnnotations = [...t.current.annotations, annotation];
          return {
            ...t,
            isDirty: true,
            history: [...t.history, t.current],
            current: { ...t.current, annotations: newAnnotations },
            future: [],
            state: "editing",
          };
        }),
      );
    },
    [],
  );

  const undo = useCallback(() => {
    if (!activeTabPath) return;
    setTabs((prev) =>
      prev.map((t) => {
        if (t.path !== activeTabPath) return t;
        if (t.history.length === 0) return t;

        const previous = t.history[t.history.length - 1];
        const newHistory = t.history.slice(0, -1);

        return {
          ...t,
          isDirty: true,
          future: [...t.future, t.current],
          current: previous,
          history: newHistory,
        };
      }),
    );
    notify("Undone", "info");
  }, [activeTabPath, notify]);

  const redo = useCallback(() => {
    if (!activeTabPath) return;
    setTabs((prev) =>
      prev.map((t) => {
        if (t.path !== activeTabPath) return t;
        if (t.future.length === 0) return t;

        const next = t.future[t.future.length - 1];
        const newFuture = t.future.slice(0, -1);

        return {
          ...t,
          isDirty: true,
          history: [...t.history, t.current],
          current: next,
          future: newFuture,
        };
      }),
    );
    notify("Redone", "info");
  }, [activeTabPath, notify]);

  const clearWorkspace = useCallback(async () => {
    const result = await confirm({
      title: "Clear Workspace",
      message:
        "Are you sure you want to clear the workspace? All local changes will be lost.",
      confirmLabel: "Clear",
      variant: "danger",
    });

    if (result === "confirm") {
      await clearWorkspaceDB();
      setTabs([]);
      setLocalScores([]);
      setActiveTabPath(null);
      notify("Workspace cleared", "info");
    }
  }, [notify, confirm]);

  const handleLocalFileAdd = useCallback(
    async (file: File) => {
      try {
        const text = await file.text();
        const path = `local/${file.name}`; // Simple path convention

        await saveScoreToWorkspace(path, file.name, {
          mei: text,
          annotations: [],
        });

        // Refresh local scores
        const scores = await listWorkspaceScores();
        setLocalScores(
          scores.map((s) => ({
            path: s.path,
            name: s.name,
          })),
        );

        handleFileOpen(path, file.name, false);
        notify("Added to Workspace", "info");
      } catch (e) {
        console.error("Failed to add local file", e);
        notify("Failed to add file", "error");
      }
    },
    [handleFileOpen, notify],
  );

  // Sync URL
  useEffect(() => {
    if (!activeTabPath) return;
    const params = new URLSearchParams();
    if (
      !activeTabPath.startsWith("blob:") &&
      !activeTabPath.startsWith("local/")
    ) {
      const fileName = activeTabPath.split("/").pop();
      if (fileName) {
        params.set("file", fileName);
        const newUrl = `${window.location.pathname}?${params.toString()}`;
        window.history.replaceState(null, "", newUrl);
      }
    }
  }, [activeTabPath]);

  const handleFileSelect = useCallback(
    (path: string) => {
      const name =
        scoreList.find((s) => s.path === path)?.name ||
        localScores.find((s) => s.path === path)?.name ||
        "Unknown";
      handleFileOpen(path, name, false);
    },
    [scoreList, localScores, handleFileOpen],
  );

  const handleFileDoubleClick = useCallback(
    (path: string) => {
      const name =
        scoreList.find((s) => s.path === path)?.name ||
        localScores.find((s) => s.path === path)?.name ||
        "Unknown";
      handleFileOpen(path, name, true);
    },
    [scoreList, localScores, handleFileOpen],
  );

  const canUndo = activeTab ? activeTab.history.length > 0 : false;
  const canRedo = activeTab ? activeTab.future.length > 0 : false;

  return {
    scoreList,
    localScores,
    tabs,
    activeTab,
    activeTabPath,
    handleTabClose,
    handleTabSelect,
    handleTabSave,
    handleFileSelect,
    handleFileDoubleClick,
    handleLocalFileAdd,
    updateScore,
    addAnnotation,
    undo,
    redo,
    canUndo,
    canRedo,
    clearWorkspace,
  };
}
