import { ChevronDown } from "lucide-react";
import type React from "react";
import { useMemo, useState } from "react";
import type { AnnotationEntry } from "../hooks/useScoreEditor";

interface InspectorProps {
  selectedIds: string[];
  meiXML?: Document;
  annotations: AnnotationEntry[];
}

const CollapsibleSection: React.FC<{
  title: string;
  children: React.ReactNode;
  defaultOpen?: boolean;
}> = ({ title, children, defaultOpen = true }) => {
  const [isOpen, setIsOpen] = useState(defaultOpen);

  return (
    <div className="border-border-main border-b last:border-0">
      <button
        type="button"
        className="hover:bg-surface-header flex w-full items-center justify-between p-3 transition-colors"
        onClick={() => setIsOpen(!isOpen)}
      >
        <span className="text-text-muted text-[10px] font-bold tracking-wider uppercase">
          {title}
        </span>
        <ChevronDown
          className={`text-text-muted h-3 w-3 transition-transform ${
            isOpen ? "rotate-180" : ""
          }`}
        />
      </button>
      {isOpen && <div className="px-4 pb-4">{children}</div>}
    </div>
  );
};

const Inspector: React.FC<InspectorProps> = ({
  selectedIds,
  meiXML,
  annotations,
}) => {
  const selectedNoteInfo = useMemo(() => {
    if (!meiXML || selectedIds.length === 0) return null;

    const id = selectedIds[0];
    const element = meiXML.querySelector(`[*|id="${id}"]`);
    if (!element) return null;

    const serializer = new XMLSerializer();
    return serializer.serializeToString(element);
  }, [selectedIds, meiXML]);

  return (
    <div className="bg-sidebar text-text-main flex h-full w-full flex-col overflow-hidden">
      <div className="border-border-main bg-surface-header/50 text-text-sub flex h-11 shrink-0 items-center border-b px-3 text-[10px] font-bold tracking-wider uppercase">
        Properties
      </div>

      <div className="flex-1 overflow-y-auto">
        <CollapsibleSection title="Selection Details">
          {selectedIds.length > 0 ? (
            <div className="space-y-3">
              <div className="flex justify-between text-sm">
                <span className="text-text-sub">Selected ID</span>
                <span className="bg-surface-muted text-text-main border-border-sub rounded border px-1.5 font-mono text-xs">
                  {selectedIds[0]}
                </span>
              </div>
              <div className="mt-2">
                <div className="text-text-sub mb-1 text-xs">MEI Fragment:</div>
                <pre className="bg-background border-border-main text-brand max-h-48 overflow-x-auto rounded border p-2 font-mono text-[10px] whitespace-pre-wrap">
                  {selectedNoteInfo || "No MEI data found"}
                </pre>
              </div>
            </div>
          ) : (
            <div className="text-text-muted py-2 text-center text-sm italic">
              Select a note to view properties
            </div>
          )}
        </CollapsibleSection>

        <CollapsibleSection title="Annotations">
          {annotations.length === 0 ? (
            <div className="text-text-muted py-2 text-center text-sm italic">
              No annotations yet
            </div>
          ) : (
            <div className="space-y-2">
              {annotations.map((ann) => (
                <div
                  key={ann.id}
                  className="border-border-main bg-surface-muted/30 rounded border p-2 text-xs"
                >
                  <div className="mb-1 flex items-start justify-between">
                    <span className="text-brand font-bold">[{ann.menuId}]</span>
                    {ann.latestPosition && (
                      <span className="text-text-muted text-[10px]">
                        M{ann.latestPosition.measure} / B
                        {ann.latestPosition.beat}
                      </span>
                    )}
                  </div>
                  <div className="text-text-main mb-1 font-medium">
                    {ann.inputValue}
                  </div>
                  <div className="text-text-muted truncate text-[10px]">
                    IDs: {ann.selectedIds.join(", ")}
                  </div>
                </div>
              ))}
            </div>
          )}
        </CollapsibleSection>
      </div>
    </div>
  );
};

export default Inspector;
