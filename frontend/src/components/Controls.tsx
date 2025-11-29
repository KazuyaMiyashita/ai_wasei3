import type React from "react";
import { useState } from "react";
import type { GenerateRequest } from "../lib/api";
import type { PartId } from "../lib/constants";
import type { components } from "../lib/schema";

type Species = components["schemas"]["Species"];

interface ControlsProps {
  onGenerate: (data: GenerateRequest) => Promise<void>;
  onReset: () => void;
  onExport: () => void;
  onImport: (jsonContent: string) => void;
  onKeyChange: (tonic: number, mode: "Major" | "Minor") => void;
  isGenerating: boolean;
}

const CF_PRESETS: Record<string, string[]> = {
  default: ["C4", "A3", "G3", "E3", "F3", "A3", "G3", "E3", "D3", "C3"],
};

export const Controls: React.FC<ControlsProps> = ({
  onGenerate,
  onReset,
  onExport,
  onImport,
  onKeyChange,
  isGenerating,
}) => {
  const [keyLabel, setKeyLabel] = useState("C Major");
  const [species, setSpecies] = useState<Species>("fifth");
  const [cfPart, setCfPart] = useState<PartId>("BASS");
  const [cfPreset] = useState("default");
  const [targetPart, setTargetPart] = useState<PartId>("SOPRANO");

  const handleKeySelect = (e: React.ChangeEvent<HTMLSelectElement>) => {
    const val = e.target.value;
    setKeyLabel(val);

    let tonic = 0;
    let mode: "Major" | "Minor" = "Major";

    switch (val) {
      case "C Major":
        tonic = 0;
        mode = "Major";
        break;
      case "A Minor":
        tonic = 3;
        mode = "Minor";
        break;
      case "G Major":
        tonic = 1;
        mode = "Major";
        break;
      case "E Minor":
        tonic = 4;
        mode = "Minor";
        break;
      case "F Major":
        tonic = -1;
        mode = "Major";
        break;
      case "D Minor":
        tonic = 2;
        mode = "Minor";
        break;
      default:
        tonic = 0;
        mode = "Major";
    }
    onKeyChange(tonic, mode);
  };

  const handleGenerate = () => {
    const cf = CF_PRESETS[cfPreset];
    const data: GenerateRequest = {
      cf,
      key: keyLabel,
      cf_part_id: cfPart,
      part_id: targetPart,
      species,
    };
    onGenerate(data);
  };

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    const reader = new FileReader();
    reader.onload = (event) => {
      const content = event.target?.result as string;
      if (content) {
        onImport(content);
      }
    };
    reader.readAsText(file);
    // Reset value so same file can be selected again
    e.target.value = "";
  };

  return (
    <div className="controls">
      <div className="content-wrapper">
        <div className="control-group">
          <label htmlFor="select-key">Key</label>
          <select id="select-key" value={keyLabel} onChange={handleKeySelect}>
            <option value="C Major">C Major</option>
            <option value="A Minor">A Minor</option>
            <option value="G Major">G Major</option>
            <option value="E Minor">E Minor</option>
            <option value="F Major">F Major</option>
            <option value="D Minor">D Minor</option>
          </select>
        </div>

        <div className="control-group">
          <label htmlFor="select-species">Species</label>
          <select
            id="select-species"
            value={species}
            onChange={(e) => setSpecies(e.target.value as Species)}
          >
            <option value="first">First (1:1)</option>
            <option value="second">Second (2:1)</option>
            <option value="third">Third (4:1)</option>
            <option value="fourth">Fourth (Suspension)</option>
            <option value="fifth">Fifth (Florid)</option>
          </select>
        </div>

        <div className="control-group">
          <label htmlFor="select-cf-part">CF Part</label>
          <select
            id="select-cf-part"
            value={cfPart}
            onChange={(e) => setCfPart(e.target.value as PartId)}
          >
            <option value="SOPRANO">Soprano</option>
            <option value="ALTO">Alto</option>
            <option value="TENOR">Tenor</option>
            <option value="BASS">Bass</option>
          </select>
        </div>

        <div className="control-group">
          <label htmlFor="select-cf-preset">CF Preset</label>
          <select id="select-cf-preset" value={cfPreset} disabled>
            <option value="default">Default CF (C Major)</option>
          </select>
        </div>

        <div className="control-group">
          <label htmlFor="select-counter-part">Target Part</label>
          <select
            id="select-counter-part"
            value={targetPart}
            onChange={(e) => setTargetPart(e.target.value as PartId)}
          >
            <option value="SOPRANO">Soprano</option>
            <option value="ALTO">Alto</option>
            <option value="TENOR">Tenor</option>
            <option value="BASS">Bass</option>
          </select>
        </div>

        <button
          type="button"
          className="btn btn-generate"
          onClick={handleGenerate}
          disabled={isGenerating}
        >
          {isGenerating ? "生成中..." : "✨ 対位法生成"}
        </button>

        <div className="toolbar" style={{ marginLeft: "auto" }}>
          <button type="button" className="btn" onClick={onReset}>
            ↺ リセット
          </button>
          <label className="btn" style={{ cursor: "pointer" }}>
            📂 インポート
            <input
              type="file"
              accept=".json"
              style={{ display: "none" }}
              onChange={handleFileChange}
            />
          </label>
          <button type="button" className="btn btn-primary" onClick={onExport}>
            ⬇ エクスポート
          </button>
        </div>
      </div>
    </div>
  );
};
