import { defaultKeymap, history, historyKeymap } from "@codemirror/commands";
import { xml } from "@codemirror/lang-xml";
import { foldGutter } from "@codemirror/language";
import { EditorState } from "@codemirror/state";
import { EditorView, keymap, lineNumbers } from "@codemirror/view";
import { useEffect, useRef } from "react";
import type { EditorController } from "../controllers/EditorController";
import type { XHTML5MEIDocument } from "../lib/XHTML5MEIDocument";

export function CodeEditor({
  xhtml5meiDocument,
  controller,
}: {
  xhtml5meiDocument: XHTML5MEIDocument;
  controller: EditorController;
}) {
  const codeMirrorRef = useRef<HTMLDivElement>(null);
  const viewRef = useRef<EditorView | null>(null);

  useEffect(() => {
    if (!codeMirrorRef.current) return;

    const startState = EditorState.create({
      doc: xhtml5meiDocument?.rawContent ?? "",
      extensions: [
        lineNumbers(),
        foldGutter(),
        history(),
        keymap.of([...defaultKeymap, ...historyKeymap]),
        xml(),
        EditorView.updateListener.of((update) => {
          if (update.docChanged) {
            controller.handleCodeMirrorUpdate(update);
          }
        }),
        EditorView.theme({
          "&": { height: "100%" },
        }),
      ],
    });

    const view = new EditorView({
      state: startState,
      parent: codeMirrorRef.current,
    });

    viewRef.current = view;

    return () => {
      view.destroy();
      viewRef.current = null;
    };
  }, [xhtml5meiDocument, controller]);

  return <div ref={codeMirrorRef} className="h-full" />;
}
