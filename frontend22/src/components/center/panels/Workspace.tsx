import { useEffect, useState } from "react";
import {
  useApplication,
  useApplicationState,
} from "../../../context/ApplicationContext";
import type { ActiveDocument } from "../../../lib/editor/active-document";
import type {
  DocumentId,
  DocumentOrigin,
} from "../../../lib/model/documents/types";

function WorkspaceItem({
  file,
  activeDoc,
  isActive,
  onClick,
}: {
  file: { name: string; path: string };
  activeDoc?: ActiveDocument;
  isActive: boolean;
  onClick: () => void;
}) {
  const [isDirty, setIsDirty] = useState(
    activeDoc?.getState().isDirty || false,
  );

  useEffect(() => {
    if (!activeDoc) {
      setIsDirty(false);
      return;
    }
    setIsDirty(activeDoc.getState().isDirty);
    return activeDoc.subscribe((state) => setIsDirty(state.isDirty));
  }, [activeDoc]);

  return (
    <li className="list-none">
      <button
        type="button"
        className={`ui-action-button flex w-full items-center justify-between px-4 py-1.5 text-left ${
          isActive ? "bg-ui-bg-subtle text-brand-primary font-medium" : ""
        }`}
        onClick={onClick}
        title={file.path}
        data-state={isActive ? "on" : "off"}
      >
        <span className="flex-1 truncate">{file.name}</span>
        {isDirty && (
          <span className="text-ui-text-muted ml-2 text-[10px] font-bold">
            M
          </span>
        )}
      </button>
    </li>
  );
}

function Workspace() {
  const application = useApplication();
  const entries = useApplicationState((state) => state.workspace.entries);
  const currentDocumentId = useApplicationState(
    (state) => state.currentDocumentId,
  );
  const activeDocumentsMap = useApplicationState(
    (state) => state.activeDocuments,
  );

  const handleFileClick = async (path: string) => {
    // ID in workspace is path
    await application.open(path as DocumentId, "workspace" as DocumentOrigin);
  };

  const files = entries
    .filter((e) => e.kind === "file")
    .sort((a, b) => a.name.localeCompare(b.name));

  const normalize = (p: string | null) => {
    if (!p) return "";
    return p.startsWith("/") ? p : `/${p}`;
  };

  return (
    <div className="ui-panel">
      <div className="ui-panel-title">Workspace</div>
      <ul className="text-ui-text-main flex list-none flex-col gap-1 px-0 text-sm">
        {files.length === 0 && (
          <li className="text-ui-text-muted px-4 italic">No files</li>
        )}
        {files.map((file) => {
          const normFilePath = normalize(file.path);
          const normActiveId = normalize(currentDocumentId);
          const isActive = normFilePath === normActiveId;

          // Find from Map directly (simpler than array find)
          // We need to iterate if ID normalization is complex, but usually ID matches path.
          // Let's iterate values since normalization logic is used.
          let sessionDoc: ActiveDocument | undefined;
          for (const d of activeDocumentsMap.values()) {
            if (normalize(d.id) === normFilePath) {
              sessionDoc = d;
              break;
            }
          }

          return (
            <WorkspaceItem
              key={file.path}
              file={file}
              activeDoc={sessionDoc}
              isActive={isActive}
              onClick={() => handleFileClick(file.path)}
            />
          );
        })}
      </ul>
    </div>
  );
}

export default Workspace;
