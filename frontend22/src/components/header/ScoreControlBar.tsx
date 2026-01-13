import { Code, Columns2, Edit, Eye, File, ZoomIn, ZoomOut } from "lucide-react";
import { Toolbar } from "radix-ui";
import {
  useApplication,
  useApplicationState,
} from "../../context/ApplicationContext";

const EditModeControls = () => {
  const application = useApplication();
  const editMode = useApplicationState((state) => state.viewState.editMode);

  return (
    <Toolbar.ToggleGroup
      type="single"
      value={editMode ? "edit" : "read"}
      className="flex shrink-0 gap-1"
      onValueChange={(val) => {
        if (val) application.setEditMode(val === "edit");
      }}
    >
      <Toolbar.ToggleItem
        value="read"
        className="ui-action-button data-[state=on]:bg-ui-bg-subtle data-[state=on]:text-brand-primary flex items-center gap-1 px-2 py-1"
      >
        <Eye size={14} />
        <span className="text-xs font-medium">閲覧</span>
      </Toolbar.ToggleItem>
      <Toolbar.ToggleItem
        value="edit"
        className="ui-action-button data-[state=on]:bg-ui-bg-subtle data-[state=on]:text-brand-primary flex items-center gap-1 px-2 py-1"
      >
        <Edit size={14} />
        <span className="text-xs font-medium">編集</span>
      </Toolbar.ToggleItem>
    </Toolbar.ToggleGroup>
  );
};

const LayoutModeControls = () => {
  const application = useApplication();
  const layoutMode = useApplicationState((state) => state.viewState.layoutMode);

  return (
    <Toolbar.ToggleGroup
      type="single"
      defaultValue={layoutMode}
      className="flex shrink-0 gap-1"
    >
      <Toolbar.ToggleItem
        value="document"
        className="ui-action-button data-[state=on]:bg-ui-bg-subtle data-[state=on]:text-brand-primary p-1.5"
        onClick={() => application.setLayoutMode("document")}
      >
        <File size={16} />
      </Toolbar.ToggleItem>
      <Toolbar.ToggleItem
        value="code"
        className="ui-action-button data-[state=on]:bg-ui-bg-subtle data-[state=on]:text-brand-primary p-1.5"
        onClick={() => application.setLayoutMode("code")}
      >
        <Code size={16} />
      </Toolbar.ToggleItem>
      <Toolbar.ToggleItem
        value="split"
        className="ui-action-button data-[state=on]:bg-ui-bg-subtle data-[state=on]:text-brand-primary p-1.5"
        onClick={() => application.setLayoutMode("split")}
      >
        <Columns2 size={16} />
      </Toolbar.ToggleItem>
    </Toolbar.ToggleGroup>
  );
};

const ZoomControls = () => {
  const application = useApplication();
  const scale = useApplicationState((state) => state.viewState.scale);

  const adjustZoom = (delta: number) => {
    const next = scale + delta;
    const clamped = Math.min(Math.max(next, 10), 400);
    application.setVerifyScale(clamped);
  };

  return (
    <div className="flex shrink-0 items-center px-1">
      <button
        type="button"
        className="ui-action-button p-1"
        onClick={() => adjustZoom(-10)}
        aria-label="Zoom out"
        disabled={scale <= 10}
      >
        <ZoomOut size={16} />
      </button>

      <span className="text-ui-text-muted hidden w-12 text-center text-xs font-semibold tabular-nums lg:block">
        {scale}%
      </span>

      <button
        type="button"
        className="ui-action-button p-1"
        onClick={() => adjustZoom(10)}
        aria-label="Zoom in"
        disabled={scale >= 400}
      >
        <ZoomIn size={16} />
      </button>
    </div>
  );
};

export default function ScoreControlBar() {
  return (
    <Toolbar.Root className="bg-ui-bg-base border-ui-border flex h-9 items-center gap-1 rounded-lg border px-1 shadow-sm">
      <EditModeControls />

      <Toolbar.Separator className="bg-ui-border mx-1 h-5 w-px shrink-0" />

      <LayoutModeControls />

      <Toolbar.Separator className="bg-ui-border mx-1 h-5 w-px shrink-0" />

      <ZoomControls />
    </Toolbar.Root>
  );
}
