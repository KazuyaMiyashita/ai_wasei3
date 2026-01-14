import { useState } from "react";
import type { EditorController } from "../hooks/useEditorController";

export function EditorControllerViewer({
  controller,
}: {
  controller: EditorController;
}) {
  const [pmTab, setPmTab] = useState<"transactions" | "xml">("transactions");

  return (
    <div className="flex h-full w-full flex-row gap-2 bg-gray-50 p-2 font-mono text-[10px]">
      <div className="flex w-1/2 flex-col overflow-hidden border border-gray-300 bg-white">
        <div className="flex border-b bg-gray-100 font-bold">
          <button
            type="button"
            onClick={() => setPmTab("transactions")}
            className={`px-2 py-1 ${pmTab === "transactions" ? "bg-white text-black" : "text-gray-500 hover:bg-gray-200"}`}
          >
            Transactions ({controller.proseMirrorTransactions.length})
          </button>
          <button
            type="button"
            onClick={() => setPmTab("xml")}
            className={`px-2 py-1 ${pmTab === "xml" ? "bg-white text-black" : "text-gray-500 hover:bg-gray-200"}`}
          >
            Current XML
          </button>
        </div>

        {pmTab === "transactions" ? (
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
        ) : (
          <div className="flex-1 overflow-auto p-1 break-all whitespace-pre-wrap">
            {controller.proseMirrorXML || "(No content)"}
          </div>
        )}
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
