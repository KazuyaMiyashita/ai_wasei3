import type { PartData } from "./performer";

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
  // Simple XPath lookup for the note
  const nsResolver = (prefix: string | null) => {
    if (prefix === "mei") return "http://www.music-encoding.org/ns/mei";
    return null;
  };

  const result = meiXML.evaluate(
    `//*[@xml:id='${noteId}']`,
    meiXML,
    nsResolver,
    XPathResult.FIRST_ORDERED_NODE_TYPE,
    null,
  );

  const element = result.singleNodeValue as Element;

  if (!element || element.tagName !== "note") {
    return [];
  }

  const pname = element.getAttribute("pname") || "c";
  const oct = parseInt(element.getAttribute("oct") || "4", 10);

  const freq = midiToFreq(noteToMidi(pname, oct));
  const durationMs = 500; // Fixed duration for preview

  return [
    {
      id: 1,
      pitch: [
        { time: 0, value: freq },
        { time: durationMs, value: freq },
      ],
      intensity: [
        { time: 0, value: -10 },
        { time: 50, value: -2 }, // Attack
        { time: durationMs - 50, value: -2 }, // Sustain
        { time: durationMs, value: -60 }, // Release
      ],
      metadata: [
        { time: 0, type: "noteon", elementId: noteId },
        { time: durationMs, type: "noteoff", elementId: noteId },
      ],
    },
  ];
}
