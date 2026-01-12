import { useCallback, useEffect, useRef, useState } from "react";
import {
  useApplication,
  useApplicationState,
} from "../../context/ApplicationContext";
import type { ActiveDocumentState } from "../../lib/editor/active-document";
import {
  applyEditorHighlights,
  applyHighlights,
  forceApplyHighlights,
} from "../../lib/renderer/highlighter";
import { getClickedElementId } from "../../lib/renderer/score-interaction";
import {
  transformVerovioSVG,
  transformXhtmlMei,
} from "../../lib/renderer/transformer";
import {
  drawAllPlaybackCursors,
  injectHitboxes,
} from "../../lib/view/svg-utils";

export function useScoreRenderer() {
  const application = useApplication();

  // Application State
  const currentDocument = useApplicationState((state) =>
    state.currentDocumentId
      ? state.activeDocuments.get(state.currentDocumentId)
      : null,
  );
  const selectedIds = useApplicationState(
    (state) => state.selection.selectedIds,
  );
  const editorSelectedIds = useApplicationState(
    (state) => state.selection.editorSelectedIds,
  );
  const scoreState = useApplicationState((state) => state.score);

  const [renderedContent, setRenderedContent] = useState<string>("");
  const containerRef = useRef<HTMLDivElement>(null);

  const [docState, setDocState] = useState<ActiveDocumentState | undefined>(
    currentDocument?.getState(),
  );

  useEffect(() => {
    if (!currentDocument) {
      setDocState(undefined);
      return;
    }
    setDocState(currentDocument.getState());
    return currentDocument.subscribe(setDocState);
  }, [currentDocument]);

  const { loading: verovioLoading, toolkitReady, layoutVersion } = scoreState;

  // Track previous selection for diffing updates
  const prevSelectedIdsRef = useRef<string[]>([]);

  // Apply Highlights (Manual DOM Manipulation with Diffing)
  const applyHighlightsEffect = useCallback(
    (forceAll = false) => {
      if (!containerRef.current) return;
      const svg = containerRef.current.querySelector("svg");
      if (!svg) return;

      const prevIds = prevSelectedIdsRef.current;
      const currentIds = selectedIds;

      if (forceAll) {
        forceApplyHighlights(svg, currentIds);
      } else {
        applyHighlights(svg, currentIds, prevIds);
      }

      // Update Ref
      prevSelectedIdsRef.current = currentIds;
    },
    [selectedIds],
  );

  // Apply Editor Highlights (Rectangles)
  const applyEditorHighlightsEffect = useCallback(() => {
    if (!containerRef.current) return;
    const svg = containerRef.current.querySelector("svg");
    if (!svg) return;

    applyEditorHighlights(svg, editorSelectedIds);
  }, [editorSelectedIds]);

  // Effect 1: Handle Document Updates (Content/Structure)
  useEffect(() => {
    if (!toolkitReady || !currentDocument || !docState) return;

    const hint = docState.lastUpdateHint;

    // Determine if we need to reload data into Verovio
    if (hint.type === "full") {
      application.scoreRenderer.loadData(currentDocument.getContent());
    } else if (hint.type === "mei-content") {
      // For now, treat mei-content same as full load for single MEI
      // Optimization: if XhtmlMei, we might handle it differently but scoreRenderer.loadData expects full MEI.
      // If XhtmlMei, transformer handles it.
      // But wait, scoreRenderer is managing Verovio instance.
      // If we are in xhtml mode, scoreRenderer might not be main renderer?
      // Actually `transformXhtmlMei` uses a toolkit instance.
      // Let's stick to standard flow: update data in renderer.
      application.scoreRenderer.loadData(currentDocument.getContent());
    } else if (hint.type === "mei-action") {
      // Optimize: Use Verovio edit
      const result = application.scoreRenderer.edit(hint.action);
      if (!result.success) {
        // Fallback to reload if edit failed
        console.warn("Optimized edit failed, falling back to reload");
        application.scoreRenderer.loadData(currentDocument.getContent());
      }
    }
    // If "none", do nothing.
  }, [
    docState, // Depend on state object identity change (triggered by notifyObservers)
    toolkitReady,
    application,
    currentDocument,
  ]);

  // Effect 2: Rendering (Produce SVG/HTML)
  // This depends on layoutVersion (incremented by scoreRenderer) or docState changes for XHTML
  useEffect(() => {
    if (!toolkitReady || !currentDocument) return;

    // Force dependency on layoutVersion to trigger re-render after loadData/redoLayout
    void layoutVersion;

    if (currentDocument.originalDocument.type === "mei") {
      const svg = application.scoreRenderer.renderPageToSVG();
      if (!svg) return;
      setRenderedContent(transformVerovioSVG(svg));
    } else {
      // XHTML+MEI
      // We need content string here.
      const content = currentDocument.getContent();
      const toolkit = application.scoreRenderer.getToolkit();
      if (!toolkit) return;
      setRenderedContent(transformXhtmlMei(content, toolkit));
    }
  }, [
    layoutVersion,
    toolkitReady,
    currentDocument, // If doc instance changes
    application,
  ]);

  // Effect 3: Structure Update (Hitboxes & Cursors)
  useEffect(() => {
    if (containerRef.current) {
      const svg = containerRef.current.querySelector("svg");
      if (svg) {
        injectHitboxes(svg);
        const content = currentDocument?.getContent() ?? null;
        drawAllPlaybackCursors(svg, content);
        applyHighlightsEffect(true);
        applyEditorHighlightsEffect();
      }
    }
  }, [applyHighlightsEffect, applyEditorHighlightsEffect, currentDocument]);

  // Effect 4: Selection Updates
  useEffect(() => {
    applyHighlightsEffect(false);
  }, [applyHighlightsEffect]);

  useEffect(() => {
    applyEditorHighlightsEffect();
  }, [applyEditorHighlightsEffect]);

  const handleClick = useCallback(
    (e: React.MouseEvent) => {
      const targetId = getClickedElementId(e.target, containerRef.current);

      if (targetId) {
        application.selectionManager.setSelectedIds([targetId]);
        application.playSelectedNote();
      } else {
        application.selectionManager.setSelectedIds([]);
      }
    },
    [application],
  );

  const handleContextMenu = useCallback((e: React.MouseEvent) => {
    e.preventDefault();
    // alert("Context menu not implemented yet.");
  }, []);

  return {
    renderedContent,
    isReady: toolkitReady && !verovioLoading,
    containerRef,
    handleClick,
    handleContextMenu,
  };
}
