import { useApplicationState } from "../../../context/ApplicationContext";

const Inspector = () => {
  const selectedIds = useApplicationState(
    (state) => state.selection.selectedIds,
  );
  const editorSelectedIds = useApplicationState(
    (state) => state.selection.editorSelectedIds,
  );

  return (
    <div className="ui-panel">
      <div className="ui-panel-section">
        <div className="ui-panel-title">Inspector</div>
        <div className="flex flex-col gap-4 font-mono text-xs break-all">
          <div className="flex flex-col gap-1">
            <span className="text-ui-text-muted font-semibold">
              Score Selection
            </span>
            <div className="bg-ui-bg-subtle text-ui-text-main rounded p-2">
              {selectedIds.length > 0 ? selectedIds.join(", ") : "(none)"}
            </div>
          </div>
          <div className="flex flex-col gap-1">
            <span className="text-ui-text-muted font-semibold">
              Editor Selection
            </span>
            <div className="bg-ui-bg-subtle text-ui-text-main rounded p-2">
              {editorSelectedIds.length > 0
                ? editorSelectedIds.join(", ")
                : "(none)"}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Inspector;
