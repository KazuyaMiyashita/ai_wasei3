import { Minus, Plus } from "lucide-react";
import type React from "react";
import type { UseScoreView } from "../../../hooks/score/useScoreView";
import { ButtonGroup } from "../../ui/ButtonGroup";
import { IconButton } from "../../ui/IconButton";

interface ScoreZoomControlProps {
  scale: number;
  onZoomIn: UseScoreView["handleZoomIn"];
  onZoomOut: UseScoreView["handleZoomOut"];
}

export const ScoreZoomControl: React.FC<ScoreZoomControlProps> = ({
  scale,
  onZoomIn,
  onZoomOut,
}) => {
  return (
    <ButtonGroup className="gap-0.5 p-0.5">
      <IconButton
        onClick={onZoomOut}
        title="Zoom Out"
        icon={<Minus className="h-4 w-4" />}
      />
      <span className="text-text-main min-w-10 text-center font-mono text-xs font-bold select-none">
        {scale}%
      </span>
      <IconButton
        onClick={onZoomIn}
        title="Zoom In"
        icon={<Plus className="h-4 w-4" />}
      />
    </ButtonGroup>
  );
};
