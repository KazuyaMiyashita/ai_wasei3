import type { FileSystemInterface } from "./filesystem";

export async function getAllFilesRecursively(
  fs: FileSystemInterface,
  path: string,
): Promise<{ path: string; content: string }[]> {
  const list = await fs.ls(path);
  let results: { path: string; content: string }[] = [];
  for (const entry of list) {
    if (entry.kind === "file") {
      const content = await fs.readFile(entry.path);
      results.push({ path: entry.path, content });
    } else {
      const subFiles = await getAllFilesRecursively(fs, entry.path);
      results = [...results, ...subFiles];
    }
  }
  return results;
}
