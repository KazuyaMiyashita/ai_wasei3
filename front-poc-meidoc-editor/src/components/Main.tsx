import { Edit, Eye } from "lucide-react";
import { Toolbar } from "radix-ui";
import { useState } from "react";
import { SAMPLE_XML } from "../lib/sampeContent";
import { CodeEditor } from "./CodeEditor";
import { DocumentEditor } from "./DocumentEditor";
import { DocumentViewer } from "./DocumentViewer";

/** XHTML5の文章中にMEIのXMLを含むドキュメント。
 */
export class XHTML5MEIDocument {
  // 正常な状態であれば内容はXMLであるためXMLDocument型として扱いたいところだが、
  // シンタックスが壊れた状態で読み込み・保存を可能にするため、string型で扱う
  rawContent: string;

  constructor(rawContent: string) {
    this.rawContent = rawContent;
  }

  isValidXML(): boolean {
    try {
      const parser = new DOMParser();
      const xmlDoc = parser.parseFromString(this.rawContent, "text/xml");
      const parseErrors = xmlDoc.getElementsByTagName("parsererror");
      return parseErrors.length === 0;
    } catch (_e) {
      return false;
    }
  }

  toXMLDocument(): XMLDocument | null {
    try {
      const parser = new DOMParser();
      const xmlDoc = parser.parseFromString(this.rawContent, "text/xml");
      const parseErrors = xmlDoc.getElementsByTagName("parsererror");
      return parseErrors.length === 0 ? xmlDoc : null;
    } catch (_e) {
      return null;
    }
  }
}

export type EditMode = "view" | "edit";

export function Main() {
  const [editMode, setEditMode] = useState<EditMode>("view");

  const [xhtml5meiDocument] = useState(new XHTML5MEIDocument(SAMPLE_XML));

  return (
    <div className="flex flex-col h-screen">
      <div className="flex flex-row w-full h-2/3">
        <div className="w-1/2 h-full bg-amber-100 overflow-scroll">
          {editMode === "view" ? (
            <DocumentViewer xhtml5meiDocument={xhtml5meiDocument} />
          ) : (
            <DocumentEditor xhtml5meiDocument={xhtml5meiDocument} />
          )}
        </div>
        <div className="w-1/2 h-full overflow-hidden">
          <CodeEditor xhtml5meiDocument={xhtml5meiDocument} />
        </div>
      </div>
      <div className="h-1/3 bg-gray-100">
        <div>
          <Toolbar.Root className="border-ui-border">
            <Toolbar.ToggleGroup
              type="single"
              value={editMode}
              className="flex shrink-0 gap-1 "
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
