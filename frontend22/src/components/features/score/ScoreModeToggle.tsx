import { MousePointer2, Pencil } from "lucide-react";
import type React from "react";
import type { UseScoreInteraction } from "../../../hooks/score/useScoreInteraction";
import { ButtonGroup } from "../../ui/ButtonGroup";
import { IconButton } from "../../ui/IconButton";

interface ScoreModeToggleProps {
  mode: UseScoreInteraction["interactionMode"];
  setMode: UseScoreInteraction["setInteractionMode"];
}

export const ScoreModeToggle: React.FC<ScoreModeToggleProps> = ({
  mode,
  setMode,
}) => {
  return (
    <ButtonGroup>
      <IconButton
        onClick={() => setMode("select")}
        isActive={mode === "select"}
        variant="toggle"
        title="Selection Mode"
        icon={<MousePointer2 className="h-4 w-4" />}
      />
      <IconButton
        onClick={() => setMode("edit")}
        isActive={mode === "edit"}
        variant="toggle"
        title="Edit Mode"
        icon={<Pencil className="h-4 w-4" />}
      />
    </ButtonGroup>
  );
};
