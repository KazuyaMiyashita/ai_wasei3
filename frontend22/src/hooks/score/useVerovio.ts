import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import verovio, { type EditorAction } from "verovio";
import { useNotification } from "../useNotification";

interface UseVerovioOptions {
  scale?: number;
}

export interface UseVerovio {
  loading: boolean;
  toolkitReady: boolean;
  loadData: (data: string) => void;
  renderPageToSVG: () => string;
  edit: (editorAction: EditorAction) => { success: boolean; message?: string };
  verovioToolkit: React.MutableRefObject<verovio.toolkit | null>;
  layoutVersion: number;
}

export const useVerovio = (options: UseVerovioOptions = {}): UseVerovio => {
  const [toolkitReady, setToolkitReady] = useState(false);
  const [loading, setLoading] = useState(true);
  const verovioToolkit = useRef<verovio.toolkit | null>(null);
  const { notify } = useNotification();

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
        notify("Verovio toolkit initialized", "info", "Verovio");
      } catch (e) {
        console.error("Failed to create verovio toolkit", e);
        const msg = e instanceof Error ? e.message : String(e);
        notify(`Failed to create verovio toolkit: ${msg}`, "error", "Verovio");
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
        } catch (_e) {
          // Ignore initial check failure, wait for callback
        }
      } catch (error) {
        console.error("Error initializing Verovio:", error);
        const msg = error instanceof Error ? error.message : String(error);
        notify(`Error initializing Verovio: ${msg}`, "error", "Verovio");
        if (mounted) setLoading(false);
      }
    };

    initVerovio();

    return () => {
      mounted = false;
      verovioToolkit.current = null;
    };
  }, [notify]);

  const [layoutVersion, setLayoutVersion] = useState(0);

  useEffect(() => {
    if (verovioToolkit.current && toolkitReady) {
      // 共通化したオプションを使用
      verovioToolkit.current.setOptions(verovioOptions);

      try {
        verovioToolkit.current.redoLayout();
        setLayoutVersion((v) => v + 1);
      } catch (_e) {
        // console.warn("redoLayout failed", e);
      }
    }
  }, [verovioOptions, toolkitReady]);

  const loadData = useCallback(
    (data: string) => {
      if (verovioToolkit.current) {
        try {
          verovioToolkit.current.loadData(data);
          setLayoutVersion((v) => v + 1);
        } catch (e) {
          console.error("Error loading data to Verovio", e);
          const msg = e instanceof Error ? e.message : String(e);
          notify(`Error loading data to Verovio: ${msg}`, "error", "Verovio");
        }
      }
    },
    [notify],
  );

  const renderPageToSVG = useCallback(() => {
    if (verovioToolkit.current) {
      try {
        return verovioToolkit.current.renderToSVG(1);
      } catch (e) {
        console.error("Error rendering page", e);
        const msg = e instanceof Error ? e.message : String(e);
        notify(`Error rendering page: ${msg}`, "error", "Verovio");
      }
    }
    return "";
  }, [notify]);

  const edit = useCallback(
    (editorAction: EditorAction): { success: boolean; message?: string } => {
      if (!verovioToolkit.current) return { success: false };

      let warningDetected = false;
      let lastWarningMsg = "";
      let backupMei = "";

      // 0. Backup current MEI state
      try {
        backupMei = verovioToolkit.current.getMEI({});
      } catch (e) {
        console.error("Failed to backup MEI before edit", e);
        notify("Edit preparation failed", "error", "Verovio");
        return { success: false, message: "Backup failed" };
      }

      // 1. Save original console functions
      const originalConsoleLog = console.log;
      const originalConsoleWarn = console.warn;

      // 2. Define hook to catch Verovio warnings
      // biome-ignore lint/suspicious/noExplicitAny: console.log, console warn has type: (...data: any[]): void
      const checkLog = (...args: any[]) => {
        const msg = args.join(" ");
        if (msg.includes("[Warning]") || msg.includes("Unsupported data")) {
          warningDetected = true;
          lastWarningMsg = msg;
        }
        // Optionally pass through to original for debugging
        // originalConsoleLog.apply(console, args);
      };

      // 3. Hijack console
      console.log = checkLog;
      console.warn = checkLog;

      try {
        // 4. Execute edit
        const result = verovioToolkit.current.edit(editorAction);

        // 5. Restore console
        console.log = originalConsoleLog;
        console.warn = originalConsoleWarn;

        // 6. Check for warnings
        if (warningDetected) {
          notify(
            `Invalid edit detected by Verovio: ${lastWarningMsg}`,
            "warn",
            "Verovio",
          );

          // Restore from backup
          if (backupMei) {
            verovioToolkit.current.loadData(backupMei);
            // redoLayout might be needed after loadData, but loadData usually handles it or invalidates layout
            verovioToolkit.current.redoLayout();
            setLayoutVersion((v) => v + 1);
          }

          return { success: false, message: lastWarningMsg };
        }

        if (result) {
          // Explicitly commit if successful and no warnings
          verovioToolkit.current.edit({ action: "commit" });

          setLayoutVersion((v) => v + 1);
          notify("Edit successful", "info", "Verovio");
          return { success: true };
        }

        notify("Edit failure (unknown reason)", "warn", "Verovio");
        return { success: false, message: "Unknown reason" };
      } catch (e) {
        // Recovery in case of exception
        console.log = originalConsoleLog;
        console.warn = originalConsoleWarn;
        const msg = e instanceof Error ? e.message : String(e);
        notify(`Error editing score: ${msg}`, "error", "Verovio");

        // Restore from backup
        if (backupMei) {
          try {
            verovioToolkit.current.loadData(backupMei);
            verovioToolkit.current.redoLayout();
            setLayoutVersion((v) => v + 1);
          } catch (restoreError) {
            notify(
              `Failed to restore MEI backup. ${restoreError}`,
              "error",
              "Verovio",
            );
          }
        }
        return { success: false, message: msg };
      }
    },
    [notify],
  );

  return {
    loading,
    toolkitReady,
    loadData,
    renderPageToSVG,
    edit,
    verovioToolkit,
    layoutVersion,
  };
};
