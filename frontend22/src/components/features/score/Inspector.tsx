import type { UseScoreInteraction } from "../../../hooks/score/useScoreInteraction";
import type { UseScoreView } from "../../../hooks/score/useScoreView";
import type { AnnotationEntry } from "../../../types";
import { AnnotationList } from "./inspector/AnnotationList";
import { SelectionDetails } from "./inspector/SelectionDetails";

interface InspectorProps {
  selectedIds: UseScoreInteraction["selectedIds"];
  meiXML: UseScoreView["meiXML"];
  annotations: AnnotationEntry[];
  onEdit?: UseScoreView["edit"];
  onEditorSelectionChange?: (ids: string[]) => void;
  editorSelectedIds?: string[];
}

const Inspector = ({
  selectedIds,
  meiXML,
  annotations,
  onEdit,
  onEditorSelectionChange,
  editorSelectedIds = [],
}: InspectorProps) => {
  return (
    <>
      <SelectionDetails
        selectedIds={selectedIds}
        meiXML={meiXML}
        onEdit={onEdit}
        onEditorSelectionChange={onEditorSelectionChange}
        editorSelectedIds={editorSelectedIds}
      />
      <AnnotationList annotations={annotations} />
    </>
  );
};

export default Inspector;
