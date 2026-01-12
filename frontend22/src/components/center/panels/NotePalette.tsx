import { ToggleGroup } from "radix-ui";
import type React from "react";
import { useState } from "react";
import { cn } from "../../../utils";
import {
  Accent,
  CClef,
  DoubleFlat,
  DoubleSharp,
  EighthNote,
  EighthRest,
  FClef,
  Fermata,
  Flat,
  GClef,
  HalfNote,
  HalfRest,
  Natural,
  QuarterNote,
  QuarterRest,
  Sharp,
  SixteenthNote,
  SixteenthRest,
  Staccato,
  Tenuto,
  ThirtySecondNote,
  ThirtySecondRest,
  WholeNote,
  WholeRest,
} from "../../ui/icons/MusicIcons";

const NotePalette: React.FC = () => {
  const [activeNoteId, setActiveNoteId] = useState<string>("note_quarter");

  const groups = [
    {
      name: "Notes",
      tools: [
        { id: "note_whole", label: "全音符", Icon: WholeNote },
        { id: "note_half", label: "2分音符", Icon: HalfNote },
        { id: "note_quarter", label: "4分音符", Icon: QuarterNote },
        { id: "note_eighth", label: "8分音符", Icon: EighthNote },
        { id: "note_sixteenth", label: "16分音符", Icon: SixteenthNote },
        { id: "note_32", label: "32分音符", Icon: ThirtySecondNote },
      ],
    },
    {
      name: "Rests",
      tools: [
        { id: "rest_whole", label: "全休符", Icon: WholeRest },
        { id: "rest_half", label: "2分休符", Icon: HalfRest },
        { id: "rest_quarter", label: "4分休符", Icon: QuarterRest },
        { id: "rest_eighth", label: "8分休符", Icon: EighthRest },
        { id: "rest_sixteenth", label: "16分休符", Icon: SixteenthRest },
        { id: "rest_32", label: "32分休符", Icon: ThirtySecondRest },
      ],
    },
    {
      name: "Accidentals",
      tools: [
        { id: "sharp", label: "シャープ", Icon: Sharp },
        { id: "flat", label: "フラット", Icon: Flat },
        { id: "natural", label: "ナチュラル", Icon: Natural },
        { id: "doublesharp", label: "ダブルシャープ", Icon: DoubleSharp },
        { id: "doubleflat", label: "ダブルフラット", Icon: DoubleFlat },
      ],
    },
    {
      name: "Clefs",
      tools: [
        { id: "clef_g", label: "ト音記号", Icon: GClef },
        { id: "clef_f", label: "ヘ音記号", Icon: FClef },
        { id: "clef_c", label: "ハ音記号", Icon: CClef },
      ],
    },
    {
      name: "Articulations",
      tools: [
        { id: "staccato", label: "スタッカート", Icon: Staccato },
        { id: "accent", label: "アクセント", Icon: Accent },
        { id: "tenuto", label: "テヌート", Icon: Tenuto },
        { id: "fermata", label: "フェルマータ", Icon: Fermata },
      ],
    },
  ];

  return (
    <div className="ui-panel">
      <h3 className="ui-panel-title">Note Palette</h3>
      <ToggleGroup.Root
        type="single"
        value={activeNoteId}
        onValueChange={(val) => val && setActiveNoteId(val)}
      >
        {groups.map((group) => (
          <div key={group.name} className="ui-panel-sub-section">
            <h4 className="ui-panel-sub-title">{group.name}</h4>
            <div className="flex flex-wrap gap-1">
              {group.tools.map(({ id, label, Icon }) => (
                <ToggleGroup.Item
                  key={id}
                  value={id}
                  title={label}
                  className={cn(
                    "ui-action-button flex h-6 w-6 items-center justify-center",
                    "hover:border-ui-border-hover border border-transparent",
                  )}
                >
                  <Icon size={24} />
                </ToggleGroup.Item>
              ))}
            </div>
          </div>
        ))}
      </ToggleGroup.Root>
    </div>
  );
};

export default NotePalette;
