import { clear, del, get, keys, set } from "idb-keyval";

export type FileSystemEntryKind = "file" | "directory";

export interface FileSystemEntry {
  name: string;
  kind: FileSystemEntryKind;
  path: string; // The full virtual path (e.g., "/folder/file.mei")
}

export interface FileSystemInterface {
  /**
   * List entries in a directory.
   * @param path Path to the directory (e.g., "/" or "/folder")
   */
  ls(path: string): Promise<FileSystemEntry[]>;

  /**
   * Read file content as string.
   * @param path Full path to the file
   */
  readFile(path: string): Promise<string>;

  /**
   * Write content to a file.
   * @param path Full path to the file
   * @param content content string
   */
  writeFile(path: string, content: string): Promise<void>;

  /**
   * Create a new directory.
   * @param path Full path to the new directory
   */
  createDirectory(path: string): Promise<void>;

  /**
   * Delete a file or directory.
   * @param path Full path to delete
   */
  delete(path: string): Promise<void>;

  /**
   * Rename/Move a file or directory.
   * @param oldPath Current full path
   * @param newPath New full path
   */
  rename(oldPath: string, newPath: string): Promise<void>;

  /**
   * Check if the backend is read-only or supports full operations.
   */
  readonly capabilities: {
    readonly save: boolean;
    readonly delete: boolean;
    readonly rename: boolean;
  };

  /**
   * List all files in the file system recursively.
   */
  listAllFiles(): Promise<string[]>;
}

export class IndexedDBFileSystem implements FileSystemInterface {
  readonly capabilities = {
    save: true,
    delete: true,
    rename: true,
  };

  async listAllFiles(): Promise<string[]> {
    const allKeys = (await keys()) as string[];
    // Filter out potential directory markers if any (though currently we use .keep files)
    // We want actual files.
    // If we use .keep files for empty dirs, we might want to exclude them or include them?
    // For workspace listing, we usually want content files.
    // Let's return all keys for now.
    return allKeys;
  }

  async ls(path: string): Promise<FileSystemEntry[]> {
    const allKeys = (await keys()) as string[];
    const normalizedPath = path.endsWith("/") ? path : `${path}/`;
    const entries = new Set<string>();
    const result: FileSystemEntry[] = [];

    for (const key of allKeys) {
      if (key.startsWith(normalizedPath)) {
        const relative = key.slice(normalizedPath.length);
        const parts = relative.split("/");
        const name = parts[0];
        if (!name) continue;

        if (parts.length > 1) {
          // It's a directory
          if (!entries.has(name)) {
            entries.add(name);
            result.push({
              name,
              kind: "directory",
              path: `${normalizedPath}${name}`,
            });
          }
        } else {
          // It's a file
          result.push({
            name,
            kind: "file",
            path: key,
          });
        }
      }
    }

    return result;
  }

  async readFile(path: string): Promise<string> {
    const content = await get(path);
    if (content === undefined) {
      throw new Error(`File not found: ${path}`);
    }
    return content as string;
  }

  async writeFile(path: string, content: string): Promise<void> {
    await set(path, content);
  }

  async createDirectory(path: string): Promise<void> {
    // In a flat KV store like idb-keyval, directories don't strictly exist
    // until a file is placed inside. We can create a placeholder if needed,
    // but for now we'll just ensure the path is valid.
    // To "persist" an empty directory, we could store a special marker.
    const placeholder = path.endsWith("/") ? `${path}.keep` : `${path}/.keep`;
    await set(placeholder, "");
  }

  async delete(path: string): Promise<void> {
    const allKeys = (await keys()) as string[];
    for (const key of allKeys) {
      if (key === path || key.startsWith(`${path}/`)) {
        await del(key);
      }
    }
  }

  async rename(oldPath: string, newPath: string): Promise<void> {
    const allKeys = (await keys()) as string[];
    for (const key of allKeys) {
      if (key === oldPath) {
        const content = await get(key);
        await set(newPath, content);
        await del(key);
      } else if (key.startsWith(`${oldPath}/`)) {
        const relative = key.slice(oldPath.length);
        const content = await get(key);
        await set(`${newPath}${relative}`, content);
        await del(key);
      }
    }
  }

  async clearAll(): Promise<void> {
    await clear();
  }
}
