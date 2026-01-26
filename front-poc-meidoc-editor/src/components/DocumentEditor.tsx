import "prosemirror-menu/style/menu.css";
import { EditorView } from "prosemirror-view";
import { useEffect, useRef } from "react";
import type { EditorController } from "../lib/EditorController";
import "../styles/main-content.css";

export function DocumentEditor({
  editorController,
}: {
  editorController: EditorController;
}) {
  const proseMirrorRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!proseMirrorRef.current) return;

    console.log("Initializing ProseMirror...");

    const state = editorController.createProseMirrorState();

    const view = new EditorView(proseMirrorRef.current, {
      state,
      nodeViews: {
        mei_node(node) {
          const dom = document.createElement("pre");
          dom.className = "mei-content";
          dom.textContent = node.attrs.rawContent;
          return {
            dom,
            update(newNode) {
              if (newNode.type.name !== "mei_node") return false;
              if (newNode.attrs.rawContent !== node.attrs.rawContent) {
                dom.textContent = newNode.attrs.rawContent;
              }
              return true;
            },
          };
        },
      },
      dispatchTransaction(tr) {
        const newState = view.state.apply(tr);
        view.updateState(newState);

        if (tr.docChanged) {
          editorController.handleProseMirrorTransaction(tr, newState);
        }
      },
    });

    editorController.setProseMirrorView(view);

    return () => {
      console.log("Destroying ProseMirror...");
      editorController.setProseMirrorView(null);
      view.destroy();
    };
  }, [editorController]);

  return (
    <div
      ref={proseMirrorRef}
      className="main-content h-full overflow-y-auto bg-white p-4"
    ></div>
  );
}
