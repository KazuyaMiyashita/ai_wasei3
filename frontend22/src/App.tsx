import {
  CloudDownload,
  EllipsisVertical,
  FilePlus,
  Folder,
  Info,
  PanelBottom,
  PanelRight,
  Pencil,
} from "lucide-react";
import { useState } from "react";
import ScorePlayer from "./components/features/audio/ScorePlayer";
import Inspector from "./components/features/score/Inspector";
import NotePalette from "./components/features/score/NotePalette";
import ScoreDisplay from "./components/features/score/ScoreDisplay";
import { ScoreModeToggle } from "./components/features/score/ScoreModeToggle";
import { ScoreToolsMenu } from "./components/features/score/ScoreToolsMenu";
import { ScoreZoomControl } from "./components/features/score/ScoreZoomControl";
import Explorer from "./components/features/workspace/Explorer";
import { HeaderControls } from "./components/features/workspace/HeaderControls";
import TabBar from "./components/features/workspace/TabBar";
import ContextMenu from "./components/layout/ContextMenu";
import Header from "./components/layout/Header";
import { Layout } from "./components/layout/Layout";
import StatusBar from "./components/layout/StatusBar";
import { DropdownMenu } from "./components/ui/DropdownMenu";
import { IconButton } from "./components/ui/IconButton";
import { SidebarPanel } from "./components/ui/SidebarPanel";
import { useApi } from "./hooks/api/useApi";
import { useScoreInteraction } from "./hooks/score/useScoreInteraction";
import { useScoreView } from "./hooks/score/useScoreView";
import { type LeftPanelType, useLayout } from "./hooks/useLayout";
import { useNotification } from "./hooks/useNotification";
import { useWorkspace } from "./hooks/workspace/useWorkspace";

import "./styles/score-viewer.css";

function App() {
  const workspace = useWorkspace();
  const layout = useLayout();
  const { lastLog } = useNotification();
  const { isConnected } = useApi();
  const [isInspectorMenuOpen, setIsInspectorMenuOpen] = useState(false);
  const [editorSelectedIds, setEditorSelectedIds] = useState<string[]>([]);

  const handleScoreUpdate = (newMei: string) => {
    if (workspace.activeTabPath) {
      workspace.updateScore(workspace.activeTabPath, newMei);
    }
  };

  const viewSettings = useScoreView(
    workspace.activeTab?.current.mei,
    handleScoreUpdate,
  );

  const interaction = useScoreInteraction(
    viewSettings.meiXML,
    workspace.activeTabPath,
    workspace.addAnnotation,
  );

  const annotations = workspace.activeTab?.current.annotations || [];

  const handleActivityBarClick = (panel: LeftPanelType) => {
    if (layout.activeLeftPanel === panel) {
      layout.toggleLeftSidebar();
    } else {
      layout.setActiveLeftPanel(panel);
      layout.setLeftSidebarOpen(true);
    }
  };

  const renderActivityBarItem = (
    panel: LeftPanelType,
    icon: React.ReactNode,
    title: string,
  ) => {
    const isActive =
      layout.activeLeftPanel === panel && layout.isLeftSidebarOpen;
    return (
      <button
        type="button"
        onClick={() => handleActivityBarClick(panel)}
        title={title}
        className="group relative flex w-full justify-center py-3 focus:outline-none"
      >
        <div
          className={`transition-colors ${
            isActive ? "text-brand" : "text-text-muted hover:text-text-main"
          }`}
        >
          {icon}
        </div>
        {isActive && (
          <div className="bg-brand absolute top-0 bottom-0 left-0 w-0.75 rounded-r-sm" />
        )}
      </button>
    );
  };

  return (
    <Layout
      isLeftSidebarOpen={layout.isLeftSidebarOpen}
      isRightSidebarOpen={layout.isRightSidebarOpen}
      inspectorPosition={layout.inspectorPosition}
      activityBar={
        <>
          {renderActivityBarItem(
            "workspace",
            <Folder className="h-6 w-6" />,
            "Workspace",
          )}
          {renderActivityBarItem(
            "server",
            <CloudDownload className="h-6 w-6" />,
            "Server Scores",
          )}
          {renderActivityBarItem(
            "templates",
            <FilePlus className="h-6 w-6" />,
            "Templates",
          )}
          <hr className="border-border-main mx-auto my-2 w-8" />
          {renderActivityBarItem(
            "palette",
            <Pencil className="h-6 w-6" />,
            "Note Palette",
          )}
        </>
      }
      header={
        <Header
          workspaceControls={
            <HeaderControls
              onLocalFileAdd={workspace.handleLocalFileAdd}
              onUndo={workspace.undo}
              onRedo={workspace.redo}
              canUndo={workspace.canUndo}
              canRedo={workspace.canRedo}
              onSave={() =>
                workspace.activeTabPath &&
                workspace.handleTabSave(workspace.activeTabPath)
              }
              onClearWorkspace={workspace.clearWorkspace}
            />
          }
          scoreTools={<ScoreToolsMenu viewSettings={viewSettings} />}
          mainContent={
            <>
              <ScoreModeToggle
                mode={interaction.interactionMode}
                setMode={interaction.setInteractionMode}
              />
              <div className="w-full max-w-md">
                <ScorePlayer meiXML={viewSettings.meiXML} />
              </div>
            </>
          }
          viewControls={
            <>
              <ScoreZoomControl
                scale={viewSettings.scale}
                onZoomIn={viewSettings.handleZoomIn}
                onZoomOut={viewSettings.handleZoomOut}
              />
              <IconButton
                onClick={layout.toggleRightSidebar}
                isActive={false}
                className={
                  !layout.isRightSidebarOpen
                    ? "text-text-muted"
                    : "text-text-main"
                }
                title="Toggle Inspector"
                icon={<Info className="h-5 w-5" />}
              />
            </>
          }
        />
      }
      leftSidebar={
        <SidebarPanel
          title={
            layout.activeLeftPanel === "workspace"
              ? "Explorer: Workspace"
              : layout.activeLeftPanel === "server"
                ? "Explorer: Server"
                : layout.activeLeftPanel === "templates"
                  ? "Explorer: Templates"
                  : "Note Palette"
          }
          className="border-r border-border-main"
        >
          {layout.activeLeftPanel === "palette" ? (
            <NotePalette />
          ) : (
            <Explorer
              scoreList={workspace.scoreList}
              localScores={workspace.localScores}
              currentFile={workspace.activeTabPath}
              onFileSelect={workspace.handleFileSelect}
              onFileDoubleClick={workspace.handleFileDoubleClick}
              view={
                layout.activeLeftPanel === "workspace"
                  ? "workspace"
                  : layout.activeLeftPanel === "server"
                    ? "server"
                    : "templates"
              }
            />
          )}
        </SidebarPanel>
      }
      rightSidebar={
        <SidebarPanel
          title="Inspector"
          className={
            layout.inspectorPosition === "right"
              ? "border-l border-border-main"
              : "border-t border-border-main"
          }
          headerActions={
            <DropdownMenu
              isOpen={isInspectorMenuOpen}
              onOpenChange={setIsInspectorMenuOpen}
              align="right"
              trigger={
                <button
                  type="button"
                  className={`hover:text-text-main cursor-pointer transition-colors ${isInspectorMenuOpen ? "text-text-main" : "text-text-muted"}`}
                >
                  <EllipsisVertical className="h-4 w-4" />
                </button>
              }
            >
              <button
                type="button"
                onClick={() => {
                  layout.setInspectorPosition("right");
                  setIsInspectorMenuOpen(false);
                }}
                className={`flex w-full cursor-pointer items-center gap-2 px-4 py-2 text-left text-sm transition-colors ${
                  layout.inspectorPosition === "right"
                    ? "bg-brand-sub text-brand"
                    : "text-text-main hover:bg-surface-muted"
                }`}
              >
                <PanelRight className="h-4 w-4" />
                <span>Panel Right</span>
              </button>
              <button
                type="button"
                onClick={() => {
                  layout.setInspectorPosition("bottom");
                  setIsInspectorMenuOpen(false);
                }}
                className={`flex w-full cursor-pointer items-center gap-2 px-4 py-2 text-left text-sm transition-colors ${
                  layout.inspectorPosition === "bottom"
                    ? "bg-brand-sub text-brand"
                    : "text-text-main hover:bg-surface-muted"
                }`}
              >
                <PanelBottom className="h-4 w-4" />
                <span>Panel Bottom</span>
              </button>
            </DropdownMenu>
          }
        >
          <Inspector
            selectedIds={interaction.selectedIds}
            meiXML={viewSettings.meiXML}
            annotations={annotations}
            onEdit={viewSettings.edit}
            onEditorSelectionChange={setEditorSelectedIds}
            editorSelectedIds={editorSelectedIds}
          />
        </SidebarPanel>
      }
      tabBar={
        <TabBar
          tabs={workspace.tabs}
          activeTabPath={workspace.activeTabPath}
          onTabSelect={workspace.handleTabSelect}
          onTabClose={workspace.handleTabClose}
        />
      }
      mainContent={
        <ScoreDisplay
          loading={viewSettings.loading}
          svgData={viewSettings.svgData}
          meiXML={viewSettings.meiXML}
          selectedIds={interaction.selectedIds}
          onSelectionChange={interaction.handleSelectionChange}
          onContextMenu={interaction.handleContextMenu}
          editorSelectedIds={editorSelectedIds}
        />
      }
      statusBar={
        <StatusBar
          latestPosition={interaction.latestPosition}
          loading={viewSettings.loading}
          lastLog={lastLog}
          isConnected={isConnected}
        />
      }
      contextMenu={
        <ContextMenu
          contextMenu={interaction.contextMenu}
          selectionMode={interaction.selectionMode}
          onMenuSelect={interaction.handleMenuSelect}
          onInputChange={interaction.handleContextMenuInputChange}
          onSubmit={interaction.handleAnnotationSubmit}
          onClose={interaction.closeContextMenu}
        />
      }
    />
  );
}

export default App;
