import { useState } from "react";
import { Group, Panel as ResizablePanel } from "react-resizable-panels";
import LeftPanel from "./LeftPanel";
import LeftRail from "./LeftRail";
import Main from "./Main";
import Tabs from "./Tabs";

export type Panel =
  | "Workspace"
  | "Explorer"
  | "Template"
  | "Inspector"
  | "NotePalette"
  | "Settings";

function Center() {
  const [currentPanel, setCurrentPanel] = useState<Panel | null>("Workspace");

  return (
    <div className="flex flex-1 overflow-hidden">
      <LeftRail currentPanel={currentPanel} setCurrentPanel={setCurrentPanel} />

      <Group orientation="horizontal">
        {currentPanel && (
          <ResizablePanel
            collapsible={true}
            collapsedSize={0}
            defaultSize={"20%"}
            minSize={"100px"}
            maxSize={"50%"}
            className="bg-ui-sidepanel-bg border-ui-border hover:border-ui-border-hover border-r"
          >
            {/* <NotePalette /> */}
            <LeftPanel currentPanel={currentPanel} />
          </ResizablePanel>
        )}
        {/* Main Content (Score Area) */}
        <ResizablePanel>
          <div className="flex h-full flex-col">
            <Tabs />
            <Main />
          </div>
        </ResizablePanel>
      </Group>
    </div>
  );
}

export default Center;
