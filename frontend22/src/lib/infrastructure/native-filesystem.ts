import type { FileSystemEntry, FileSystemInterface } from "./filesystem";

export class NativeFileSystem implements FileSystemInterface {
  readonly capabilities = {
    save: true,
    delete: true,
    rename: true, // Supported in some browsers, otherwise requires copy/delete
  };

  constructor(private root: FileSystemDirectoryHandle) {}

  private async getHandle(
    path: string,
    options: { create?: boolean; kind?: "file" | "directory" } = {},
  ): Promise<FileSystemFileHandle | FileSystemDirectoryHandle> {
    const parts = path.split("/").filter(Boolean);
    let current: FileSystemDirectoryHandle = this.root;

    for (let i = 0; i < parts.length - 1; i++) {
      current = await current.getDirectoryHandle(parts[i], {
        create: options.create,
      });
    }

    const last = parts[parts.length - 1];
    if (!last) return this.root;

    if (options.kind === "directory") {
      return await current.getDirectoryHandle(last, { create: options.create });
    } else {
      return await current.getFileHandle(last, { create: options.create });
    }
  }

  async listAllFiles(): Promise<string[]> {
    const files: string[] = [];
    const traverse = async (dir: FileSystemDirectoryHandle, path: string) => {
      // @ts-expect-error - values() is async iterable
      for await (const entry of dir.values()) {
        const entryPath = path ? `${path}/${entry.name}` : entry.name;
        if (entry.kind === "file") {
          files.push(`/${entryPath}`);
        } else {
          await traverse(entry as FileSystemDirectoryHandle, entryPath);
        }
      }
    };
    await traverse(this.root, "");
    return files;
  }

  async ls(path: string): Promise<FileSystemEntry[]> {
    const handle = (await this.getHandle(path, {
      kind: "directory",
    })) as FileSystemDirectoryHandle;
    const result: FileSystemEntry[] = [];
    const normalizedPath = path.endsWith("/") ? path : `${path}/`;
    const basePath = path === "/" ? "" : normalizedPath;

    // @ts-expect-error - values() is async iterable
    for await (const entry of handle.values()) {
      result.push({
        name: entry.name,
        kind: entry.kind === "directory" ? "directory" : "file",
        path: `${basePath}${entry.name}`,
      });
    }

    return result;
  }

  async readFile(path: string): Promise<string> {
    const handle = (await this.getHandle(path, {
      kind: "file",
    })) as FileSystemFileHandle;
    const file = await handle.getFile();
    return await file.text();
  }

  async writeFile(path: string, content: string): Promise<void> {
    const handle = (await this.getHandle(path, {
      create: true,
      kind: "file",
    })) as FileSystemFileHandle;
    const writable = await handle.createWritable();
    await writable.write(content);
    await writable.close();
  }

  async createDirectory(path: string): Promise<void> {
    await this.getHandle(path, { create: true, kind: "directory" });
  }

  async delete(path: string): Promise<void> {
    const parts = path.split("/").filter(Boolean);
    const name = parts.pop();
    if (!name) return;
    const parentPath = `/${parts.join("/")}`;
    const parent = (await this.getHandle(parentPath, {
      kind: "directory",
    })) as FileSystemDirectoryHandle;
    await parent.removeEntry(name, { recursive: true });
  }

  async rename(oldPath: string, newPath: string): Promise<void> {
    // Basic implementation: copy and delete if move is not supported natively
    const content = await this.readFile(oldPath);
    await this.writeFile(newPath, content);
    await this.delete(oldPath);
  }
}
