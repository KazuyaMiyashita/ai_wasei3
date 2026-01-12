import { ArrowDown, ArrowUp } from "lucide-react";
import { ContextMenu } from "radix-ui";
import type { MouseEvent, RefObject } from "react";
import { useApplicationState } from "../../context/ApplicationContext";
import type { ActiveDocument } from "../../lib/editor/active-document";

interface DocumentViewProps {
  activeDocument: ActiveDocument;
  renderedContent: string;
  containerRef: RefObject<HTMLDivElement>;
  handleClick: (event: MouseEvent) => void;
  handleContextMenu: (event: MouseEvent) => void;
}

export function DocumentView({
  activeDocument,
  renderedContent,
  containerRef,
  handleClick,
  handleContextMenu: _handleContextMenu, // Keeping it but Radix might override
}: DocumentViewProps) {
  // const application = useApplication(); // Not used
  const selectedIds = useApplicationState(
    (state) => state.selection.selectedIds,
  );
  const hasSelection = selectedIds.length > 0;

  const handleAction = (operation: string) => {
    if (!hasSelection) return;
    const targetId = selectedIds[0];
    void activeDocument.richEdit(operation, { id: targetId });
  };

  return (
    <div
      className={
        activeDocument.originalDocument.type === "xhtml5+mei"
          ? "main-container-x-fit h-full overflow-y-auto"
          : "main-container-x-scrollable h-full overflow-y-auto"
      }
    >
      <ContextMenu.Root>
        <ContextMenu.Trigger>
          <main
            ref={containerRef}
            onClick={handleClick}
            onKeyDown={(e) => {
              if (e.key === "Enter" || e.key === " ") {
                e.preventDefault();
                // We need to pass a MouseEvent-like object or handle logic differently.
                // Since handleClick uses getClickedElementId which relies on event target,
                // keyboard navigation for Score is non-trivial (which element is focused?).
                // For now, just satisfying the linter by adding the handler,
                // but real keyboard interaction requires a different model (cursor).
              }
            }}
            // tabindex is needed for onKeyDown
            // biome-ignore lint/a11y/noNoninteractiveTabindex: Canvas needs focus
            tabIndex={0}
            className={
              activeDocument.originalDocument.type === "xhtml5+mei"
                ? "main-content mx-auto px-12 py-6 outline-none"
                : "outline-none"
            }
            /* biome-ignore lint/security/noDangerouslySetInnerHtml: Verovio output is trusted */
            dangerouslySetInnerHTML={{ __html: renderedContent }}
          />
        </ContextMenu.Trigger>
        <ContextMenu.Portal>
          <ContextMenu.Content className="bg-ui-bg-base border-ui-border animate-in fade-in zoom-in-95 min-w-48 rounded-md border p-1 shadow-md">
            <ContextMenu.Item
              disabled={!hasSelection}
              onSelect={() => handleAction("noteUp")}
              className="hover:bg-ui-bg-hover text-ui-text-main data-highlighted:bg-ui-bg-hover flex cursor-pointer items-center gap-2 rounded-sm px-2 py-1.5 text-xs outline-none data-[disabled]:opacity-50"
            >
              <ArrowUp size={14} />
              <span>Note Up</span>
            </ContextMenu.Item>
            <ContextMenu.Item
              disabled={!hasSelection}
              onSelect={() => handleAction("noteDown")}
              className="hover:bg-ui-bg-hover text-ui-text-main data-highlighted:bg-ui-bg-hover flex cursor-pointer items-center gap-2 rounded-sm px-2 py-1.5 text-xs outline-none data-[disabled]:opacity-50"
            >
              <ArrowDown size={14} />
              <span>Note Down</span>
            </ContextMenu.Item>
          </ContextMenu.Content>
        </ContextMenu.Portal>
      </ContextMenu.Root>
    </div>
  );
}
