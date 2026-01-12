import { CircleSmall, X } from "lucide-react";
import { useEffect, useState } from "react";
import {
  useApplication,
  useApplicationState,
} from "../../context/ApplicationContext";
import type { ActiveDocument } from "../../lib/editor/active-document";
import { cn } from "../../utils";

interface TabComponentProps {
  name: string;
  isSelected: boolean;
  isDirty: boolean;
  isPreview: boolean;
  onClickTab: () => void;
  onClickCloseButton: () => void;
  onDoubleClickTab: () => void;
}
const TabComponent: React.FC<TabComponentProps> = ({
  name,
  isSelected,
  isDirty,
  isPreview,
  onClickTab,
  onClickCloseButton,
  onDoubleClickTab,
}) => {
  return (
    <div
      role="tab" // タブであることを明示
      aria-selected={isSelected} // 選択状態を支援技術に伝える
      tabIndex={0} // キーボードでフォーカス可能にする
      className={cn(
        "items-center gap-1",
        isSelected ? "" : "max-w-32",
        "border-ui-border flex cursor-pointer flex-row border-r bg-neutral-50 p-2", // cursor-pointer added
        isSelected ? "bg-white" : "bg-neutral-50",
        isSelected
          ? "border-t-brand-primary border-t"
          : "border-t border-t-neutral-50", // 高さを揃えるための背景色と同じボーダー
      )}
      onClick={onClickTab}
      onDoubleClick={onDoubleClickTab}
      onKeyDown={(e) => {
        if (e.key === "Enter" || e.key === " ") {
          e.preventDefault();
          onClickTab();
        }
      }}
    >
      <div
        className={cn(
          "whitespace-nowrap",
          isSelected ? "" : "truncate",
          isPreview ? "text-ui-text-muted italic" : "",
        )}
      >
        {name}
      </div>

      <button
        type="button"
        aria-label="Close tab"
        className="group ui-action-button ml-2 flex h-3 w-3 items-center justify-center" // margin added
        onClick={(event) => {
          event.stopPropagation(); // 親の onClick を発火させない
          onClickCloseButton();
        }}
        onKeyDown={(e) => {
          if (e.key === "Enter" || e.key === " ") {
            e.preventDefault();
            onClickCloseButton();
          }
        }}
      >
        {/* ホバー時に隠す要素 */}
        <div className={isDirty ? "group-hover:hidden" : "hidden"}>
          <CircleSmall
            size="12"
            fill="currentColor"
            className="text-brand-primary"
          />
        </div>

        {/* ホバー時に表示する、または isDirty でない時に表示する要素 */}
        <div className={isDirty ? "hidden group-hover:block" : "block"}>
          <X size="12" />
        </div>
      </button>
    </div>
  );
};

function TabItem({
  doc,
  isSelected,
  onClick,
  onClose,
}: {
  doc: ActiveDocument;
  isSelected: boolean;
  onClick: () => void;
  onClose: () => void;
}) {
  const [isDirty, setIsDirty] = useState(doc.getState().isDirty);

  useEffect(() => {
    setIsDirty(doc.getState().isDirty);
    return doc.subscribe((state) => setIsDirty(state.isDirty));
  }, [doc]);

  const name = doc.originalDocument.path.split("/").pop() || doc.id;

  return (
    <TabComponent
      name={name}
      isSelected={isSelected}
      isDirty={isDirty}
      isPreview={false} // New-lib doesn't track isPreview yet
      onClickTab={onClick}
      onDoubleClickTab={() => {}} // Not used currently
      onClickCloseButton={onClose}
    />
  );
}

import { useMemo } from "react";

// ... (imports)

function Tabs() {
  const application = useApplication();
  const activeDocumentsMap = useApplicationState(
    (state) => state.activeDocuments,
  );
  const currentDocumentId = useApplicationState(
    (state) => state.currentDocumentId,
  );

  const activeDocuments = useMemo(
    () => Array.from(activeDocumentsMap.values()),
    [activeDocumentsMap],
  );

  const handleClose = async (doc: ActiveDocument) => {
    if (doc.getState().isDirty) {
      const confirmDiscard = confirm(
        `Changes you made to "${doc.originalDocument.path}" may not be saved.\n\nClose anyway?`,
      );
      if (!confirmDiscard) {
        return;
      }
    }
    await application.close(doc.id);
  };

  return (
    <div className="border-ui-border bg-ui-tabs-bg text-ui-tabs-text border-b text-xs select-none">
      <ul className="flex flex-row justify-start overflow-x-auto">
        {activeDocuments.map((doc) => {
          return (
            <li key={doc.id}>
              <TabItem
                doc={doc}
                isSelected={currentDocumentId === doc.id}
                onClick={() => application.activate(doc.id)}
                onClose={() => handleClose(doc)}
              />
            </li>
          );
        })}
      </ul>
    </div>
  );
}

export default Tabs;
