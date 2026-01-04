import { type DBSchema, openDB } from "idb";
import type { ScoreState, Tab } from "../../types";

interface WorkspaceDB extends DBSchema {
  scores: {
    key: string; // path (unique identifier)
    value: {
      path: string;
      name: string;
      content: string; // MEI content
      // biome-ignore lint/suspicious/noExplicitAny: We will change the workspace structure later, so let's leave it as it is.
      annotations: any[];
      updatedAt: number;
    };
  };
  meta: {
    key: string;
    // biome-ignore lint/suspicious/noExplicitAny: We will change the workspace structure later, so let's leave it as it is.
    value: any;
  };
}

const DB_NAME = "wasei-workspace-db";
const DB_VERSION = 1;

async function getDB() {
  return openDB<WorkspaceDB>(DB_NAME, DB_VERSION, {
    upgrade(db) {
      if (!db.objectStoreNames.contains("scores")) {
        db.createObjectStore("scores", { keyPath: "path" });
      }
      if (!db.objectStoreNames.contains("meta")) {
        db.createObjectStore("meta", { keyPath: "key" });
      }
    },
  });
}

// Score Operations
export async function saveScoreToWorkspace(
  path: string,
  name: string,
  state: ScoreState,
) {
  const db = await getDB();
  await db.put("scores", {
    path,
    name,
    content: state.mei,
    annotations: state.annotations,
    updatedAt: Date.now(),
  });
}

export async function getScoreFromWorkspace(path: string) {
  const db = await getDB();
  return db.get("scores", path);
}

export async function listWorkspaceScores() {
  const db = await getDB();
  return db.getAll("scores");
}

export async function deleteScoreFromWorkspace(path: string) {
  const db = await getDB();
  await db.delete("scores", path);
}

// Workspace State Operations (Tabs, Active Tab)
export interface WorkspaceState {
  tabs: Tab[];
  activeTabPath: string | null;
}

export async function saveWorkspaceState(state: WorkspaceState) {
  const db = await getDB();
  // We need to strip large content from tabs before saving metadata
  // effectively storing "pointers" or lightweight tab info if the content is already in 'scores' store?
  // However, tabs might have unsaved changes (dirty state) that differ from saved 'scores'.
  // Given the requirement is to persist "editing" state (tab open/close), we should probably persist the *current* state of tabs.
  // Since we are using IndexedDB, we can afford to store the full content of open tabs here too,
  // or we can rely on 'scores' if the tab is not dirty.
  // For simplicity and robustness, let's store the full tab state in meta.
  await db.put("meta", { key: "state", value: state });
}

export async function getWorkspaceState(): Promise<WorkspaceState | undefined> {
  const db = await getDB();
  const result = await db.get("meta", "state");
  return result?.value;
}

export async function clearWorkspaceDB() {
  const db = await getDB();
  await db.clear("scores");
  await db.clear("meta");
}
