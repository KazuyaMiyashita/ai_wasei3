import { xml } from "@codemirror/lang-xml";
import {
  Decoration,
  type DecorationSet,
  EditorView,
  keymap,
  type ViewUpdate,
} from "@codemirror/view";
import CodeMirror, {
  Prec,
  type ReactCodeMirrorRef,
  StateEffect,
  StateField,
} from "@uiw/react-codemirror";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import {
  useApplication,
  useApplicationState,
} from "../../context/ApplicationContext";
import { FullReplaceAction } from "../../lib/editor/actions";
import type {
  ActiveDocument,
  ActiveDocumentState,
} from "../../lib/editor/active-document";
import {
  buildIdRangeMap,
  findIdsFromSelection,
  type IdRange,
} from "../../lib/model/music/xml-navigation";

interface CodeViewProps {
  activeDocument: ActiveDocument;
}

const setSelectionEffect = StateEffect.define<string[]>();

const selectionField = StateField.define<DecorationSet>({
  create() {
    return Decoration.none;
  },
  update(decorations, tr) {
    decorations = decorations.map(tr.changes);
    for (const e of tr.effects) {
      if (e.is(setSelectionEffect)) {
        const ids = e.value;
        const deco = [];
        const text = tr.state.doc.toString();
        // Optimization: Cache idRangeMap if text hasn't changed.
        const idMap = buildIdRangeMap(text);

        for (const id of ids) {
          const ranges = idMap.filter((r: IdRange) => r.id === id);
          for (const range of ranges) {
            deco.push(
              Decoration.mark({
                class: "cm-score-selection-highlight",
              }).range(range.start, range.end),
            );
          }
        }
        deco.sort((a, b) => a.from - b.from);
        return Decoration.set(deco, true);
      }
    }
    return decorations;
  },
  provide: (f) => EditorView.decorations.from(f),
});

export function CodeView({ activeDocument }: CodeViewProps) {
  const application = useApplication();
  const selectedIds = useApplicationState(
    (state) => state.selection.selectedIds,
  );
  const editorSelectedIds = useApplicationState(
    (state) => state.selection.editorSelectedIds,
  );

  const [_docState, setDocState] = useState<ActiveDocumentState>(
    activeDocument.getState(),
  );

  useEffect(() => {
    setDocState(activeDocument.getState());
    return activeDocument.subscribe(setDocState);
  }, [activeDocument]);

  const [editorContent, setEditorContent] = useState(
    activeDocument.getContent(),
  );
  const editorRef = useRef<ReactCodeMirrorRef>(null);

  const cachedIdRangeMapRef = useRef<IdRange[]>([]);
  const timeoutRef = useRef<NodeJS.Timeout | null>(null);

  // Update editor content when activeDocument changes externally
  useEffect(() => {
    const currentContent = activeDocument.getContent();
    if (currentContent !== editorContent) {
      setEditorContent(currentContent);
      cachedIdRangeMapRef.current = buildIdRangeMap(currentContent);
    } else if (
      cachedIdRangeMapRef.current.length === 0 &&
      currentContent.length > 0
    ) {
      // Initialization case
      cachedIdRangeMapRef.current = buildIdRangeMap(currentContent);
    }
  }, [activeDocument, editorContent]);

  // Clean up timeout
  useEffect(() => {
    return () => {
      if (timeoutRef.current) clearTimeout(timeoutRef.current);
    };
  }, []);

  const isValidXml = useCallback((xmlString: string) => {
    try {
      const parser = new DOMParser();
      const doc = parser.parseFromString(xmlString, "application/xml");
      const errorNode = doc.querySelector("parsererror");
      return !errorNode;
    } catch {
      return false;
    }
  }, []);

  const performUpdate = useCallback(
    (value: string, force = false) => {
      if (force || isValidXml(value)) {
        // Only update if content is strictly different
        if (value !== activeDocument.getContent()) {
          activeDocument.edit(new FullReplaceAction(value));
        }
      }
    },
    [activeDocument, isValidXml],
  );

  const handleEditorChange = useCallback(
    (value: string) => {
      setEditorContent(value);
      cachedIdRangeMapRef.current = buildIdRangeMap(value);

      // Debounce update (Auto-Apply)
      if (timeoutRef.current) clearTimeout(timeoutRef.current);
      timeoutRef.current = setTimeout(() => {
        performUpdate(value, false); // Only apply automatically if valid
      }, 200); // 200ms debounce
    },
    [performUpdate],
  );

  // Sync: Editor Highlight <- Score Selection
  useEffect(() => {
    if (editorRef.current?.view) {
      editorRef.current.view.dispatch({
        effects: setSelectionEffect.of(selectedIds),
      });
    }
  }, [selectedIds]);

  const handleUpdate = useCallback(
    (update: ViewUpdate) => {
      if (!update.selectionSet && !update.focusChanged) return;
      if (!update.view.hasFocus) return;

      const { state } = update;
      const mainSelection = state.selection.main;

      const resultIds = findIdsFromSelection(
        cachedIdRangeMapRef.current,
        mainSelection.from,
        mainSelection.to,
      );

      const isChanged =
        resultIds.length !== editorSelectedIds.length ||
        resultIds.some(
          (id: string, index: number) => id !== editorSelectedIds[index],
        );

      if (isChanged) {
        application.selectionManager.setEditorSelectedIds(resultIds);
      }
    },
    [application, editorSelectedIds],
  );

  const extensions = useMemo(
    () => [
      xml(),
      selectionField,
      Prec.highest(
        keymap.of([
          {
            key: "Mod-Enter",
            run: (view) => {
              const content = view.state.doc.toString();
              performUpdate(content, true);
              return true;
            },
          },
          {
            key: "Mod-s",
            run: (view) => {
              const content = view.state.doc.toString();
              performUpdate(content, true);
              application.save();
              return true;
            },
          },
          {
            key: "Shift-Mod-s",
            run: (view) => {
              const content = view.state.doc.toString();
              performUpdate(content, true);
              const name = prompt(
                "Save as (filename):",
                activeDocument.originalDocument.path.split("/").pop() ||
                  "new_file.mei",
              );
              if (name) {
                const path = name.startsWith("/") ? name : `/${name}`;
                application.saveAs(path);
              }
              return true;
            },
          },
        ]),
      ),
      EditorView.theme({
        "&": {
          fontSize: "12px",
          fontFamily: "monospace",
          height: "100%",
        },
        ".cm-content": {
          padding: "0",
        },
        ".cm-line": {
          padding: "0 4px",
          lineHeight: "1.0",
        },
        ".cm-activeLine": {
          backgroundColor: "transparent",
        },
        ".cm-score-selection-highlight": {
          backgroundColor:
            "color-mix(in srgb, var(--color-selection, #b3d4fc) 15%, transparent)",
          borderLeft: "1.4px solid var(--color-selection, #b3d4fc)",
        },
        ".cm-selectionBackground, .cm-content ::selection": {
          backgroundColor:
            "color-mix(in srgb, var(--color-editor-selection, #b3d4fc) 20%, transparent) !important",
        },
      }),
    ],
    [performUpdate, activeDocument.originalDocument.path, application],
  );

  return (
    <div className="main-content flex h-full flex-col">
      <div className="flex-1 overflow-auto">
        <CodeMirror
          ref={editorRef}
          value={editorContent}
          height="100%"
          extensions={extensions}
          onChange={handleEditorChange}
          onUpdate={handleUpdate}
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
        />
      </div>
    </div>
  );
}
