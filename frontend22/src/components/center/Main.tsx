import { Loader2 } from "lucide-react";
import { useEffect } from "react";
import { Group, Panel } from "react-resizable-panels";
import { useApplicationState } from "../../context/ApplicationContext";
import { useScoreRenderer } from "../../hooks/score/useScoreRenderer";
import { CodeEditor } from "./CodeEditor";
import { DocumentEditor } from "./DocumentEditor";
import { DocumentView } from "./DocumentView";

export function Main() {
  // We need to resolve the document object from ID.
  const activeDocument = useApplicationState((state) =>
    state.currentDocumentId
      ? state.activeDocuments.get(state.currentDocumentId)
      : null,
  );

  const layoutMode = useApplicationState((state) => state.viewState.layoutMode);
  const editMode = useApplicationState((state) => state.viewState.editMode);

  const {
    renderedContent,
    isReady,
    containerRef,
    handleClick,
    handleContextMenu,
  } = useScoreRenderer();

  // Global Keyboard Shortcuts
  useEffect(() => {
    // ...
  }, []);

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

  // Determine which components to show based on editMode
  const isMusicDoc =
    activeDocument.originalDocument.type === "xhtml5+mei" ||
    activeDocument.originalDocument.type === "mei";

  const showDocumentEditor = editMode && isMusicDoc;

  const documentComponent = showDocumentEditor ? (
    <DocumentEditor activeDocument={activeDocument} />
  ) : (
    <DocumentView
      activeDocument={activeDocument}
      renderedContent={renderedContent}
      containerRef={containerRef}
      handleClick={handleClick}
      handleContextMenu={handleContextMenu}
    />
  );

  const codeComponent = <CodeEditor activeDocument={activeDocument} />;

  // Early return for Loading if needed (only if NOT in document editor mode which uses its own loading)
  if (!showDocumentEditor && !isReady) {
    return (
      <div className="flex h-full items-center justify-center text-gray-400">
        <Loader2 className="mr-2 animate-spin" /> Loading Verovio...
      </div>
    );
  }

  // Layout selection
  if (layoutMode === "document") {
    return documentComponent;
  }
  if (layoutMode === "code") {
    return codeComponent;
  }
  if (layoutMode === "split") {
    return (
      <Group orientation="horizontal">
        <Panel
          collapsible={false}
          defaultSize={"50%"}
          className="border-ui-border hover:border-ui-border-hover border-r"
        >
          {documentComponent}
        </Panel>
        <Panel>{codeComponent}</Panel>
      </Group>
    );
  }
}

export default Main;
