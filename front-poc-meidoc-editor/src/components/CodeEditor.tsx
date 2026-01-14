import { defaultKeymap, history, historyKeymap } from "@codemirror/commands";
import { xml } from "@codemirror/lang-xml";
import { foldGutter } from "@codemirror/language";
import { EditorState } from "@codemirror/state";
import { EditorView, keymap, lineNumbers } from "@codemirror/view";
import { useEffect, useRef } from "react";
import type { EditorController } from "../lib/EditorController";

export function CodeEditor({
  editorController,
}: {
  editorController: EditorController;
}) {
  const codeMirrorRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!codeMirrorRef.current) return;

    console.log("Initializing CodeMirror...");

    const startState = EditorState.create({
      doc: editorController.document.rawContent,
      extensions: [
        lineNumbers(),
        foldGutter(),
        history(),
        keymap.of([...defaultKeymap, ...historyKeymap]),
        xml(),
        EditorView.updateListener.of((update) => {
          if (update.docChanged) {
            editorController.handleCodeMirrorUpdate(update);
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

    editorController.setCodeMirrorView(view);

    return () => {
      console.log("Destroying CodeMirror...");
      editorController.setCodeMirrorView(null);
      view.destroy();
    };
  }, [editorController]);

  return <div ref={codeMirrorRef} className="h-full" />;
}
