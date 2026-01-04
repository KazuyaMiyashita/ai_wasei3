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
import {
  ArrowLeft,
  ArrowRight,
  ArrowUp,
  ChevronsUp,
  ZoomIn,
} from "lucide-react";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import type { UseScoreInteraction } from "../../../../hooks/score/useScoreInteraction";
import type { UseScoreView } from "../../../../hooks/score/useScoreView";
import { useNotification } from "../../../../hooks/useNotification";
import { editFromXml, validateEditXml } from "../../../../lib/score/mei-edit";
import { formatXml } from "../../../../lib/score/mei-utils";
import {
  buildIdRangeMap,
  findElementById,
  findLowestCommonAncestor,
  type IdRange,
} from "../../../../lib/score/xml-navigation";
import { CollapsibleSection } from "../../../ui/CollapsibleSection";
import { IconButton } from "../../../ui/IconButton";

interface SelectionDetailsProps {
  selectedIds: UseScoreInteraction["selectedIds"];
  meiXML: UseScoreView["meiXML"];
  onEdit?: UseScoreView["edit"];
  onEditorSelectionChange?: (ids: string[]) => void;
  editorSelectedIds?: string[];
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
        // ここでの計算コストを避けるため、別途計算したmapを渡す等の最適化も考えられるが
        // 現状はそこまで巨大なXMLを扱わない前提とする
        const idMap = buildIdRangeMap(text);

        for (const id of ids) {
          // 同じIDを持つ要素が複数ある場合（本来XMLとしては不正だが）や
          // 複数のIDが指定された場合に対応するため、すべて探す
          const ranges = idMap.filter((r) => r.id === id);
          for (const range of ranges) {
            deco.push(
              Decoration.mark({
                class: "cm-score-selection-highlight",
              }).range(range.start, range.end),
            );
          }
        }
        // Decoration.set に渡す配列は start でソートされている必要がある
        deco.sort((a, b) => a.from - b.from);
        return Decoration.set(deco, true);
      }
    }
    return decorations;
  },
  provide: (f) => EditorView.decorations.from(f),
});

export const SelectionDetails = ({
  selectedIds: scoreSelectedIds,
  meiXML,
  onEdit,
  onEditorSelectionChange,
  editorSelectedIds = [],
}: SelectionDetailsProps) => {
  const { notify } = useNotification();
  const editorRef = useRef<ReactCodeMirrorRef>(null);

  // The ID of the element currently displayed as the root in the editor
  const [viewRootId, setViewRootId] = useState<string | null>(null);

  // Content state
  const [editorContent, setEditorContent] = useState<string>("");
  const [editedXml, setEditedXml] = useState<string | null>(null);
  const [isXmlValid, setIsXmlValid] = useState(true);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const isSubmittingRef = useRef(false);

  // 比較用のrefはuseEffect内での無限ループ防止や変更検知のために必要
  // stateにするとレンダリングが増えるため、ここではrefのまま維持が適切と判断
  const lastSelectedIdsRef = useRef<string[]>([]);

  // onUpdate内で参照するためのキャッシュ
  // これをstateにするとonUpdateが再生成され、Editorの再設定が走る可能性があるためref推奨
  const cachedIdRangeMapRef = useRef<IdRange[]>([]);

  // Sync: Score Selection -> Editor View
  useEffect(() => {
    if (!meiXML || scoreSelectedIds.length === 0) return;

    // 楽譜側での選択が以前と変わっているかチェック
    const isNewSelection =
      JSON.stringify(scoreSelectedIds) !==
      JSON.stringify(lastSelectedIdsRef.current);

    if (isNewSelection) {
      // 選択が変わった時（新しく音符をクリックした時など）だけ
      // 共通祖先を探して viewRootId を更新する
      const newRoot = findLowestCommonAncestor(meiXML, scoreSelectedIds);
      if (newRoot) {
        const id = newRoot.getAttribute("xml:id") || newRoot.id;
        if (id) {
          setViewRootId(id);
          // 今回の選択を記録しておく
          lastSelectedIdsRef.current = [...scoreSelectedIds];
        }
      }
    }
  }, [scoreSelectedIds, meiXML]);

  // Update Editor Content when viewRootId changes
  useEffect(() => {
    if (!meiXML || !viewRootId) {
      setEditorContent("");
      return;
    }
    const element = findElementById(meiXML, viewRootId);
    if (!element) {
      setEditorContent("Element not found");
      return;
    }

    const serializer = new XMLSerializer();
    const raw = serializer.serializeToString(element);
    const formatted = formatXml(raw) || raw;
    setEditorContent(formatted);
    setEditedXml(null); // Reset edits on view change
    setIsXmlValid(true);
    setErrorMessage(null);
    cachedIdRangeMapRef.current = buildIdRangeMap(formatted);
  }, [viewRootId, meiXML]);

  const performEdit = useCallback(() => {
    if (
      editedXml !== null &&
      viewRootId &&
      meiXML &&
      onEdit &&
      isXmlValid &&
      !errorMessage
    ) {
      isSubmittingRef.current = true;
      const result = editFromXml(meiXML, viewRootId, editedXml);

      if (result.type !== "error") {
        if (result.type === "no-change") {
          notify("No changes detected", "info", "Inspector");
        } else {
          const editStatus = onEdit(result);
          if (editStatus.success) {
            setEditedXml(null);
            setIsXmlValid(true);
            setErrorMessage(null);
          } else {
            setIsXmlValid(false);
            setErrorMessage(editStatus.message || "Edit failed in Verovio");
          }
        }
      } else {
        notify(result.error || "Edit failed", "error", "Inspector");
        setIsXmlValid(false);
        setErrorMessage(result.error || "Unknown error");
      }

      setTimeout(() => {
        isSubmittingRef.current = false;
      }, 100);
    }
  }, [editedXml, viewRootId, meiXML, onEdit, notify, isXmlValid, errorMessage]);

  // Navigation Handlers
  const handleNavTop = () => {
    if (meiXML?.documentElement) {
      const id =
        meiXML.documentElement.getAttribute("xml:id") ||
        meiXML.documentElement.id;
      if (id) setViewRootId(id);
    }
  };

  const handleNavParent = () => {
    if (!meiXML || !viewRootId) return;
    const current = findElementById(meiXML, viewRootId);
    if (current?.parentElement) {
      const id =
        current.parentElement.getAttribute("xml:id") ||
        current.parentElement.id;
      if (id) setViewRootId(id);
    }
  };

  const handleNavPrev = () => {
    if (!meiXML || !viewRootId) return;
    const current = findElementById(meiXML, viewRootId);
    if (!current) return;

    let prev = current.previousSibling;
    while (prev && prev.nodeType !== Node.ELEMENT_NODE) {
      prev = prev.previousSibling;
    }

    if (prev && prev.nodeType === Node.ELEMENT_NODE) {
      const el = prev as Element;
      if (el.id || el.getAttribute("xml:id")) {
        const id = el.getAttribute("xml:id") || el.id;
        setViewRootId(id);
      }
    }
  };

  const handleNavNext = () => {
    if (!meiXML || !viewRootId) return;
    const current = findElementById(meiXML, viewRootId);
    if (!current) return;

    let next = current.nextSibling;
    while (next && next.nodeType !== Node.ELEMENT_NODE) {
      next = next.nextSibling;
    }

    if (next && next.nodeType === Node.ELEMENT_NODE) {
      const el = next as Element;
      if (el.id || el.getAttribute("xml:id")) {
        const id = el.getAttribute("xml:id") || el.id;
        setViewRootId(id);
      }
    }
  };

  const handleZoomIn = () => {
    if (scoreSelectedIds.length > 0) {
      if (scoreSelectedIds[0] !== viewRootId) {
        setViewRootId(scoreSelectedIds[0]);
      }
    }
  };

  const handleEditorChange = (value: string) => {
    setEditedXml(value);
    cachedIdRangeMapRef.current = buildIdRangeMap(value);

    if (viewRootId && meiXML) {
      const { isValid, error } = validateEditXml(meiXML, viewRootId, value);
      setIsXmlValid(isValid);
      setErrorMessage(error || null);
    }
  };

  const handleUpdate = useCallback(
    (update: ViewUpdate) => {
      if (!update.selectionSet && !update.focusChanged) return;
      if (!update.view.hasFocus) return;

      if (!onEditorSelectionChange) return;

      const { state } = update;
      const mainSelection = state.selection.main;
      const startOffset = mainSelection.from;
      const endOffset = mainSelection.to;
      const isRange = !mainSelection.empty;

      let resultIds: string[] = [];

      if (!isRange) {
        const containers = cachedIdRangeMapRef.current.filter(
          (r) => r.start <= startOffset && r.end >= startOffset,
        );
        if (containers.length > 0) {
          const deepest = containers.reduce((prev, curr) =>
            curr.end - curr.start < prev.end - prev.start ? curr : prev,
          );
          resultIds = [deepest.id];
        }
      } else {
        resultIds = cachedIdRangeMapRef.current
          .filter((r) => {
            const startIsInside =
              r.start >= startOffset && r.start <= endOffset;
            const endIsInside = r.end >= startOffset && r.end <= endOffset;
            return startIsInside || endIsInside;
          })
          .map((r) => r.id);
      }

      onEditorSelectionChange(resultIds);
    },
    [onEditorSelectionChange],
  );

  const displayValue = editedXml !== null ? editedXml : editorContent;

  // Sync: Editor Highlight <- Score Selection
  // biome-ignore lint/correctness/useExhaustiveDependencies: displayValue is required to re-apply highlights when content changes
  useEffect(() => {
    if (editorRef.current?.view) {
      editorRef.current.view.dispatch({
        effects: setSelectionEffect.of(scoreSelectedIds),
      });
    }
  }, [scoreSelectedIds, displayValue]);

  const extensions = useMemo(
    () => [
      xml(),
      selectionField,
      // EditorView.lineWrapping, // Disable line wrapping
      Prec.highest(
        keymap.of([
          {
            key: "Mod-Enter",
            run: () => {
              performEdit();
              return true;
            },
          },
        ]),
      ),
      EditorView.theme({
        "&": {
          fontSize: "11px",
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
            "color-mix(in srgb, var(--color-selection) 15%, transparent)",
          borderLeft: "1.4px solid var(--color-selection)",
        },
        ".cm-selectionBackground, .cm-content ::selection": {
          backgroundColor:
            "color-mix(in srgb, var(--color-editor-selection) 20%, transparent) !important",
        },
      }),
    ],
    [performEdit],
  );

  return (
    <CollapsibleSection title="Selection Details" contentClassName="px-0 pb-0">
      <div className="flex flex-col h-full">
        <div className="px-4 py-2 border-b border-border-sub bg-surface-muted/20 flex flex-wrap gap-2 justify-between items-center">
          <div className="flex gap-1">
            <IconButton
              icon={<ChevronsUp className="h-4 w-4" />}
              title="Top"
              onClick={handleNavTop}
              className="h-6 w-6"
              variant="ghost"
            />
            <IconButton
              icon={<ArrowUp className="h-4 w-4" />}
              title="Parent"
              onClick={handleNavParent}
              className="h-6 w-6"
              variant="ghost"
            />
            <IconButton
              icon={<ArrowLeft className="h-4 w-4" />}
              title="Prev Sibling"
              onClick={handleNavPrev}
              className="h-6 w-6"
              variant="ghost"
            />
            <IconButton
              icon={<ArrowRight className="h-4 w-4" />}
              title="Next Sibling"
              onClick={handleNavNext}
              className="h-6 w-6"
              variant="ghost"
            />
            <div className="w-px h-4 bg-border-sub mx-1 self-center" />
            <IconButton
              icon={<ZoomIn className="h-4 w-4" />}
              title="Zoom Selection"
              onClick={handleZoomIn}
              disabled={scoreSelectedIds.length === 0}
              className="h-6 w-6"
              variant="ghost"
            />
          </div>

          <div className="flex items-center gap-2">
            <span className="text-xs text-text-sub font-mono">
              {viewRootId || "No Selection"}
            </span>
            <button
              type="button"
              onClick={performEdit}
              disabled={!isXmlValid || !editedXml}
              className={`bg-surface border border-border-main text-text-main cursor-pointer rounded px-2 py-0.5 text-[10px] transition-colors
                  ${
                    !isXmlValid || !editedXml
                      ? "opacity-50 cursor-not-allowed"
                      : "hover:bg-surface-hover"
                  }
                `}
            >
              Apply
            </button>
          </div>
        </div>

        <div className="h-64 w-full relative overflow-auto">
          <CodeMirror
            ref={editorRef}
            value={displayValue}
            height="100%"
            extensions={extensions}
            onChange={handleEditorChange}
            onUpdate={handleUpdate}
            basicSetup={{
              lineNumbers: false,
              foldGutter: false,
              highlightActiveLine: false,
              highlightSelectionMatches: false,
              indentOnInput: false,
              autocompletion: false,
              bracketMatching: false,
            }}
          />
          {!isXmlValid && errorMessage && (
            <div className="absolute bottom-0 left-0 right-0 bg-warning/10 text-warning text-[10px] p-1 border-t border-warning z-10">
              {errorMessage}
            </div>
          )}
        </div>

        <div className="px-4 py-1 text-[10px] text-text-muted flex flex-col gap-1 border-t border-border-sub">
          <div className="flex justify-between">
            <span>Cmd+Enter to Apply</span>
          </div>
          <div className="font-mono text-[9px] text-text-sub opacity-75 break-all">
            <div>scoreSelectedIds: {scoreSelectedIds.join(", ")}</div>
            <div>editorSelectedIds: {editorSelectedIds.join(", ")}</div>
          </div>
        </div>
      </div>
    </CollapsibleSection>
  );
};
