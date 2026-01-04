import type React from "react";
import { createContext, useCallback, useContext, useState } from "react";
import { apiClient } from "../../lib/api/client";
import type { PartData } from "../../lib/audio/performer";
import type { ScoreEntry } from "../../types";
import { useNotification } from "../useNotification";

interface ApiContextValue {
  isConnected: boolean;
  getScoreList: () => Promise<ScoreEntry[]>;
  getPerformanceData: (meiXML: Document) => Promise<PartData[]>;
  convertPartwise: (meiXML: Document) => Promise<string>;
}

const ApiContext = createContext<ApiContextValue | null>(null);

export const ApiProvider: React.FC<{ children: React.ReactNode }> = ({
  children,
}) => {
  const [isConnected, setIsConnected] = useState(true);
  const { notify } = useNotification();

  const handleRequest = useCallback(
    async <T,>(
      request: () => Promise<T>,
      successMsg?: string,
      errorMsgPrefix?: string,
      apiName?: string,
    ): Promise<T> => {
      try {
        const result = await request();
        if (!isConnected) setIsConnected(true);
        if (successMsg) notify(successMsg, "info", apiName || "API");
        return result;
      } catch (error) {
        setIsConnected(false);
        const msg = error instanceof Error ? error.message : String(error);
        if (errorMsgPrefix) {
          notify(`${errorMsgPrefix}: ${msg}`, "error", apiName || "API");
        }
        throw error;
      }
    },
    [isConnected, notify],
  );

  const getScoreList = useCallback(async () => {
    return handleRequest(
      () => apiClient.getScoreList(),
      undefined, // Don't notify on every list fetch success to avoid spam, usually handled by caller if needed
      "Failed to fetch score list",
      "API /score",
    );
  }, [handleRequest]);

  const getPerformanceData = useCallback(
    async (meiXML: Document) => {
      return handleRequest(
        () => apiClient.getPerformanceData(meiXML),
        undefined,
        "Performance data generation failed",
        "API /perform",
      );
    },
    [handleRequest],
  );

  const convertPartwise = useCallback(
    async (meiXML: Document) => {
      return handleRequest(
        () => apiClient.convertPartwise(meiXML),
        "Partwise conversion successful",
        "Partwise conversion failed",
        "API /partwise",
      );
    },
    [handleRequest],
  );

  return (
    <ApiContext.Provider
      value={{ isConnected, getScoreList, getPerformanceData, convertPartwise }}
    >
      {children}
    </ApiContext.Provider>
  );
};

export function useApi() {
  const context = useContext(ApiContext);
  if (!context) {
    throw new Error("useApi must be used within an ApiProvider");
  }
  return context;
}
