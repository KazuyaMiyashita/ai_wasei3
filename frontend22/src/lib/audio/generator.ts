import { findElementById } from "../model/music/xml-navigation";
import type { PartData } from "./performer";

// このファイルに音楽の要素のロジックを定義しないこと。
// frontend22/src/lib/model/music/elements.ts に記述しなければならない。
// また現在のロジックはbackend22のものと異なるものである。

function noteToMidi(pname: string, oct: number): number {
  const noteMap: Record<string, number> = {
    c: 0,
    d: 2,
    e: 4,
    f: 5,
    g: 7,
    a: 9,
    b: 11,
  };
  const base = noteMap[pname.toLowerCase()] ?? 0;
  // MIDI note: C4 (Middle C) = 60. C4 is usually oct=4.
  // MEI oct=4 is C4.
  return (oct + 1) * 12 + base;
}

function midiToFreq(midi: number): number {
  return 440 * 2 ** ((midi - 69) / 12);
}

/**
 * MEI楽譜データと選択された音符のIDから、再生用のPartData[]を生成します。
 * @param meiXML MEI形式のXMLドキュメント
 * @param noteId 選択された音符のxml:id
 * @returns 再生用データ
 */
export function generateAudioForNote(
  meiXML: Document,
  noteId: string,
): PartData[] {
  const element = findElementById(meiXML, noteId);

  if (!element || element.tagName !== "note") {
    return [];
  }

  // FIXME: 調号や臨時記号を取得していない。
  const pname = element.getAttribute("pname") || "c";
  const oct = parseInt(element.getAttribute("oct") || "4", 10);

  const freq = midiToFreq(noteToMidi(pname, oct));
  const durationMs = 500; // Fixed duration for preview

  // FIXME: 音の立ち上がりがプツプツする。
  return [
    {
      id: 1,
      pitch: [
        { time: 0, value: freq },
        { time: durationMs, value: freq },
      ],
      intensity: [
        { time: 0, value: -9999 },
        { time: 100 + 0, value: -96 },
        { time: 100 + 50, value: -2 }, // Attack
        { time: 100 + durationMs - 50, value: -2 }, // Sustain
        { time: 100 + durationMs, value: -96 }, // Release
      ],
      metadata: [
        { time: 0, type: "noteon", elementId: noteId },
        { time: durationMs, type: "noteoff", elementId: noteId },
      ],
    },
  ];
}
