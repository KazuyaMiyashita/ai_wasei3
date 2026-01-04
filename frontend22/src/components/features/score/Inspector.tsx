import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import type { UseScoreInteraction } from "../../../hooks/score/useScoreInteraction";
import type { UseScoreView } from "../../../hooks/score/useScoreView";
import { useNotification } from "../../../hooks/useNotification";
import { editFromXml, validateEditXml } from "../../../lib/score/mei-edit";
import { formatXml } from "../../../lib/score/mei-utils";
import type { AnnotationEntry } from "../../../types";
import { CollapsibleSection } from "../../ui/CollapsibleSection";

interface InspectorProps {
  selectedIds: UseScoreInteraction["selectedIds"];
  meiXML: UseScoreView["meiXML"];
  annotations: AnnotationEntry[];
  onEdit?: UseScoreView["edit"];
}

const Inspector = ({
  selectedIds,
  meiXML,
  annotations,
  onEdit,
}: InspectorProps) => {
  const { notify } = useNotification();
  const selectedNoteInfo = useMemo(() => {
    if (!meiXML || selectedIds.length === 0) return null;

    const id = selectedIds[0];
    const element = meiXML.querySelector(`[*|id="${id}"]`);
    if (!element) return null;

    const serializer = new XMLSerializer();
    const raw = serializer.serializeToString(element);
    return formatXml(raw) || raw;
  }, [selectedIds, meiXML]);

  const [editedXml, setEditedXml] = useState<string | null>(null);
  const [isXmlValid, setIsXmlValid] = useState(true);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const isSubmittingRef = useRef(false);

  // biome-ignore lint/correctness/useExhaustiveDependencies: Reset editedXml when selection changes.
  useEffect(() => {
    // Only reset if the selection changes to a different ID or is cleared.
    setEditedXml(null);
    setIsXmlValid(true);
    setErrorMessage(null);
    isSubmittingRef.current = false;
  }, [selectedIds]);

  const displayValue =
    editedXml !== null ? editedXml : selectedNoteInfo || "No MEI data found";

  const handleBlur = useCallback(() => {
    // If submitting, skip blur validation to prevent overwriting error state
    if (isSubmittingRef.current) return;

    // If there is already an error message (e.g. from Verovio warning), do not overwrite it with generic validation
    if (errorMessage) return;

    if (editedXml !== null && selectedIds.length > 0 && meiXML) {
      const { isValid, error } = validateEditXml(
        meiXML,
        selectedIds[0],
        editedXml,
      );
      setIsXmlValid(isValid);
      setErrorMessage(error || null);
    }
  }, [editedXml, selectedIds, meiXML, errorMessage]);

  const performEdit = useCallback(() => {
    if (editedXml !== null && selectedIds.length > 0 && meiXML && onEdit) {
      isSubmittingRef.current = true;
      const result = editFromXml(meiXML, selectedIds[0], editedXml);

      if (result.type !== "error") {
        if (result.type === "no-change") {
          notify("No changes detected", "info", "Inspector");
        } else {
          const editStatus = onEdit(result);
          if (editStatus.success) {
            setEditedXml(null);
            setIsXmlValid(true);
            setErrorMessage(null);
          } else {
            setIsXmlValid(false);
            setErrorMessage(editStatus.message || "Edit failed in Verovio");
          }
        }
      } else {
        notify(result.error || "Edit failed", "error", "Inspector");
        setIsXmlValid(false);
        setErrorMessage(result.error || "Unknown error");
      }

      // Allow blur handling again after a short delay
      setTimeout(() => {
        isSubmittingRef.current = false;
      }, 100);
    }
  }, [editedXml, selectedIds, meiXML, onEdit, notify]);

  const handleEditClick = useCallback(() => {
    performEdit();
  }, [performEdit]);

  const handleTextChange = useCallback(
    (e: React.ChangeEvent<HTMLTextAreaElement>) => {
      const nextValue = e.target.value;
      setEditedXml(nextValue);

      if (selectedIds.length > 0 && meiXML) {
        const { isValid, error } = validateEditXml(
          meiXML,
          selectedIds[0],
          nextValue,
        );
        setIsXmlValid(isValid);
        setErrorMessage(error || null);
      }
    },
    [selectedIds, meiXML],
  );

  const handleKeyDown = useCallback(
    (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
      if ((e.metaKey || e.ctrlKey) && e.key === "Enter") {
        e.preventDefault();
        // Blur first to ensure validation runs if needed, or just run validation directly
        // Here we run validation implicitly via performEdit -> editFromXml
        performEdit();
      }
    },
    [performEdit],
  );

  return (
    <>
      <CollapsibleSection
        title="Selection Details"
        contentClassName="px-4 pb-4"
      >
        {selectedIds.length > 0 ? (
          <div className="space-y-3">
            <div className="flex justify-between text-sm">
              <span className="text-text-sub">Selected ID</span>
              <span className="bg-surface-muted text-text-main border-border-sub rounded border px-1.5 font-mono text-xs">
                {selectedIds[0]}
              </span>
            </div>
            <div className="mt-2">
              <div className="flex items-center justify-between mb-1">
                <div className="text-text-sub text-xs">MEI Fragment:</div>
                <button
                  type="button"
                  onClick={handleEditClick}
                  disabled={!isXmlValid}
                  className={`bg-surface border border-border-main text-text-main cursor-pointer rounded px-2 py-0.5 text-[10px] transition-colors
                    ${!isXmlValid ? "opacity-50 cursor-not-allowed" : "hover:bg-surface-hover"}
                  `}
                >
                  Edit
                </button>
              </div>
              <textarea
                className={`bg-background text-brand min-h-32 w-full rounded border p-2 font-mono text-[10px] whitespace-pre-wrap focus:outline-none ${
                  isXmlValid && !errorMessage
                    ? "border-border-main focus:border-brand"
                    : "border-warning focus:border-warning"
                }`}
                value={displayValue}
                onChange={handleTextChange}
                onBlur={handleBlur}
                onKeyDown={handleKeyDown}
                spellCheck={false}
              />
              {!isXmlValid && errorMessage && (
                <div className="text-warning text-[10px] mt-1 wrap-break-word">
                  {errorMessage}
                </div>
              )}
              {isXmlValid && (
                <div className="text-text-muted text-[10px] mt-1 text-right">
                  Cmd+Enter to edit
                </div>
              )}
            </div>
          </div>
        ) : (
          <div className="text-text-muted py-2 text-center text-xs italic">
            Select a note to view properties
          </div>
        )}
      </CollapsibleSection>

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
    </>
  );
};

export default Inspector;
