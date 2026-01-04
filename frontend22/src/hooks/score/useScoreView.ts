import { useCallback, useEffect, useState } from "react";
import type { EditResult } from "../../lib/score/mei-edit";
import { useApi } from "../api/useApi";
import { useNotification } from "../useNotification";
import { useVerovio } from "./useVerovio";

export interface UseScoreView {
  scale: number;
  loading: boolean;
  meiXML: Document | undefined;
  svgData: string;
  handleZoomIn: () => void;
  handleZoomOut: () => void;
  handlePartwise: () => Promise<void>;
  edit: (result: EditResult) => { success: boolean; message?: string };
}

export function useScoreView(
  meiData: string | undefined,
  onUpdateScore: (newMei: string) => void,
): UseScoreView {
  const [scale, setScale] = useState<number>(100);
  const { notify } = useNotification();
  const { convertPartwise } = useApi();

  // Score Data
  const [meiXML, setMeiXML] = useState<Document | undefined>();
  const [svgData, setSvgData] = useState<string>("");

  const {
    loading: verovioLoading,
    toolkitReady,
    loadData: loadVerovioData,
    renderPageToSVG,
    edit: verovioEdit,
    verovioToolkit,
    layoutVersion,
  } = useVerovio({ scale });

  // Load Data Effect
  useEffect(() => {
    if (!toolkitReady) return;

    if (!meiData) {
      setMeiXML(undefined);
      setSvgData("");
      return;
    }

    try {
      const parser = new DOMParser();
      const doc = parser.parseFromString(meiData, "application/xml");
      setMeiXML(doc);
      loadVerovioData(meiData);
    } catch (e) {
      console.error("Failed to parse MEI", e);
      notify("Failed to parse MEI", "error");
    }
  }, [meiData, toolkitReady, loadVerovioData, notify]);

  // Edit Handler
  const edit = useCallback(
    (result: EditResult): { success: boolean; message?: string } => {
      if (result.type === "no-change") {
        notify("No changes detected", "info", "Editor");
        return { success: true };
      }

      if (result.type === "error") {
        notify(result.error || "Edit failed", "error", "Editor");
        return { success: false, message: result.error };
      }

      if (result.type === "verovio-edit") {
        const editStatus = verovioEdit(result.action);
        if (editStatus.success && verovioToolkit.current) {
          // Sync state with Verovio
          try {
            const newMei = verovioToolkit.current.getMEI({});
            // Update workspace state
            onUpdateScore(newMei);

            // Local update for responsiveness (although onUpdateScore will trigger prop change)
            const parser = new DOMParser();
            const newDoc = parser.parseFromString(newMei, "application/xml");
            setMeiXML(newDoc);
          } catch (e) {
            console.error("Failed to sync MEI after edit", e);
          }
        }
        return editStatus;
      }

      if (result.type === "full-reload") {
        try {
          loadVerovioData(result.newMei);
          const parser = new DOMParser();
          const newDoc = parser.parseFromString(
            result.newMei,
            "application/xml",
          );
          setMeiXML(newDoc);

          // Update workspace state
          onUpdateScore(result.newMei);

          notify("Score updated (structure changed)", "info", "Editor");
          return { success: true };
        } catch (e) {
          const msg = e instanceof Error ? e.message : String(e);
          console.error("Full reload failed", e);
          notify("Failed to update score structure", "error", "Editor");
          return { success: false, message: msg };
        }
      }

      return { success: false, message: "Invalid EditResult type" };
    },
    [verovioEdit, verovioToolkit, loadVerovioData, notify, onUpdateScore],
  );

  // Render Page Effect
  useEffect(() => {
    if (toolkitReady && !verovioLoading) {
      void layoutVersion; // Dependency to trigger re-render

      const data = renderPageToSVG();
      if (data) setSvgData(data);
    }
  }, [verovioLoading, toolkitReady, renderPageToSVG, layoutVersion]);
  const handleZoomIn = useCallback(() => {
    setScale((prev) => Math.min(prev + 5, 200));
  }, []);

  const handleZoomOut = useCallback(() => {
    setScale((prev) => Math.max(prev - 5, 10));
  }, []);

  const handlePartwise = useCallback(async () => {
    if (!meiXML) return;

    try {
      const convertedXmlString = await convertPartwise(meiXML);

      // Update workspace state
      onUpdateScore(convertedXmlString);

      // Verovio reload will happen via effect when prop changes
      // But we might want immediate feedback?
      // The prop change is fast enough usually.
    } catch (error) {
      console.error("Error during partwise conversion:", error);
    }
  }, [meiXML, convertPartwise, onUpdateScore]);

  return {
    scale,
    loading: verovioLoading,
    meiXML,
    svgData,
    handleZoomIn,
    handleZoomOut,
    handlePartwise,
    edit,
  };
}
