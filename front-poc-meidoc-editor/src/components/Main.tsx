import { useMemo } from "react";
import { EditorController } from "../lib/EditorController";
import { SAMPLE_XML } from "../lib/sampeContent";
import { XHTML5MEIDocument } from "../lib/XHTML5MEIDocument";
import { CodeEditor } from "./CodeEditor";
import { DocumentEditor } from "./DocumentEditor";
import { EditorControllerDebugView } from "./EditorControllerDebugView";

export function Main() {
  const editorController = useMemo(() => {
    return new EditorController(new XHTML5MEIDocument(SAMPLE_XML));
  }, []);

  return (
    <div className="flex h-screen flex-col">
      <div className="border-ui-border flex h-2/3 w-full flex-row border-b">
        <div className="border-ui-border h-full w-1/3 overflow-scroll border-r">
          {/* <DocumentViewer controller={editorController} /> */}
        </div>
        <div className="border-ui-border h-full w-1/3 overflow-scroll border-r">
          <DocumentEditor editorController={editorController} />
        </div>
        <div className="h-full w-1/3 overflow-hidden">
          <CodeEditor editorController={editorController} />
        </div>
      </div>
      <div className="border-ui-border flex h-1/3 flex-col border-b bg-gray-100">
        <div className="flex-1 overflow-hidden">
          <EditorControllerDebugView controller={editorController} />
        </div>
      </div>
    </div>
  );
}

export default Main;
