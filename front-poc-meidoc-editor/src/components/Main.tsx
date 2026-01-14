import { Edit, Eye } from "lucide-react";
import { Toolbar } from "radix-ui";
import { useState } from "react";
import { useEditorController } from "../hooks/useEditorController";
import { SAMPLE_XML } from "../lib/sampeContent";
import { XHTML5MEIDocument } from "../lib/XHTML5MEIDocument";
import { CodeEditor } from "./CodeEditor";
import { DocumentEditor } from "./DocumentEditor";
import { DocumentViewer } from "./DocumentViewer";
import { EditorControllerViewer } from "./EditorControllerViewer";

export type EditMode = "view" | "edit";

export function Main() {
  const [editMode, setEditMode] = useState<EditMode>("view");

  const editorController = useEditorController(
    new XHTML5MEIDocument(SAMPLE_XML),
  );

  return (
    <div className="flex h-screen flex-col">
      <div className="flex h-2/3 w-full flex-row">
        <div className="h-full w-1/2 overflow-scroll">
          {editMode === "view" ? (
            <DocumentViewer xhtml5meiDocument={editorController.document} />
          ) : (
            <DocumentEditor editorController={editorController} />
          )}
        </div>
        <div className="h-full w-1/2 overflow-hidden">
          <CodeEditor editorController={editorController} />
        </div>
      </div>
      <div className="flex h-1/3 flex-col bg-gray-100">
        <div className="shrink-0 border-b border-gray-200 bg-white p-1">
          <Toolbar.Root className="border-ui-border">
            <Toolbar.ToggleGroup
              type="single"
              value={editMode}
              className="flex shrink-0 gap-1"
              onValueChange={(value) => {
                if (value) setEditMode(value as EditMode);
              }}
            >
              <Toolbar.ToggleItem
                value="view"
                className="ui-action-button data-[state=on]:bg-ui-bg-subtle data-[state=on]:text-brand-primary flex items-center gap-1 px-2 py-1"
              >
                <Eye size={14} />
                <span className="text-xs font-medium">閲覧</span>
              </Toolbar.ToggleItem>
              <Toolbar.ToggleItem
                value="edit"
                className="ui-action-button data-[state=on]:bg-ui-bg-subtle data-[state=on]:text-brand-primary flex items-center gap-1 px-2 py-1"
              >
                <Edit size={14} />
                <span className="text-xs font-medium">編集</span>
              </Toolbar.ToggleItem>
            </Toolbar.ToggleGroup>
          </Toolbar.Root>
        </div>
        <div className="flex-1 overflow-hidden">
          <EditorControllerViewer controller={editorController} />
        </div>
      </div>
    </div>
  );
}

export default Main;
