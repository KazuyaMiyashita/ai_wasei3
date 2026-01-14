import { xml } from "@codemirror/lang-xml";
import CodeMirror, { type ReactCodeMirrorRef } from "@uiw/react-codemirror";

import { useEffect, useRef, useState } from "react";
import type { XHTML5MEIDocument } from "./Main";

export function CodeEditor({
  xhtml5meiDocument,
}: {
  xhtml5meiDocument: XHTML5MEIDocument;
}) {
  const codeMirrorRef = useRef<ReactCodeMirrorRef>(null);

  const [editorContent, setEditorContent] = useState(
    xhtml5meiDocument?.rawContent ?? "",
  );
  useEffect(() => {
    setEditorContent(xhtml5meiDocument?.rawContent ?? "");
  }, [xhtml5meiDocument]);

  return (
    <CodeMirror
      ref={codeMirrorRef}
      value={editorContent}
      height="100%"
      className="h-full"
      extensions={[xml()]}
      basicSetup={{
        lineNumbers: true,
        foldGutter: true,
        highlightActiveLine: false,
        highlightSelectionMatches: false,
        indentOnInput: false,
        autocompletion: false,
        bracketMatching: false,
        closeBrackets: false,
      }}
      onUpdate={(update) => {
        // update は ViewUpdate 型のオブジェクト
        if (update.docChanged) {
          console.log("--- CodeMirror Transaction ---");

          // すべての変更（changes）をループで確認
          update.transactions.forEach((tr) => {
            tr.changes.iterChanges((fromA, toA, fromB, toB, inserted) => {
              console.log(`変更範囲: ${fromA}〜${toA}`);
              console.log(`挿入されたテキスト: "${inserted.toString()}"`);
              // inserted.toString() が空なら「削除」を意味します
            });
          });
        }
      }}
    />
  );
}
