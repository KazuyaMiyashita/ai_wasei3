import { useRef, useState } from "react";
import Explorer from "./components/Explorer";
import Header from "./components/Header";
import Inspector from "./components/Inspector";
import NotePalette from "./components/NotePalette";
import ResizableSidebar from "./components/ResizableSidebar";
import ScoreDisplay from "./components/ScoreDisplay";
import StatusBar from "./components/StatusBar";
import TabBar from "./components/TabBar";
import { useScoreEditor } from "./hooks/useScoreEditor";
import "./styles/score-viewer.css";

function App() {
  const {
    scoreList,
    localScores,
    currentFile,
    meiXML,
    svgData,
    scale,
    loading,
    isPlaying,
    togglePlayback,
    stopPlayback,
    selectionMode,
    selectedIds,
    latestPosition,
    handleSelectionChange,
    handleFileSelect,
    handleFileDoubleClick,
    handleLocalFileAdd,
    handleZoomIn,
    handleZoomOut,
    handlePartwise,
    annotations,
    contextMenu,
    handleContextMenu,
    closeContextMenu,
    handleMenuSelect,
    handleAnnotationSubmit,
    handleContextMenuInputChange,
    tabs,
    activeTabPath,
    handleTabSelect,
    handleTabClose,
    interactionMode,
    setInteractionMode,
  } = useScoreEditor();

  const [isLeftSidebarOpen, setLeftSidebarOpen] = useState(true);
  const [isRightSidebarOpen, setRightSidebarOpen] = useState(true);

  const scrollContainerRef = useRef<HTMLDivElement>(null);

  return (
    <div className="bg-background text-text-main flex h-screen w-screen flex-col overflow-hidden font-sans">
      {/* 1. Header */}
      <Header
        isPlaying={isPlaying}
        onTogglePlayback={togglePlayback}
        onStopPlayback={stopPlayback}
        meiXML={meiXML}
        scale={scale}
        onZoomIn={handleZoomIn}
        onZoomOut={handleZoomOut}
        onPartwise={handlePartwise}
        isLeftSidebarOpen={isLeftSidebarOpen}
        onToggleLeftSidebar={() => setLeftSidebarOpen(!isLeftSidebarOpen)}
        isRightSidebarOpen={isRightSidebarOpen}
        onToggleRightSidebar={() => setRightSidebarOpen(!isRightSidebarOpen)}
        interactionMode={interactionMode}
        onSetInteractionMode={setInteractionMode}
      />

      <div className="flex flex-1 overflow-hidden">
        {/* 2. Left Sidebar (Explorer or Palette) */}
        <ResizableSidebar
          side="left"
          isOpen={isLeftSidebarOpen}
          initialWidth={256}
          minWidth={200}
        >
          {interactionMode === "select" ? (
            <Explorer
              scoreList={scoreList}
              localScores={localScores}
              currentFile={currentFile}
              onFileSelect={handleFileSelect}
              onFileDoubleClick={handleFileDoubleClick}
              onLocalFileAdd={handleLocalFileAdd}
            />
          ) : (
            <NotePalette />
          )}
        </ResizableSidebar>

        {/* 3. Main Area (Tabs + Score) */}
        <div className="bg-background relative flex min-w-0 flex-1 flex-col">
          <TabBar
            tabs={tabs}
            activeTabPath={activeTabPath}
            onTabSelect={handleTabSelect}
            onTabClose={handleTabClose}
          />

          <main
            ref={scrollContainerRef}
            className="bg-surface items-[safe_center] relative flex flex-1 flex-col overflow-auto p-0"
          >
            <div className="bg-surface mx-auto min-w-min">
              <ScoreDisplay
                loading={loading}
                svgData={svgData}
                meiXML={meiXML}
                onSelectionChange={handleSelectionChange}
                onContextMenu={handleContextMenu}
              />
            </div>
          </main>
        </div>

        {/* 4. Inspector (Right Sidebar) */}
        <ResizableSidebar
          side="right"
          isOpen={isRightSidebarOpen}
          initialWidth={320}
          minWidth={250}
        >
          <Inspector
            selectedIds={selectedIds}
            meiXML={meiXML}
            annotations={annotations}
          />
        </ResizableSidebar>
      </div>

      {/* 5. StatusBar */}
      <StatusBar latestPosition={latestPosition} loading={loading} />

      {/* Context Menu */}
      {contextMenu && (
        // biome-ignore lint/a11y/noStaticElementInteractions: Context menu container
        // biome-ignore lint/a11y/useKeyWithClickEvents: Context menu container
        <div
          className="bg-surface border-border-main fixed z-50 min-w-50 rounded-md border p-2 shadow-lg"
          style={{ top: contextMenu.y, left: contextMenu.x }}
          onClick={(e) => e.stopPropagation()}
        >
          {contextMenu.step === "menu" ? (
            <div className="text-text-main flex flex-col gap-1 text-sm">
              {selectionMode === "note" && (
                <>
                  <button
                    type="button"
                    className="hover:bg-surface-muted w-full rounded px-2 py-1.5 text-left transition-colors"
                    onClick={() => handleMenuSelect("NoteAnnotation")}
                  >
                    Add note annotation
                  </button>
                  <button
                    type="button"
                    className="hover:bg-surface-muted w-full rounded px-2 py-1.5 text-left transition-colors"
                    onClick={() => handleMenuSelect("KeyInformation")}
                  >
                    Add key information
                  </button>
                </>
              )}
              {selectionMode === "staff" && (
                <>
                  <button
                    type="button"
                    className="hover:bg-surface-muted w-full rounded px-2 py-1.5 text-left transition-colors"
                    onClick={() => handleMenuSelect("StaffAnnotation")}
                  >
                    Add staff annotation
                  </button>
                  <button
                    type="button"
                    className="hover:bg-surface-muted w-full rounded px-2 py-1.5 text-left transition-colors"
                    onClick={() => handleMenuSelect("KeyInformation")}
                  >
                    Add key information
                  </button>
                </>
              )}
              {selectionMode === "none" && (
                <button
                  type="button"
                  className="hover:bg-surface-muted w-full rounded px-2 py-1.5 text-left transition-colors"
                  onClick={() => handleMenuSelect("ScoreInformation")}
                >
                  Add score information
                </button>
              )}
            </div>
          ) : (
            <div className="flex flex-col gap-2">
              <div className="text-text-sub text-xs font-bold tracking-wider uppercase">
                {contextMenu.activeMenuId}
              </div>
              <input
                type="text"
                className="border-border-main bg-surface text-text-main focus:border-brand w-full rounded border px-2 py-1 text-sm focus:outline-none"
                value={contextMenu.inputValue}
                onChange={(e) => handleContextMenuInputChange(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === "Enter") handleAnnotationSubmit();
                }}
                // biome-ignore lint/a11y/noAutofocus: input focus
                autoFocus
              />
              <div className="flex justify-end gap-2">
                <button
                  type="button"
                  className="text-text-sub hover:text-text-main text-xs"
                  onClick={closeContextMenu}
                >
                  Cancel
                </button>
                <button
                  type="button"
                  className="bg-brand text-text-on-brand hover:bg-brand-hover rounded px-3 py-1.5 text-xs font-medium transition-colors"
                  onClick={handleAnnotationSubmit}
                >
                  Add
                </button>
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  );
}

export default App;
