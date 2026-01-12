import { parsePerformanceData } from "../audio/parser";
import type { PartData } from "../audio/performer";

export interface ResourceEntry {
  kind: "directory" | "file";
  name: string;
  path: string; // Path relative to resource root (starts with /)
  type?: string;
}

export const apiClient = {
  async getPerformanceData(meiXML: Document): Promise<PartData[]> {
    const serializer = new XMLSerializer();
    const xmlString = serializer.serializeToString(meiXML);

    const formData = new FormData();
    const blob = new Blob([xmlString], { type: "application/xml" });
    formData.append("file", blob, "score.mei");

    const response = await fetch("/perform", {
      method: "POST",
      body: formData,
    });

    if (!response.ok) {
      const errData = await response.json().catch(() => ({}));
      throw new Error(errData.message || "演奏データの生成に失敗しました");
    }

    const text = await response.text();
    return parsePerformanceData(text);
  },

  async generateCounterpoint(
    meiXML: Document,
    limit: number,
    onFileReady: (data: {
      count: number;
      id: string;
      content_base64: string;
      filename: string;
    }) => void,
    onComplete: (data: { total: number }) => void,
    onError: (message: string) => void,
  ): Promise<void> {
    const serializer = new XMLSerializer();
    const xmlString = serializer.serializeToString(meiXML);

    const formData = new FormData();
    const blob = new Blob([xmlString], { type: "application/xml" });
    formData.append("file", blob, "source.mei");
    formData.append("limit", limit.toString());

    // Dynamic import to avoid dependency issues if not installed, or standard fetch
    const { fetchEventSource } = await import("@microsoft/fetch-event-source");

    return new Promise<void>((resolve, reject) => {
      fetchEventSource("/counterpoint", {
        method: "POST",
        body: formData,
        async onopen(response) {
          if (
            response.ok &&
            response.headers.get("content-type")?.includes("text/event-stream")
          ) {
            return; // everything is fine
          }

          // Handle error response
          let errorMessage = `Server error: ${response.status} ${response.statusText}`;
          try {
            if (
              response.headers.get("content-type")?.includes("application/json")
            ) {
              const data = await response.json();
              if (data.message) {
                errorMessage = data.message;
              }
            }
          } catch {
            // ignore JSON parse error, use default message
          }
          throw new Error(errorMessage);
        },
        onmessage(msg) {
          try {
            if (msg.event === "file_ready") {
              const data = JSON.parse(msg.data);
              onFileReady(data);
            } else if (msg.event === "complete") {
              const data = JSON.parse(msg.data);
              onComplete(data);
              resolve(); // Resolve promise when complete
            } else if (msg.event === "error") {
              const data = JSON.parse(msg.data);
              throw new Error(data.message);
            }
          } catch (e) {
            console.error("Error processing SSE message", e);
            throw e; // Will trigger onerror
          }
        },
        onerror(err) {
          const message = err instanceof Error ? err.message : String(err);
          onError(message);
          reject(err);
          throw err;
        },
        openWhenHidden: true,
      });
    });
  },

  async fetchResources(relativePath: string): Promise<ResourceEntry[]> {
    // Construct URL: /resource + relativePath
    const url = relativePath ? `/resources${relativePath}` : `/resources`;

    const res = await fetch(url);
    if (!res.ok) throw new Error("Failed to fetch");
    const text = await res.text();
    const parser = new DOMParser();
    const xml = parser.parseFromString(text, "application/xml");

    const newEntries: ResourceEntry[] = [];
    const root = xml.querySelector("resource");
    if (root) {
      root.querySelectorAll("directory").forEach((el) => {
        newEntries.push({
          kind: "directory",
          name: el.getAttribute("name") || "",
          path: el.getAttribute("path") || "",
        });
      });
      root.querySelectorAll("file").forEach((el) => {
        newEntries.push({
          kind: "file",
          name: el.getAttribute("name") || "",
          path: el.getAttribute("path") || "",
          type: el.getAttribute("type") || "",
        });
      });
    }
    return newEntries;
  },

  async fetchResourceContent(path: string): Promise<string> {
    const url = `/resources${path}`;
    const res = await fetch(url);
    if (!res.ok) throw new Error(`Failed to fetch resource: ${path}`);
    return await res.text();
  },
};
