import type { UseScoreInteraction } from "../../hooks/score/useScoreInteraction";

interface ContextMenuProps {
  contextMenu: UseScoreInteraction["contextMenu"];
  selectionMode: UseScoreInteraction["selectionMode"];
  onMenuSelect: UseScoreInteraction["handleMenuSelect"];
  onInputChange: UseScoreInteraction["handleContextMenuInputChange"];
  onSubmit: UseScoreInteraction["handleAnnotationSubmit"];
  onClose: UseScoreInteraction["closeContextMenu"];
}

const ContextMenu: React.FC<ContextMenuProps> = ({
  contextMenu,
  selectionMode,
  onMenuSelect,
  onInputChange,
  onSubmit,
  onClose,
}) => {
  return (
    contextMenu && (
      // biome-ignore lint/a11y/noStaticElementInteractions: Context menu container
      // biome-ignore lint/a11y/useKeyWithClickEvents: Context menu container
      <div
        className="bg-surface border-border-main fixed z-50 min-w-50 rounded-md border p-2 shadow-lg"
        style={{
          top: contextMenu.y,
          left: contextMenu.x,
        }}
        onClick={(e) => e.stopPropagation()}
      >
        {contextMenu.step === "menu" ? (
          <div className="text-text-main flex flex-col gap-1 text-sm">
            {selectionMode === "note" && (
              <>
                <button
                  type="button"
                  className="hover:bg-surface-muted w-full rounded px-2 py-1.5 text-left transition-colors"
                  onClick={() => onMenuSelect("NoteAnnotation")}
                >
                  Add note annotation
                </button>
                <button
                  type="button"
                  className="hover:bg-surface-muted w-full rounded px-2 py-1.5 text-left transition-colors"
                  onClick={() => onMenuSelect("KeyInformation")}
                >
                  Add key information
                </button>
              </>
            )}
            {selectionMode === "staff" && (
              <>
                <button
                  type="button"
                  className="hover:bg-surface-muted w-full rounded px-2 py-1.5 text-left transition-colors"
                  onClick={() => onMenuSelect("StaffAnnotation")}
                >
                  Add staff annotation
                </button>
                <button
                  type="button"
                  className="hover:bg-surface-muted w-full rounded px-2 py-1.5 text-left transition-colors"
                  onClick={() => onMenuSelect("KeyInformation")}
                >
                  Add key information
                </button>
              </>
            )}
            {selectionMode === "none" && (
              <button
                type="button"
                className="hover:bg-surface-muted w-full rounded px-2 py-1.5 text-left transition-colors"
                onClick={() => onMenuSelect("ScoreInformation")}
              >
                Add score information
              </button>
            )}
          </div>
        ) : (
          <div className="flex flex-col gap-2">
            <div className="text-text-sub text-xs font-bold tracking-wider uppercase">
              {contextMenu.activeMenuId}
            </div>
            <input
              type="text"
              className="border-border-main bg-surface text-text-main focus:border-brand w-full rounded border px-2 py-1 text-sm focus:outline-none"
              value={contextMenu.inputValue}
              onChange={(e) => onInputChange(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === "Enter") onSubmit();
              }}
              // biome-ignore lint/a11y/noAutofocus: input focus
              autoFocus
            />
            <div className="flex justify-end gap-2">
              <button
                type="button"
                className="text-text-sub hover:text-text-main text-xs"
                onClick={onClose}
              >
                Cancel
              </button>
              <button
                type="button"
                className="bg-brand text-text-on-brand hover:bg-brand-hover rounded px-3 py-1.5 text-xs font-medium transition-colors"
                onClick={onSubmit}
              >
                Add
              </button>
            </div>
          </div>
        )}
      </div>
    )
  );
};

export default ContextMenu;
