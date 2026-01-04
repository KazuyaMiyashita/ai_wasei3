import type React from "react";
import { useState } from "react";
import type { UseWorkspace } from "../../../hooks/workspace/useWorkspace";
import { DropdownMenu } from "../../ui/DropdownMenu";

interface HeaderControlsProps {
  onLocalFileAdd: UseWorkspace["handleLocalFileAdd"];
  onUndo: () => void;
  onRedo: () => void;
  onSave: () => void;
  canUndo: boolean;
  canRedo: boolean;
  onClearWorkspace: () => void;
}

export const HeaderControls: React.FC<HeaderControlsProps> = ({
  onLocalFileAdd,
  onUndo,
  onRedo,
  onSave,
  canUndo,
  canRedo,
  onClearWorkspace,
}) => {
  const [isFileMenuOpen, setIsFileMenuOpen] = useState(false);
  const [isEditMenuOpen, setIsEditMenuOpen] = useState(false);

  const handleLocalFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    onLocalFileAdd(file);
    e.target.value = "";
    setIsFileMenuOpen(false);
  };

  return (
    <div className="flex items-center gap-4">
      <div className="flex items-center gap-2 select-none">
        <div className="bg-brand text-text-on-brand flex h-8 w-8 items-center justify-center rounded font-bold shadow-sm">
          M
        </div>
        <div className="text-text-main hidden text-lg font-bold tracking-tight sm:block">
          Music Analysis
        </div>
      </div>

      <nav className="ml-6 flex gap-1">
        {/* File Menu */}
        <DropdownMenu
          isOpen={isFileMenuOpen}
          onOpenChange={setIsFileMenuOpen}
          trigger={
            <button
              type="button"
              className={`hover:bg-surface-hover text-text-sub cursor-pointer rounded px-3 py-1.5 text-sm font-medium transition-colors ${
                isFileMenuOpen ? "bg-surface-hover text-text-main" : ""
              }`}
            >
              File
            </button>
          }
        >
          <label className="text-text-main hover:bg-surface-muted flex w-full cursor-pointer items-center justify-between px-4 py-2 text-left text-sm transition-colors">
            <span className="flex items-center gap-2">Open Local File</span>
            <input
              type="file"
              className="hidden"
              accept=".mei,.xml"
              onChange={handleLocalFileChange}
            />
          </label>
          <button
            type="button"
            onClick={() => {
              onSave();
              setIsFileMenuOpen(false);
            }}
            className="text-text-main hover:bg-surface-muted flex w-full cursor-pointer items-center justify-between px-4 py-2 text-left text-sm transition-colors"
          >
            Save Score
          </button>
          <div className="border-border-main my-1 border-t" />
          <button
            type="button"
            onClick={() => {
              onClearWorkspace();
              setIsFileMenuOpen(false);
            }}
            className="text-error hover:bg-surface-muted flex w-full cursor-pointer items-center justify-between px-4 py-2 text-left text-sm transition-colors"
          >
            Clear Workspace
          </button>
        </DropdownMenu>

        {/* Edit Menu */}
        <DropdownMenu
          isOpen={isEditMenuOpen}
          onOpenChange={setIsEditMenuOpen}
          trigger={
            <button
              type="button"
              className={`hover:bg-surface-hover text-text-sub cursor-pointer rounded px-3 py-1.5 text-sm font-medium transition-colors ${
                isEditMenuOpen ? "bg-surface-hover text-text-main" : ""
              }`}
            >
              Edit
            </button>
          }
        >
          <button
            type="button"
            onClick={() => {
              onUndo();
              setIsEditMenuOpen(false);
            }}
            disabled={!canUndo}
            className={`flex w-full cursor-pointer items-center justify-between px-4 py-2 text-left text-sm transition-colors ${
              canUndo
                ? "text-text-main hover:bg-surface-muted"
                : "text-text-muted cursor-not-allowed"
            }`}
          >
            Undo
          </button>
          <button
            type="button"
            onClick={() => {
              onRedo();
              setIsEditMenuOpen(false);
            }}
            disabled={!canRedo}
            className={`flex w-full cursor-pointer items-center justify-between px-4 py-2 text-left text-sm transition-colors ${
              canRedo
                ? "text-text-main hover:bg-surface-muted"
                : "text-text-muted cursor-not-allowed"
            }`}
          >
            Redo
          </button>
        </DropdownMenu>

        <button
          type="button"
          className="hover:bg-surface-hover text-text-sub cursor-pointer rounded px-3 py-1.5 text-sm font-medium transition-colors"
        >
          View
        </button>
      </nav>
    </div>
  );
};
