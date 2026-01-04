import type { AnnotationEntry } from "../../../../types";
import { CollapsibleSection } from "../../../ui/CollapsibleSection";

interface AnnotationListProps {
  annotations: AnnotationEntry[];
}

export const AnnotationList = ({ annotations }: AnnotationListProps) => {
  return (
    <CollapsibleSection title="Annotations" contentClassName="px-4 pb-4">
      {annotations.length === 0 ? (
        <div className="text-text-muted py-2 text-center text-xs italic">
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
                    M{ann.latestPosition.measure} / B{ann.latestPosition.beat}
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
  );
};
