import { Code, Columns2, File, ZoomIn, ZoomOut } from "lucide-react";
import { Toolbar } from "radix-ui";
import {
  useApplication,
  useApplicationState,
} from "../../context/ApplicationContext";

const ViewModeControls = () => {
  const application = useApplication();
  const viewMode = useApplicationState((state) => state.viewState.viewMode);

  return (
    <Toolbar.ToggleGroup
      type="single"
      defaultValue={viewMode}
      className="flex shrink-0 gap-1"
    >
      <Toolbar.ToggleItem
        value="document"
        className="ui-action-button data-[state=on]:bg-ui-bg-subtle data-[state=on]:text-brand-primary p-1.5"
        onClick={() => application.setViewMode("document")}
      >
        <File size={16} />
      </Toolbar.ToggleItem>
      <Toolbar.ToggleItem
        value="code"
        className="ui-action-button data-[state=on]:bg-ui-bg-subtle data-[state=on]:text-brand-primary p-1.5"
        onClick={() => application.setViewMode("code")}
      >
        <Code size={16} />
      </Toolbar.ToggleItem>
      <Toolbar.ToggleItem
        value="split"
        className="ui-action-button data-[state=on]:bg-ui-bg-subtle data-[state=on]:text-brand-primary p-1.5"
        onClick={() => application.setViewMode("split")}
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
        disabled={scale <= 10} // 最小値で無効化
      >
        <ZoomOut size={16} />
      </button>

      {/* 数値をステートと連動 */}
      <span className="text-ui-text-muted hidden w-12 text-center text-xs font-semibold tabular-nums lg:block">
        {scale}%
      </span>

      <button
        type="button"
        className="ui-action-button p-1"
        onClick={() => adjustZoom(10)}
        aria-label="Zoom in"
        disabled={scale >= 400} // 最大値で無効化
      >
        <ZoomIn size={16} />
      </button>
    </div>
  );
};

// --- メインコンポーネント ---

export default function ScoreControlBar() {
  return (
    <Toolbar.Root className="bg-ui-bg-base border-ui-border flex h-9 items-center gap-1 rounded-lg border px-1 shadow-sm">
      <ViewModeControls />

      <Toolbar.Separator className="bg-ui-border mx-1 h-5 w-px shrink-0" />

      <ZoomControls />
    </Toolbar.Root>
  );
}
