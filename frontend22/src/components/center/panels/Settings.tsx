import { Database, FolderTree, Trash2 } from "lucide-react";
import {
  useApplication,
  useApplicationState,
} from "../../../context/ApplicationContext";

function Settings() {
  const application = useApplication();
  const mode = useApplicationState((state) => state.workspace.mode);

  return (
    <div className="ui-panel">
      <div className="ui-panel-section">
        <div className="ui-panel-title">Settings</div>
        <div className="space-y-6">
          <section>
            <h3 className="text-ui-text-muted mb-3 text-sm font-semibold tracking-wider uppercase">
              Storage Mode
            </h3>
            <div className="space-y-2">
              <button
                type="button"
                onClick={() => application.workspaceManager.switchToInternal()}
                className={`flex w-full items-center gap-3 rounded border px-3 py-2 transition-all ${
                  mode === "indexeddb"
                    ? "bg-ui-border border-ui-border-hover text-ui-text-main"
                    : "hover:bg-ui-border/50 text-ui-text-muted border-transparent"
                }`}
              >
                <Database size={18} />
                <div className="text-left">
                  <div className="font-medium">Browser Storage</div>
                  <div className="text-xs opacity-70">IndexedDB (Default)</div>
                </div>
              </button>

              <button
                type="button"
                onClick={() => application.workspaceManager.switchToNative()}
                className={`flex w-full items-center gap-3 rounded border px-3 py-2 transition-all ${
                  mode === "native"
                    ? "bg-ui-border border-ui-border-hover text-ui-text-main"
                    : "hover:bg-ui-border/50 text-ui-text-muted border-transparent"
                }`}
              >
                <FolderTree size={18} />
                <div className="text-left">
                  <div className="font-medium">Local Directory</div>
                  <div className="text-xs opacity-70">
                    Native File System API
                  </div>
                </div>
              </button>
            </div>
          </section>

          <section className="border-ui-border border-t pt-6">
            <h3 className="mb-3 text-sm font-semibold tracking-wider text-red-400 uppercase">
              Developer Options
            </h3>
            <button
              type="button"
              onClick={() => {
                if (
                  confirm("Clear all internal storage? This cannot be undone.")
                ) {
                  application.workspaceManager.clearInternal();
                }
              }}
              className="flex w-full items-center gap-3 rounded border border-red-900/30 px-3 py-2 text-red-400 transition-all hover:bg-red-900/20"
            >
              <Trash2 size={18} />
              <div className="text-left">
                <div className="font-medium">Clear Internal Storage</div>
                <div className="text-xs opacity-70">
                  Delete all files in IndexedDB
                </div>
              </div>
            </button>
          </section>
        </div>
      </div>
    </div>
  );
}

export default Settings;
