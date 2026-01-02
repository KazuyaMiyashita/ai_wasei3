import type React from "react";

interface ScoreItem {
  path: string;
  name: string;
}

interface ScoreControlsProps {
  currentFile: string;
  scores: ScoreItem[];
  onFileSelect: (e: React.ChangeEvent<HTMLSelectElement>) => void;
  onLocalFileSelect: (e: React.ChangeEvent<HTMLInputElement>) => void;
  onPrevFile: () => void;
  onNextFile: () => void;
  scale: number;
  onZoomIn: () => void;
  onZoomOut: () => void;
  onPartwise: () => void;
}

const ScoreControls: React.FC<ScoreControlsProps> = ({
  currentFile,
  scores,
  onFileSelect,
  onLocalFileSelect,
  onPrevFile,
  onNextFile,
  scale,
  onZoomIn,
  onZoomOut,
  onPartwise,
}) => {
  const currentIndex = scores.findIndex((s) => s.path === currentFile);

  return (
    <div className="sticky top-0 z-10 border-b border-gray-200 bg-white shadow-sm">
      <div className="mx-auto grid max-w-200 grid-cols-1 items-center gap-4 p-4 md:grid-cols-3">
        {/* Left Actions */}
        <div className="flex items-center justify-center gap-2 md:justify-self-start">
          <button
            type="button"
            onClick={onPartwise}
            className="rounded-md bg-blue-600 px-3 py-1.5 text-sm font-medium text-white shadow-sm transition-all hover:bg-blue-700 active:scale-95"
          >
            partwise
          </button>
          <label className="cursor-pointer rounded-md border border-gray-300 bg-white px-3 py-1.5 text-sm font-medium text-gray-700 shadow-sm transition-all hover:bg-gray-50 active:scale-95">
            Open File
            <input
              type="file"
              accept=".mei,.xml"
              className="hidden"
              onChange={onLocalFileSelect}
            />
          </label>
        </div>

        {/* File Navigation - Centered */}
        <div className="flex items-center justify-center gap-2 justify-self-center rounded-lg border border-gray-200 bg-gray-100 p-1.5">
          <button
            type="button"
            onClick={onPrevFile}
            disabled={currentIndex <= 0}
            className="rounded-md p-2 text-gray-600 transition-all hover:bg-white hover:shadow-sm disabled:opacity-30 disabled:hover:bg-transparent disabled:hover:shadow-none"
            title="Previous File"
          >
            ←
          </button>

          <div className="relative">
            <select
              value={currentFile}
              onChange={onFileSelect}
              className="w-48 cursor-pointer appearance-none bg-transparent py-1 pr-8 pl-3 text-center text-sm font-medium text-gray-700 hover:text-gray-900 focus:outline-none"
            >
              {currentFile === "" && <option value="">Local file</option>}
              {scores.map((score) => (
                <option key={score.path} value={score.path}>
                  {score.name}
                </option>
              ))}
            </select>
            <div className="pointer-events-none absolute inset-y-0 right-0 flex items-center px-2 text-gray-500">
              <svg
                className="h-4 w-4 fill-current"
                viewBox="0 0 20 20"
                aria-hidden="true"
              >
                <path d="M5.293 7.293a1 1 0 011.414 0L10 10.586l3.293-3.293a1 1 0 111.414 1.414l-4 4a1 1 0 01-1.414 0l-4-4a1 1 0 010-1.414z" />
              </svg>
            </div>
          </div>

          <button
            type="button"
            onClick={onNextFile}
            disabled={currentIndex === -1 || currentIndex >= scores.length - 1}
            className="rounded-md p-2 text-gray-600 transition-all hover:bg-white hover:shadow-sm disabled:opacity-30 disabled:hover:bg-transparent disabled:hover:shadow-none"
            title="Next File"
          >
            →
          </button>
        </div>

        {/* Zoom Controls - Right aligned */}
        <div className="flex items-center justify-center gap-3 md:justify-end">
          <button
            type="button"
            onClick={onZoomOut}
            className="flex h-8 w-8 items-center justify-center rounded-md border border-gray-300 bg-white text-lg font-medium text-gray-700 transition-all hover:bg-gray-50 hover:text-gray-900 active:scale-95"
            title="Zoom Out"
          >
            -
          </button>

          <span className="min-w-15 rounded border border-gray-200 bg-gray-50 px-2 py-1 text-center font-mono text-sm font-medium text-gray-600">
            {scale}%
          </span>

          <button
            type="button"
            onClick={onZoomIn}
            className="flex h-8 w-8 items-center justify-center rounded-md border border-gray-300 bg-white text-lg font-medium text-gray-700 transition-all hover:bg-gray-50 hover:text-gray-900 active:scale-95"
            title="Zoom In"
          >
            +
          </button>
        </div>
      </div>
    </div>
  );
};

export default ScoreControls;
