import type { ScoreEntry } from "../../types";
import { parsePerformanceData } from "../audio/parser";
import type { PartData } from "../audio/performer";

export const apiClient = {
  async getScoreList(): Promise<ScoreEntry[]> {
    const response = await fetch("/score");
    if (!response.ok) {
      throw new Error(`Failed to fetch score list: ${response.statusText}`);
    }
    return response.json();
  },

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

  async convertPartwise(meiXML: Document): Promise<string> {
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

    return response.text();
  },
};
