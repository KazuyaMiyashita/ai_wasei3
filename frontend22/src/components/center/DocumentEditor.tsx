import { baseKeymap } from "prosemirror-commands";
import { history, redo, undo } from "prosemirror-history";
import { keymap } from "prosemirror-keymap";
import { EditorState } from "prosemirror-state";
import { EditorView } from "prosemirror-view";
import { useEffect, useRef } from "react";
import { FullReplaceAction } from "../../lib/editor/actions";
import type { ActiveDocument } from "../../lib/editor/active-document";
import { parseXmlToAst } from "../../lib/editor/ast/parser";
import { serializeAstToXml } from "../../lib/editor/ast/serializer";
import type { XhtmlMeiContentAdapter } from "../../lib/editor/content-adapters";
import { xhtmlMeiSchema } from "../../lib/editor/schema/xhtml-mei";
import { SyncManager } from "../../lib/editor/sync/manager";
import { MeiNodeView } from "../../lib/editor/view/mei-node-view";

interface DocumentEditorProps {
  activeDocument: ActiveDocument;
}

export function DocumentEditor({ activeDocument }: DocumentEditorProps) {
  const proseMirrorRef = useRef<HTMLDivElement>(null);
  const editorViewRef = useRef<EditorView | null>(null);

  // Ref to track the last code content synced from ProseMirror to prevent loops
  const lastSyncedCode = useRef(activeDocument.getContent());
  const isUpdatingFromCode = useRef(false);

  // Initialize ProseMirror
  useEffect(() => {
    if (!proseMirrorRef.current) return;

    proseMirrorRef.current.innerHTML = "";

    const adapter = activeDocument.adapter as XhtmlMeiContentAdapter;
    const ast =
      adapter.getAst?.() || parseXmlToAst(activeDocument.getContent()).root;

    const state = EditorState.create({
      doc: SyncManager.astToProseMirrorDoc(ast),
      schema: xhtmlMeiSchema,
      plugins: [
        history(),
        keymap({
          "Mod-z": undo,
          "Mod-y": redo,
          "Mod-Shift-z": redo,
        }),
        keymap(baseKeymap),
      ],
    });

    const view = new EditorView(proseMirrorRef.current, {
      state,
      nodeViews: {
        mei(node) {
          return new MeiNodeView(node);
        },
      },
      dispatchTransaction(tr) {
        const newState = view.state.apply(tr);
        view.updateState(newState);

        if (tr.docChanged && !isUpdatingFromCode.current) {
          const newAst = SyncManager.pmDocToAst(newState.doc);
          const newXml = serializeAstToXml(newAst);

          lastSyncedCode.current = newXml;
          activeDocument.edit(new FullReplaceAction(newXml));
        }
      },
    });

    editorViewRef.current = view;
    return () => view.destroy();
  }, [activeDocument]);

  // Sync from ActiveDocument (Code -> Rich)
  useEffect(() => {
    const unsubscribe = activeDocument.subscribe(() => {
      const content = activeDocument.getContent();
      if (content === lastSyncedCode.current) return;

      // Debounced or immediate? Let's follow existing logic
      isUpdatingFromCode.current = true;
      const result = parseXmlToAst(content);
      if (editorViewRef.current) {
        const tr = SyncManager.reconcile(
          editorViewRef.current.state,
          result.root,
        );
        if (tr.docChanged) {
          editorViewRef.current.dispatch(tr);
        }
      }
      lastSyncedCode.current = content;
      isUpdatingFromCode.current = false;
    });

    return unsubscribe;
  }, [activeDocument]);

  const containerClass =
    activeDocument.originalDocument.type === "mei"
      ? "main-container-x-scrollable"
      : "main-container-x-fit";

  return (
    <div className={`h-full overflow-y-auto bg-white ${containerClass}`}>
      <div
        ref={proseMirrorRef}
        className={
          activeDocument.originalDocument.type === "xhtml5+mei"
            ? "prose-mirror-editor main-content mx-auto px-12 py-6 outline-none"
            : "prose-mirror-editor outline-none"
        }
      />
    </div>
  );
}
