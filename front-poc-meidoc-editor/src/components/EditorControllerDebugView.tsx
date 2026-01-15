import { useState, useSyncExternalStore } from "react";
import type { EditorController } from "../lib/EditorController";
import type { ResilientNode } from "../lib/ResilientSyntaxTree";

export function EditorControllerDebugView({
  controller,
}: {
  controller: EditorController;
}) {
  const [activeTab, setActiveTab] = useState<"logs" | "rst" | "pm" | "cm">(
    "logs",
  );
  const [pmSubTab, setPmSubTab] = useState<"transactions" | "xml">(
    "transactions",
  );

  useSyncExternalStore(
    (callback) => controller.subscribe(callback),
    () => controller.version,
  );

  const renderRSTNode = (node: ResilientNode, depth: number) => {
    return (
      <div key={node.id} style={{ paddingLeft: depth * 10 }}>
        <span className="text-[8px] text-gray-400">{node.type}</span>{" "}
        <span className="font-bold text-blue-700">
          {node.tagName || (node.type === "Text" ? "#text" : "")}
        </span>
        {node.type === "Text" && (
          <span className="text-gray-600">
            {" "}
            "{node.textContent.slice(0, 20)}
            {node.textContent.length > 20 ? "..." : ""}"
          </span>
        )}
        <span className="text-[8px] text-gray-400">
          {" "}
          id:{node.id.slice(0, 4)} len:{node.length}
        </span>
        {node.children.map((child) => renderRSTNode(child, depth + 1))}
      </div>
    );
  };

  return (
    <div className="flex h-full w-full flex-col gap-2 bg-gray-50 p-2 font-mono text-[10px]">
      <div className="flex items-center justify-between bg-gray-200 p-1">
        <div className="flex gap-2">
          <button
            type="button"
            onClick={() => setActiveTab("logs")}
            className={`px-2 py-1 ${activeTab === "logs" ? "bg-white font-bold" : "hover:bg-gray-300"}`}
          >
            Logs
          </button>
          <button
            type="button"
            onClick={() => setActiveTab("rst")}
            className={`px-2 py-1 ${activeTab === "rst" ? "bg-white font-bold" : "hover:bg-gray-300"}`}
          >
            RST
          </button>
          <button
            type="button"
            onClick={() => setActiveTab("pm")}
            className={`px-2 py-1 ${activeTab === "pm" ? "bg-white font-bold" : "hover:bg-gray-300"}`}
          >
            PM
          </button>
          <button
            type="button"
            onClick={() => setActiveTab("cm")}
            className={`px-2 py-1 ${activeTab === "cm" ? "bg-white font-bold" : "hover:bg-gray-300"}`}
          >
            CM
          </button>
        </div>
        <div className="px-2">
          Status:{" "}
          <span
            className={
              controller.lockState === "Idle"
                ? "text-green-600"
                : "font-bold text-red-600"
            }
          >
            {controller.lockState}
          </span>
        </div>
      </div>

      <div className="flex-1 overflow-hidden border border-gray-300 bg-white">
        {activeTab === "logs" && (
          <div className="h-full overflow-auto p-1">
            {controller.logs.map((log, i) => (
              // biome-ignore lint/suspicious/noArrayIndexKey: order is stable
              <div key={i} className="border-b border-gray-100 py-1">
                <span className="text-gray-400">
                  [{new Date(log.timestamp).toISOString().slice(11, 23)}]
                </span>{" "}
                <span
                  className={`inline-block w-8 font-bold ${log.source === "CM" ? "text-blue-600" : log.source === "PM" ? "text-purple-600" : "text-gray-600"}`}
                >
                  {log.source}
                </span>
                <span
                  className={`mx-1 inline-block w-12 ${log.type === "Lock" ? "text-red-500" : "text-green-600"}`}
                >
                  {log.type}
                </span>
                <span>{log.details}</span>
              </div>
            ))}
          </div>
        )}
        {activeTab === "rst" && (
          <div className="h-full overflow-auto p-1 whitespace-pre">
            {renderRSTNode(controller.rst.root, 0)}
          </div>
        )}
        {activeTab === "pm" && (
          <div className="flex h-full flex-col">
            <div className="flex border-b bg-gray-100 font-bold">
              <button
                type="button"
                onClick={() => setPmSubTab("transactions")}
                className={`px-2 py-1 ${pmSubTab === "transactions" ? "bg-white text-black" : "text-gray-500"}`}
              >
                Trs
              </button>
              <button
                type="button"
                onClick={() => setPmSubTab("xml")}
                className={`px-2 py-1 ${pmSubTab === "xml" ? "bg-white text-black" : "text-gray-500"}`}
              >
                XML
              </button>
            </div>
            {pmSubTab === "transactions" ? (
              <div className="flex-1 overflow-auto p-1 whitespace-nowrap">
                {[...controller.proseMirrorTransactions]
                  .reverse()
                  .map((tr, i) => {
                    const index = controller.proseMirrorTransactions.length - i;
                    const steps =
                      tr.steps.length > 0
                        ? JSON.stringify(tr.steps.map((s) => s.toJSON()))
                        : "";
                    return (
                      // biome-ignore lint/suspicious/noArrayIndexKey: order is stable
                      <div key={i} className="py-0.5">
                        #{index} {tr.selection.from}-{tr.selection.to} {steps}
                      </div>
                    );
                  })}
              </div>
            ) : (
              <div className="flex-1 overflow-auto p-1 break-all whitespace-pre-wrap">
                {controller.proseMirrorXML}
              </div>
            )}
          </div>
        )}
        {activeTab === "cm" && (
          <div className="h-full overflow-auto p-1">
            {[...controller.codeMirrorTransactions].reverse().map((tr, i) => (
              // biome-ignore lint/suspicious/noArrayIndexKey: order is stable
              <div key={i} className="py-0.5">
                Changes: {tr.changes.desc.length}
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
