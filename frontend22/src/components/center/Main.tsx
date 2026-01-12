import { Loader2 } from "lucide-react";
import { useEffect } from "react";
import { Group, Panel } from "react-resizable-panels";
import {
  useApplication,
  useApplicationState,
} from "../../context/ApplicationContext";
import { useScoreRenderer } from "../../hooks/score/useScoreRenderer";
import { CodeView } from "./CodeView";
import { DocumentView } from "./DocumentView";

export function Main() {
  const application = useApplication();
  // We need to resolve the document object from ID.
  const activeDocument = useApplicationState((state) =>
    state.currentDocumentId
      ? state.activeDocuments.get(state.currentDocumentId)
      : null,
  );

  const viewMode = useApplicationState((state) => state.viewState.viewMode);

  const {
    renderedContent,
    isReady,
    containerRef,
    handleClick,
    handleContextMenu,
  } = useScoreRenderer();

  // Global Keyboard Shortcuts
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if ((e.metaKey || e.ctrlKey) && e.key.toLowerCase() === "s") {
        if (!activeDocument) return;

        if (e.defaultPrevented) return;

        e.preventDefault();
        if (e.shiftKey) {
          const defaultName =
            activeDocument.originalDocument.path.split("/").pop() ||
            "new_file.mei";
          const name = prompt("Save as (filename):", defaultName);
          if (name) {
            const path = name.startsWith("/") ? name : `/${name}`;
            void application.saveAs(path);
          }
        } else {
          void application.save();
        }
      }
    };

    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [activeDocument, application]);

  if (!activeDocument) {
    return (
      <main className="relative flex flex-1 items-center justify-center bg-gray-100 text-gray-400">
        <div className="text-center">
          <p className="text-lg font-medium">No document open</p>
          <p className="mt-2 text-sm">
            Open a file from the Workspace to start editing.
          </p>
        </div>
      </main>
    );
  }

  if (!isReady) {
    return (
      <div className="flex h-full items-center justify-center text-gray-400">
        <Loader2 className="mr-2 animate-spin" /> Loading Verovio...
      </div>
    );
  }

  const documentView = (
    <DocumentView
      activeDocument={activeDocument}
      renderedContent={renderedContent}
      containerRef={containerRef}
      handleClick={handleClick}
      handleContextMenu={handleContextMenu}
    />
  );

  const codeView = <CodeView activeDocument={activeDocument} />;

  if (viewMode === "document") {
    return documentView;
  }
  if (viewMode === "code") {
    return codeView;
  }
  if (viewMode === "split") {
    return (
      <Group orientation="horizontal">
        <Panel
          collapsible={false}
          defaultSize={"50%"} // In version 4.3.0 of react-resizable-panels, specify "50%".
          className="border-ui-border hover:border-ui-border-hover border-r"
        >
          {documentView}
        </Panel>
        <Panel>{codeView}</Panel>
      </Group>
    );
  }
}

export default Main;
