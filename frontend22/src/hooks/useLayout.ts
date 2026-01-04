import { useState } from "react";

export type LeftPanelType = "workspace" | "server" | "templates" | "palette";
export type InspectorPosition = "right" | "bottom";

export interface UseLayout {
  isLeftSidebarOpen: boolean;
  setLeftSidebarOpen: React.Dispatch<React.SetStateAction<boolean>>;
  toggleLeftSidebar: () => void;
  isRightSidebarOpen: boolean;
  setRightSidebarOpen: React.Dispatch<React.SetStateAction<boolean>>;
  toggleRightSidebar: () => void;
  activeLeftPanel: LeftPanelType;
  setActiveLeftPanel: React.Dispatch<React.SetStateAction<LeftPanelType>>;
  inspectorPosition: InspectorPosition;
  setInspectorPosition: React.Dispatch<React.SetStateAction<InspectorPosition>>;
}

export function useLayout(): UseLayout {
  const [isLeftSidebarOpen, setLeftSidebarOpen] = useState(true);
  const [isRightSidebarOpen, setRightSidebarOpen] = useState(true);
  const [activeLeftPanel, setActiveLeftPanel] =
    useState<LeftPanelType>("workspace");
  const [inspectorPosition, setInspectorPosition] =
    useState<InspectorPosition>("right");

  return {
    isLeftSidebarOpen,
    setLeftSidebarOpen,
    toggleLeftSidebar: () => setLeftSidebarOpen((prev) => !prev),
    isRightSidebarOpen,
    setRightSidebarOpen,
    toggleRightSidebar: () => setRightSidebarOpen((prev) => !prev),
    activeLeftPanel,
    setActiveLeftPanel,
    inspectorPosition,
    setInspectorPosition,
  };
}
