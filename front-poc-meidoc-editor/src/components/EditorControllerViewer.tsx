import type { EditorController } from "../hooks/useEditorController";

export function EditorControllerViewer({
  controller,
}: {
  controller: EditorController;
}) {
  return (
    <div className="flex h-full w-full flex-row gap-2 bg-gray-50 p-2 font-mono text-[10px]">
      <div className="flex w-1/2 flex-col overflow-hidden border border-gray-300 bg-white">
        <div className="border-b bg-gray-100 p-1 font-bold">
          ProseMirror ({controller.proseMirrorTransactions.length})
        </div>
        <div className="flex-1 overflow-auto p-1 whitespace-nowrap">
          {[...controller.proseMirrorTransactions].reverse().map((tr, i) => {
            const index = controller.proseMirrorTransactions.length - i;
            const steps =
              tr.steps.length > 0
                ? JSON.stringify(tr.steps.map((s) => s.toJSON()))
                : "";

            return (
              // biome-ignore lint/suspicious/noArrayIndexKey: Order is stable
              <div key={i} className="py-0.5">
                #{index} Select: {tr.selection.from}-{tr.selection.to}{" "}
                {steps && `(${steps})`}
              </div>
            );
          })}
        </div>
      </div>
      <div className="flex w-1/2 flex-col overflow-hidden border border-gray-300 bg-white">
        <div className="border-b bg-gray-100 p-1 font-bold">
          CodeMirror ({controller.codeMirrorTransactions.length})
        </div>
        <div className="flex-1 overflow-auto p-1 whitespace-nowrap">
          {[...controller.codeMirrorTransactions].reverse().map((tr, i) => {
            const index = controller.codeMirrorTransactions.length - i;
            const changes: string[] = [];
            if (!tr.changes.empty) {
              tr.changes.iterChanges((fromA, toA, _fromB, _toB, inserted) => {
                changes.push(
                  `${fromA}-${toA}->"${inserted.toString().replace(/\n/g, "\\n")}"`,
                );
              });
            }
            const changesStr =
              changes.length > 0 ? `(${changes.join(", ")})` : "";

            return (
              // biome-ignore lint/suspicious/noArrayIndexKey: Order is stable
              <div key={i} className="py-0.5">
                #{index} Select: {tr.selection?.main.from}-
                {tr.selection?.main.to} {changesStr}
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
}
