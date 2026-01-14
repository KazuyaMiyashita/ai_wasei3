import { Edit, Eye } from "lucide-react";
import { Toolbar } from "radix-ui";
import { useEffect, useState } from "react";
import { EditorController } from "../controllers/EditorController";
import { SAMPLE_XML } from "../lib/sampeContent";
import type { XHTML5MEIDocument } from "../lib/XHTML5MEIDocument";
import { CodeEditor } from "./CodeEditor";
import { DocumentEditor } from "./DocumentEditor";
import { DocumentViewer } from "./DocumentViewer";

export type EditMode = "view" | "edit";

export function Main() {
  const [editMode, setEditMode] = useState<EditMode>("view");

  // コントローラーの初期化（一度だけ実行）
  const [controller] = useState(() => new EditorController(SAMPLE_XML));

  // 表示用のドキュメント状態
  const [xhtml5meiDocument, setXhtml5meiDocument] = useState<XHTML5MEIDocument>(
    controller.getDocument(),
  );

  // コントローラーからの変更通知を受け取る
  // useEffect(() => {
  //   const unsubscribe = controller.subscribe((newDoc) => {
  //     setXhtml5meiDocument(newDoc);
  //   });
  //   return unsubscribe;
  // }, [controller]);

  return (
    <div className="flex h-screen flex-col">
      <div className="flex h-2/3 w-full flex-row">
        <div className="h-full w-1/2 overflow-scroll bg-amber-100">
          {editMode === "view" ? (
            <DocumentViewer xhtml5meiDocument={xhtml5meiDocument} />
          ) : (
            <DocumentEditor
              xhtml5meiDocument={xhtml5meiDocument}
              controller={controller}
            />
          )}
        </div>
        <div className="h-full w-1/2 overflow-hidden">
          <CodeEditor
            xhtml5meiDocument={xhtml5meiDocument}
            controller={controller}
          />
        </div>
      </div>
      <div className="h-1/3 bg-gray-100">
        <div>
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
      </div>
    </div>
  );
}

export default Main;
