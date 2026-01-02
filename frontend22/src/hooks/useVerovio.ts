import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import verovio from "verovio";

interface UseVerovioOptions {
  scale?: number;
}

export const useVerovio = (options: UseVerovioOptions = {}) => {
  const [toolkitReady, setToolkitReady] = useState(false);
  const [loading, setLoading] = useState(true);
  const verovioToolkit = useRef<verovio.toolkit | null>(null);

  const { scale } = options;

  const verovioScaleFactor = 0.5; // UI上で100%と表示しているスケールをVerovioに渡す際に変換する

  const verovioOptions: verovio.VerovioOptions = useMemo(
    () => ({
      scale: (scale ?? 100) * verovioScaleFactor,
      adjustPageHeight: true,
      breaks: "none",
      header: "none",
      footer: "none",
      font: "Bravura",
    }),
    [scale],
  );

  // ツールキットの初期化。初回のみ実行する
  useEffect(() => {
    let mounted = true;
    setLoading(true);

    const setupToolkit = () => {
      if (!mounted) return;
      try {
        verovioToolkit.current = new verovio.toolkit();
        setToolkitReady(true);
      } catch (e) {
        console.error("Failed to create verovio toolkit", e);
      } finally {
        if (mounted) setLoading(false);
      }
    };

    const initVerovio = async () => {
      try {
        if (verovio.module)
          verovio.module.onRuntimeInitialized = () => setupToolkit();

        try {
          const tempToolkit = new verovio.toolkit();
          if (tempToolkit) {
            setupToolkit();
            return;
          }
        } catch (_e) {}
      } catch (error) {
        console.error("Error initializing Verovio:", error);
        if (mounted) setLoading(false);
      }
    };

    initVerovio();

    return () => {
      mounted = false;
      verovioToolkit.current = null;
    };
  }, []);

  const [layoutVersion, setLayoutVersion] = useState(0);

  useEffect(() => {
    if (verovioToolkit.current && toolkitReady) {
      // 共通化したオプションを使用
      verovioToolkit.current.setOptions(verovioOptions);

      try {
        verovioToolkit.current.redoLayout();
        setLayoutVersion((v) => v + 1);
      } catch (_e) {
        // Data might not be loaded yet
      }
    }
  }, [verovioOptions, toolkitReady]);

  const loadData = useCallback((data: string) => {
    if (verovioToolkit.current) {
      verovioToolkit.current.loadData(data);
      setLayoutVersion((v) => v + 1);
    }
  }, []);

  const renderPageToSVG = useCallback(() => {
    if (verovioToolkit.current) {
      try {
        return verovioToolkit.current.renderToSVG(1);
      } catch (e) {
        console.error("Error rendering page", e);
      }
    }
    return "";
  }, []);

  return {
    loading,
    toolkitReady,
    loadData,
    renderPageToSVG,
    verovioToolkit,
    layoutVersion,
  };
};
