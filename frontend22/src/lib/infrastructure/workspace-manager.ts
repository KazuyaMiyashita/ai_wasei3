import { Subscribable } from "../shared/subscribable";
import {
  type FileSystemEntry,
  type FileSystemInterface,
  IndexedDBFileSystem,
} from "./filesystem";
import { getAllFilesRecursively } from "./filesystem-utils";
import { NativeFileSystem } from "./native-filesystem";

export interface WorkspaceState {
  fs: FileSystemInterface;
  entries: FileSystemEntry[];
  mode: "indexeddb" | "native";
}

export class WorkspaceManager extends Subscribable<WorkspaceState> {
  private fs: FileSystemInterface;
  private entries: FileSystemEntry[] = [];
  private mode: "indexeddb" | "native" = "indexeddb";

  constructor() {
    super();
    this.fs = new IndexedDBFileSystem();
    this.updateState();
    this.refresh();
  }

  get currentFs() {
    return this.fs;
  }

  get currentEntries() {
    return this.entries;
  }

  get currentMode() {
    return this.mode;
  }

  async refresh() {
    try {
      const result = await this.fs.ls("/");
      this.entries = result;
      this.updateState();
    } catch (error) {
      console.error("Failed to list workspace entries:", error);
    }
  }

  async switchToNative(): Promise<boolean> {
    try {
      if (typeof window !== "undefined" && !("showDirectoryPicker" in window)) {
        console.error(
          "Your browser does not support the File System Access API.",
        );
        return false;
      }

      // biome-ignore lint/suspicious/noExplicitAny: File System Access API
      const handle = await (window as any).showDirectoryPicker();
      const nativeFs = new NativeFileSystem(handle);
      this.fs = nativeFs;
      this.mode = "native";
      await this.refresh();
      // refresh calls updateState, but if refresh fails/doesn't change entries, we should update mode.
      // refresh() sets entries and updates state.
      return true;
    } catch (error) {
      console.error("Failed to switch to native workspace:", error);
      return false;
    }
  }

  async switchToInternal() {
    this.fs = new IndexedDBFileSystem();
    this.mode = "indexeddb";
    await this.refresh();
  }

  async clearInternal(): Promise<boolean> {
    const idbFs = new IndexedDBFileSystem();
    await idbFs.clearAll();
    if (this.mode === "indexeddb") {
      await this.refresh();
    }
    return true;
  }

  async exportWorkspace(): Promise<void> {
    const files = await getAllFilesRecursively(this.fs, "/");
    const data = JSON.stringify(files, null, 2);
    const blob = new Blob([data], { type: "application/json" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = `workspace_export_${new Date().toISOString().slice(0, 10)}.json`;
    a.click();
    URL.revokeObjectURL(url);
  }

  async importWorkspace(file: File): Promise<void> {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = async (e) => {
        try {
          const content = e.target?.result as string;
          const files = JSON.parse(content) as {
            path: string;
            content: string;
          }[];
          for (const f of files) {
            await this.fs.writeFile(f.path, f.content);
          }
          await this.refresh();
          resolve();
        } catch (error) {
          console.error("Import failed:", error);
          reject(error);
        }
      };
      reader.onerror = reject;
      reader.readAsText(file);
    });
  }

  private updateState() {
    this.emit({
      fs: this.fs,
      entries: [...this.entries],
      mode: this.mode,
    });
  }
}
