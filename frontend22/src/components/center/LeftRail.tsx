import {
  FilePlus,
  FolderOpen,
  Globe,
  InfoIcon,
  Pencil,
  Settings,
} from "lucide-react";
import { Toolbar } from "radix-ui";
import { cn } from "../../utils";
import type { Panel } from "./Center";

const LeftRailItem: React.FC<{
  panel: Panel;
  children: React.ReactNode;
}> = ({ panel, children }) => {
  return (
    <Toolbar.ToggleItem
      value={panel}
      className={cn(
        "ui-action-button p-2",
        "data-[state=on]:text-brand-primary",
      )}
      aria-label={panel}
    >
      {children}
    </Toolbar.ToggleItem>
  );
};

interface LeftRailProps {
  currentPanel: Panel | null;
  setCurrentPanel: (value: Panel | null) => void;
}

const LeftRail: React.FC<LeftRailProps> = ({
  currentPanel,
  setCurrentPanel,
}) => {
  return (
    <Toolbar.Root
      orientation="vertical"
      className="border-ui-border bg-ui-leftrail-bg flex h-full w-12 flex-col items-center border-r py-2"
    >
      <Toolbar.ToggleGroup
        type="single"
        value={currentPanel || ""}
        onValueChange={(val) => setCurrentPanel(val ? (val as Panel) : null)}
        className="flex h-full w-full flex-col items-center gap-y-1"
      >
        {/* 上部アイコングループ */}
        <div className="flex flex-col gap-y-1">
          <LeftRailItem panel="Workspace">
            <FolderOpen size={20} />
          </LeftRailItem>
          <LeftRailItem panel="Template">
            <FilePlus size={20} />
          </LeftRailItem>
          <LeftRailItem panel="Explorer">
            <Globe size={20} />
          </LeftRailItem>
        </div>

        {/* 罫線（水平線） */}
        <Toolbar.Separator className="bg-ui-border my-2 h-px w-6" />

        {/* 罫線下のアイコン */}
        <div className="flex flex-col gap-y-1">
          <LeftRailItem panel="Inspector">
            <InfoIcon size={20} />
          </LeftRailItem>
          <LeftRailItem panel="NotePalette">
            <Pencil size={20} />
          </LeftRailItem>
        </div>

        {/* スペーサー */}
        <div className="flex-1" />

        {/* 下部アイコングループ */}
        <div className="flex flex-col gap-y-1">
          <LeftRailItem panel="Settings">
            <Settings size={20} />
          </LeftRailItem>
        </div>
      </Toolbar.ToggleGroup>
    </Toolbar.Root>
  );
};

export default LeftRail;
