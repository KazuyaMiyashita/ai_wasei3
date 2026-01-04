import type React from "react";
import { useState } from "react";
import { ButtonGroup } from "../../ui/ButtonGroup";
import { IconButton } from "../../ui/IconButton";
import * as MIcon from "../../ui/MusicIcon";

const NotePalette: React.FC = () => {
  const [activeNoteId, setActiveNoteId] = useState<string>("note_quarter");

  const groups = [
    {
      name: "Notes",
      tools: [
        { id: "note_whole", label: "全音符", Icon: MIcon.WholeNote },
        { id: "note_half", label: "2分音符", Icon: MIcon.HalfNote },
        { id: "note_quarter", label: "4分音符", Icon: MIcon.QuarterNote },
        { id: "note_eighth", label: "8分音符", Icon: MIcon.EighthNote },
        { id: "note_sixteenth", label: "16分音符", Icon: MIcon.SixteenthNote },
        { id: "note_32", label: "32分音符", Icon: MIcon.ThirtySecondNote },
      ],
    },
    {
      name: "Rests",
      tools: [
        { id: "rest_whole", label: "全休符", Icon: MIcon.WholeRest },
        { id: "rest_half", label: "2分休符", Icon: MIcon.HalfRest },
        { id: "rest_quarter", label: "4分休符", Icon: MIcon.QuarterRest },
        { id: "rest_eighth", label: "8分休符", Icon: MIcon.EighthRest },
        { id: "rest_sixteenth", label: "16分休符", Icon: MIcon.SixteenthRest },
        { id: "rest_32", label: "32分休符", Icon: MIcon.ThirtySecondRest },
      ],
    },
    {
      name: "Accidentals",
      tools: [
        { id: "sharp", label: "シャープ", Icon: MIcon.Sharp },
        { id: "flat", label: "フラット", Icon: MIcon.Flat },
        { id: "natural", label: "ナチュラル", Icon: MIcon.Natural },
        { id: "doublesharp", label: "ダブルシャープ", Icon: MIcon.DoubleSharp },
        { id: "doubleflat", label: "ダブルフラット", Icon: MIcon.DoubleFlat },
      ],
    },
    {
      name: "Clefs",
      tools: [
        { id: "clef_g", label: "ト音記号", Icon: MIcon.GClef },
        { id: "clef_f", label: "ヘ音記号", Icon: MIcon.FClef },
        { id: "clef_c", label: "ハ音記号", Icon: MIcon.CClef },
      ],
    },
    {
      name: "Articulations",
      tools: [
        { id: "staccato", label: "スタッカート", Icon: MIcon.Staccato },
        { id: "accent", label: "アクセント", Icon: MIcon.Accent },
        { id: "tenuto", label: "テヌート", Icon: MIcon.Tenuto },
        { id: "fermata", label: "フェルマータ", Icon: MIcon.Fermata },
      ],
    },
  ];

  return (
    <div className="p-4 space-y-6">
      {groups.map((group) => (
        <div key={group.name} className="space-y-2">
          <h3 className="text-[9px] text-text-muted font-semibold px-1 uppercase tracking-tight">
            {group.name}
          </h3>
          <div className="flex flex-wrap gap-1">
            <ButtonGroup>
              {group.tools.map(({ id, label, Icon }) => (
                <IconButton
                  key={id}
                  variant="toggle"
                  isActive={activeNoteId === id}
                  icon={<Icon size={24} />}
                  onClick={() => setActiveNoteId(id)}
                  title={label}
                />
              ))}
            </ButtonGroup>
          </div>
        </div>
      ))}
    </div>
  );
};

export default NotePalette;
