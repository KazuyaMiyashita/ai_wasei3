import verovio, { type EditorAction } from "verovio";
import type { Logger } from "../infrastructure/logger";
import { Subscribable } from "../shared/subscribable";

export interface ScoreState {
  loading: boolean;
  toolkitReady: boolean;
  layoutVersion: number;
}

export class ScoreRenderer extends Subscribable<ScoreState> {
  private toolkit: verovio.toolkit | null = null;
  private _loading = true;
  private _toolkitReady = false;
  private _layoutVersion = 0;
  private logger: Logger;
  private currentScale = 100;
  private readonly verovioScaleFactor = 0.5;

  constructor(logger: Logger) {
    super();
    this.logger = logger;
    this.updateState();
  }

  async init() {
    this._loading = true;
    this.updateState();

    const setupToolkit = () => {
      try {
        this.toolkit = new verovio.toolkit();
        this._toolkitReady = true;
        this.logger.notify("Verovio toolkit initialized", "info", "Verovio");
        this.updateOptions();
      } catch (e) {
        console.error("Failed to create verovio toolkit", e);
        this.logger.notify(
          `Failed to create verovio toolkit: ${e}`,
          "error",
          "Verovio",
        );
      } finally {
        this._loading = false;
        this.updateState();
      }
    };

    if (verovio.module) {
      verovio.module.onRuntimeInitialized = setupToolkit;
    }

    try {
      // biome-ignore lint/suspicious/noExplicitAny: Check module
      if ((verovio.module as any)?.asm) {
        setupToolkit();
        return;
      }
      const temp = new verovio.toolkit();
      if (temp) {
        setupToolkit();
      }
    } catch (_) {
      // Ignore
    }
  }

  setScale(scale: number) {
    this.currentScale = scale;
    if (this._toolkitReady) {
      this.updateOptions();
      this.redoLayout();
    }
  }

  private updateOptions() {
    if (!this.toolkit) return;
    const options: verovio.VerovioOptions = {
      scale: (this.currentScale ?? 100) * this.verovioScaleFactor,
      adjustPageHeight: true,
      svgViewBox: true,
      breaks: "encoded",
      breaksSmartSb: 0.0,
      header: "none",
      footer: "none",
      font: "Bravura",
    };
    this.toolkit.setOptions(options);
  }

  redoLayout() {
    if (this.toolkit && this._toolkitReady) {
      try {
        this.toolkit.redoLayout();
        this._layoutVersion++;
        this.updateState();
      } catch (e) {
        console.warn("redoLayout failed", e);
      }
    }
  }

  loadData(data: string) {
    if (this.toolkit) {
      try {
        this.toolkit.loadData(data);
        this._layoutVersion++;
        this.updateState();
      } catch (e) {
        console.error("Error loading data to Verovio", e);
        this.logger.notify(`Error loading data: ${e}`, "error", "Verovio");
      }
    }
  }

  edit(editorAction: EditorAction): { success: boolean; message?: string } {
    if (!this.toolkit)
      return { success: false, message: "Toolkit not initialized" };

    let warningDetected = false;
    let lastWarningMsg = "";
    let backupMei = "";

    // 0. Backup current MEI state
    try {
      backupMei = this.toolkit.getMEI({});
    } catch (e) {
      console.error("Failed to backup MEI before edit", e);
      this.logger.notify("Edit preparation failed", "error", "Verovio");
      return { success: false, message: "Backup failed" };
    }

    // 1. Save original console functions
    const originalConsoleLog = console.log;
    const originalConsoleWarn = console.warn;

    // 2. Define hook to catch Verovio warnings
    // biome-ignore lint/suspicious/noExplicitAny: console hijack
    const checkLog = (...args: any[]) => {
      const msg = args.join(" ");
      if (msg.includes("[Warning]") || msg.includes("Unsupported data")) {
        warningDetected = true;
        lastWarningMsg = msg;
      }
    };

    // 3. Hijack console
    console.log = checkLog;
    console.warn = checkLog;

    try {
      // 4. Execute edit
      const result = this.toolkit.edit(editorAction);

      // 5. Restore console
      console.log = originalConsoleLog;
      console.warn = originalConsoleWarn;

      // 6. Check for warnings
      if (warningDetected) {
        this.logger.notify(
          `Invalid edit detected by Verovio: ${lastWarningMsg}`,
          "warn",
          "Verovio",
        );

        // Restore from backup
        if (backupMei) {
          this.toolkit.loadData(backupMei);
          this.toolkit.redoLayout();
          this._layoutVersion++;
          this.updateState();
        }

        return { success: false, message: lastWarningMsg };
      }

      if (result) {
        // Explicitly commit if successful and no warnings
        this.toolkit.edit({ action: "commit" });

        this._layoutVersion++;
        this.updateState();
        this.logger.notify("Edit successful", "info", "Verovio");
        return { success: true };
      }

      this.logger.notify("Edit failure (unknown reason)", "warn", "Verovio");
      return { success: false, message: "Unknown reason" };
    } catch (e) {
      // Recovery in case of exception
      console.log = originalConsoleLog;
      console.warn = originalConsoleWarn;
      const msg = e instanceof Error ? e.message : String(e);
      this.logger.notify(`Error editing score: ${msg}`, "error", "Verovio");

      // Restore from backup
      if (backupMei) {
        try {
          this.toolkit.loadData(backupMei);
          this.toolkit.redoLayout();
          this._layoutVersion++;
          this.updateState();
        } catch (restoreError) {
          this.logger.notify(
            `Failed to restore MEI backup. ${restoreError}`,
            "error",
            "Verovio",
          );
        }
      }
      return { success: false, message: msg };
    }
  }

  renderPageToSVG(page = 1): string {
    if (this.toolkit) {
      try {
        return this.toolkit.renderToSVG(page);
      } catch (e) {
        console.error("Error rendering page", e);
      }
    }
    return "";
  }

  getToolkit(): verovio.toolkit | null {
    return this.toolkit;
  }

  private updateState() {
    this.emit({
      loading: this._loading,
      toolkitReady: this._toolkitReady,
      layoutVersion: this._layoutVersion,
    });
  }
}
