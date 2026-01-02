import type React from "react";

interface VisualData {
  [id: number]: { pitch: number; intensity: number };
}

interface Props {
  data: VisualData;
}

const PerformanceVisualizer: React.FC<Props> = ({ data }) => {
  const channels = [1, 2, 3, 4];

  return (
    <div className="flex w-full max-w-sm flex-col gap-1.5 px-4">
      {channels.map((id) => {
        const info = data[id] || { pitch: 0, intensity: -96 };
        // Map intensity (-96 to 0) to width (0% to 100%)
        const normalizedVol = Math.max(0, (info.intensity + 96) / 96);

        return (
          <div key={id} className="flex items-center gap-3">
            <span className="w-6 text-[10px] font-bold text-gray-500">
              CH{id}
            </span>
            <div className="h-1.5 flex-1 overflow-hidden rounded-full border border-gray-200 bg-gray-100">
              <div
                className="h-full bg-indigo-500 transition-all duration-75 ease-linear"
                style={{ width: `${normalizedVol * 100}%` }}
              />
            </div>
            <span className="w-12 text-right font-mono text-[10px] text-gray-400">
              {info.pitch > 0 ? `${Math.round(info.pitch)}Hz` : "-"}
            </span>
          </div>
        );
      })}
    </div>
  );
};

export default PerformanceVisualizer;
