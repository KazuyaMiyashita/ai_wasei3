import type { Panel } from "./Center";
import Resources from "./panels/Explorer";
import Inspector from "./panels/Inspector";
import NotePalette from "./panels/NotePalette";
import Settings from "./panels/Settings";
import Template from "./panels/Template";
import Workspace from "./panels/Workspace";

interface LeftPanelProps {
  currentPanel: Panel;
}

const LeftPanel: React.FC<LeftPanelProps> = ({ currentPanel }) => {
  if (currentPanel === "Workspace") {
    return <Workspace />;
  } else if (currentPanel === "Template") {
    return <Template />;
  } else if (currentPanel === "Explorer") {
    return <Resources />;
  } else if (currentPanel === "Inspector") {
    return <Inspector />;
  } else if (currentPanel === "NotePalette") {
    return <NotePalette />;
  } else if (currentPanel === "Settings") {
    return <Settings />;
  }
  return null;
};

export default LeftPanel;
