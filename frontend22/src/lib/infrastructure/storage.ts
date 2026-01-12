import { apiClient, type ResourceEntry } from "../api/client";
import type { Document, DocumentRef } from "../model/documents/document";
import type {
  DocumentId,
  DocumentPath,
  DocumentType,
} from "../model/documents/types";
import { type FileSystemInterface, IndexedDBFileSystem } from "./filesystem";

interface IStorage {
  list(): Promise<DocumentRef[]>;
  load(id: DocumentId): Promise<Document | null>;
  save(document: Document): Promise<void>;
}

export class Workspace implements IStorage {
  private fs: FileSystemInterface;

  constructor(fs?: FileSystemInterface) {
    this.fs = fs || new IndexedDBFileSystem();
  }

  static async loadWorkspace(fs?: FileSystemInterface): Promise<Workspace> {
    return new Workspace(fs);
  }

  private getTypeFromPath(path: string): DocumentType {
    if (path.endsWith(".mei")) return "mei";
    return "xhtml5+mei";
  }

  async load(id: DocumentId): Promise<Document | null> {
    const path = id as string;
    try {
      const content = await this.fs.readFile(path);
      return {
        id: id,
        path: path as DocumentPath,
        origin: "workspace",
        type: this.getTypeFromPath(path),
        content,
      };
    } catch (_e) {
      return null;
    }
  }

  async list(): Promise<DocumentRef[]> {
    const files = await this.fs.listAllFiles();
    return files.map((path) => ({
      id: path as DocumentId,
      path: path as DocumentPath,
      type: this.getTypeFromPath(path),
    }));
  }

  async save(document: Document): Promise<void> {
    await this.fs.writeFile(document.path, document.content);
  }

  async export(): Promise<{ path: string; content: string }[]> {
    const files = await this.fs.listAllFiles();
    const result: { path: string; content: string }[] = [];
    for (const path of files) {
      const content = await this.fs.readFile(path);
      result.push({ path, content });
    }
    return result;
  }

  async import(data: { path: string; content: string }[]): Promise<void> {
    for (const item of data) {
      await this.fs.writeFile(item.path, item.content);
    }
  }

  setFileSystem(fs: FileSystemInterface) {
    this.fs = fs;
  }
}

export class Resources implements IStorage {
  private getTypeFromPath(path: string): DocumentType {
    if (path.endsWith(".mei")) return "mei";
    return "xhtml5+mei";
  }

  async list(): Promise<DocumentRef[]> {
    // Currently, list() isn't well defined for a hierarchical remote resource.
    // We could fetch root, but for now we'll return empty as we use browse()
    return [];
  }

  async browse(path: string): Promise<ResourceEntry[]> {
    return apiClient.fetchResources(path);
  }

  async load(id: DocumentId): Promise<Document | null> {
    // id is expected to be the path (e.g. /Scores/foo.mei)
    // or full url /resources/Scores/foo.mei?
    // The previous implementation used /resources + path in client.
    // Let's assume ID passed here is the path relative to /resources or full path?
    // In Application.open(..., "resource"), we pass the ID.
    // Explorer previously constructed url: `/resources${entry.path}`.
    // So ID starts with `/resources`.
    // client.fetchResourceContent takes path relative to /resources (starts with /).
    // So we strip `/resources` prefix if present.

    let path = id as string;
    if (path.startsWith("/resources")) {
      path = path.substring(10);
    }

    try {
      const content = await apiClient.fetchResourceContent(path);
      return {
        id: id,
        path: path as DocumentPath, // Store relative path? Or full ID? Let's keep path as what we loaded.
        origin: "resource",
        type: this.getTypeFromPath(path),
        content,
      };
    } catch (e) {
      console.error("Failed to load resource", e);
      return null;
    }
  }

  async save(_document: Document): Promise<void> {
    throw new Error("Cannot save to remote resources directly. Use saveAs.");
  }
}
