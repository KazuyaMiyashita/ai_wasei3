import Vex from "vexflow";
import {
  CLEF_MAP,
  CONFIG,
  PART_KEYS,
  PART_NAME_MAP,
  type PartId,
} from "./constants";
import {
  type Duration,
  type FullScore,
  type Key,
  type Measure,
  NoteName,
  type Pitch,
  type TimeSignature,
} from "./model";

const {
  Renderer,
  Stave,
  StaveNote,
  Voice,
  Formatter,
  Accidental: VexAccidental,
  StaveTie,
  Dot,
  Beam,
  Annotation,
  StaveConnector,
} = Vex.Flow;

// --- Adapter Logic (Model -> VexFlow) ---

function pitchToVexFlowKey(pitch: Pitch): string {
  const { step, alter, octave } = pitch.internationalPitchNotation();
  const acc =
    alter === 0 ? "" : alter > 0 ? "#".repeat(alter) : "b".repeat(-alter);
  return `${step.toLowerCase()}${acc}/${octave}`;
}

function restToVexFlowKey(clef: string): string {
  switch (clef) {
    case "bass":
      return "d/3";
    case "tenor":
      return "a/3";
    case "alto":
      return "c/4";
    case "soprano":
      return "g/4";
    case "treble":
      return "b/4";
    default:
      return "b/4";
  }
}

function durationToVexFlow(duration: Duration, isRest: boolean): string {
  const val = duration.val;
  let d = "q";
  if (val >= 4) d = "w";
  else if (val >= 3) d = "hd";
  else if (val >= 2) d = "h";
  else if (val >= 1.5) d = "qd";
  else if (val >= 1) d = "q";
  else if (val >= 0.75) d = "8d";
  else if (val >= 0.5) d = "8";
  else if (val >= 0.25) d = "16";

  if (isRest) d += "r";
  return d;
}

function getKeySignatureString(key: Key): string {
  if (key.mode.name === "Minor") {
    const majorTonicVal = key.tonic.value + key.mode.offset;
    const tempName = new NoteName(majorTonicVal);
    return tempName.name();
  }
  return key.tonic.name();
}

// --- Types & Interfaces ---

export interface RenderContext {
  context: ReturnType<Vex.Renderer["getContext"]>;
  staves: Vex.Stave[][]; // [Part][Measure]
  notes: Vex.StaveNote[][][]; // [Part][Measure][NoteIndex]
  measureRects: SVGElement[][]; // [Part][Measure]
}

interface SelectionInfo {
  part: number;
  measure: number;
  note: number;
}

// --- Renderer Class ---

export class ScoreRenderer {
  private div: HTMLDivElement;
  private renderer: Vex.Renderer;
  private context: ReturnType<Vex.Renderer["getContext"]>;

  constructor(div: HTMLDivElement) {
    this.div = div;
    this.renderer = new Renderer(div, Renderer.Backends.SVG);
    this.context = this.renderer.getContext();
  }

  render(score: FullScore, selection: SelectionInfo | null): RenderContext {
    this.div.innerHTML = ""; // Clear
    this.renderer = new Renderer(this.div, Renderer.Backends.SVG);
    this.context = this.renderer.getContext();
    this.context.setFont("Arial", 10, "");

    const orderedPartKeys: PartId[] = PART_KEYS.filter((k) => score.parts[k]);

    if (orderedPartKeys.length === 0)
      return { context: this.context, staves: [], notes: [], measureRects: [] };

    const measureCount = score.parts[orderedPartKeys[0]].length;
    const measureWidths = this.calculateMeasureWidths(score, measureCount);

    const totalWidth =
      measureWidths.reduce((a, b) => a + b, 0) + CONFIG.startX + 50;
    const totalHeight = orderedPartKeys.length * CONFIG.partHeight + 50;

    this.renderer.resize(totalWidth, totalHeight);

    // --- Data Preparation Phase ---
    // Generate all VexFlow objects (Staves, Notes, Voices) without drawing yet.
    // However, VexFlow objects often need Context to be drawn later.
    // We will structure this by Parts and Measures.

    const allStaves: Vex.Stave[][] = []; // [Part][Measure]
    const allNotes: Vex.StaveNote[][][] = []; // [Part][Measure]
    const allVoices: Vex.Voice[][] = Array.from(
      { length: measureCount },
      () => [],
    ); // [Measure][Part]
    const allMeasureRects: SVGElement[][] = []; // [Part][Measure]
    const allTies: Vex.StaveTie[] = [];

    orderedPartKeys.forEach((partId, partIndex) => {
      const partStaves: Vex.Stave[] = [];
      const partNotes: Vex.StaveNote[][] = [];
      const partRects: SVGElement[] = [];

      let currentX = CONFIG.startX;
      const y = CONFIG.startY + partIndex * CONFIG.partHeight;
      let pendingTieNote: Vex.StaveNote | null = null;

      score.parts[partId].forEach((measure, measureIndex) => {
        const width = measureWidths[measureIndex];

        // 1. Create Stave
        const stave = this.createStave(
          currentX,
          y,
          width,
          partId,
          measureIndex === 0,
          score,
        );
        partStaves.push(stave);

        // 2. Create Measure Overlay (for selection)
        // We create DOM element here.
        const rect = this.createMeasureOverlay(
          currentX,
          y,
          width,
          stave.getHeight(),
        );
        partRects.push(rect);

        // 3. Create Notes
        const clef = CLEF_MAP[partId] || "treble";

        const { vfNotes, ties, lastNote } = this.createVexFlowNotes(
          measure,
          score.key,
          clef,
          pendingTieNote,
          selection,
          partIndex,
          measureIndex,
        );

        partNotes.push(vfNotes);
        allTies.push(...ties);
        pendingTieNote = lastNote; // For next measure

        // 4. Create Voice
        if (vfNotes.length > 0) {
          const voice = this.createVoice(score.timeSignature, vfNotes);
          allVoices[measureIndex].push(voice);
        }

        currentX += width;
      });

      allStaves.push(partStaves);
      allNotes.push(partNotes);
      allMeasureRects.push(partRects);
    });

    // --- Drawing Phase ---

    // 1. Draw Staves & Highlights
    allStaves.forEach((partStaves, partIndex) => {
      partStaves.forEach((stave, measureIndex) => {
        stave.setContext(this.context).draw();

        // Measure Highlight
        if (
          selection &&
          selection.part === partIndex &&
          selection.measure === measureIndex &&
          selection.note === -1
        ) {
          this.highlightMeasure(
            stave.getX(),
            stave.getY(),
            stave.getWidth(),
            stave.getHeight(),
          );
        }
      });
    });

    // 2. Format & Draw Voices
    for (let i = 0; i < measureCount; i++) {
      const voices = allVoices[i];
      if (voices.length === 0) continue;

      const staves = allStaves.map((partStaves) => partStaves[i]);
      const width = measureWidths[i];

      this.formatAndDrawVoices(voices, staves, width, score.timeSignature);
    }

    // 3. Draw Connectors
    this.drawConnectors(allStaves);

    // 4. Draw Ties
    allTies.forEach((t) => {
      t.setContext(this.context).draw();
    });

    return {
      context: this.context,
      staves: allStaves,
      notes: allNotes,
      measureRects: allMeasureRects,
    };
  }

  // --- Helper Methods ---

  private calculateMeasureWidths(score: FullScore, count: number): number[] {
    const keySigStr = getKeySignatureString(score.key);
    const timeSigStr = score.timeSignature.toString();

    const dummyStave = new Stave(0, 0, 500);
    dummyStave
      .addClef("treble")
      .addKeySignature(keySigStr)
      .addTimeSignature(timeSigStr);
    const startModifierWidth = dummyStave.getNoteStartX() - dummyStave.getX();

    const measureWidths: number[] = [];
    for (let i = 0; i < count; i++) {
      measureWidths.push(
        i === 0 ? startModifierWidth + CONFIG.baseWidth : CONFIG.baseWidth,
      );
    }
    return measureWidths;
  }

  private createStave(
    x: number,
    y: number,
    width: number,
    partId: PartId,
    isFirst: boolean,
    score: FullScore,
  ): Vex.Stave {
    const stave = new Stave(x, y, width);
    if (isFirst) {
      stave.addClef(CLEF_MAP[partId] || "treble");
      stave.addKeySignature(getKeySignatureString(score.key));
      stave.addTimeSignature(score.timeSignature.toString());
      stave.setText(PART_NAME_MAP[partId], Vex.Flow.Modifier.Position.LEFT);
    }
    return stave;
  }

  private createVexFlowNotes(
    measure: Measure,
    key: Key,
    clef: string,
    prevTieNote: Vex.StaveNote | null,
    selection: SelectionInfo | null,
    partIndex: number,
    measureIndex: number,
  ): {
    vfNotes: Vex.StaveNote[];
    ties: Vex.StaveTie[];
    lastNote: Vex.StaveNote | null;
  } {
    const accidentals = measure.calculateAccidentals(key);
    const vfNotes: Vex.StaveNote[] = [];
    const ties: Vex.StaveTie[] = [];
    let pendingTieNote = prevTieNote;
    let lastNote: Vex.StaveNote | null = null;

    measure.notes.forEach((note, noteIndex) => {
      const keys = note.isRest()
        ? [restToVexFlowKey(clef)]
        : [pitchToVexFlowKey(note.value as Pitch)];
      const duration = durationToVexFlow(note.duration, note.isRest());

      const vfNote = new StaveNote({ keys, duration, clef });

      if (!note.isRest()) {
        vfNote.autoStem();
        const pitch = note.value as Pitch;

        // Accidental
        const accidental = accidentals[noteIndex];
        if (accidental) {
          vfNote.addModifier(new VexAccidental(accidental.type));
        } else {
          // To avoid disrupting the layout while working on the score,
          // add transparent accidentals even when they are not needed.
          const phantomAccidental = new VexAccidental("#");
          phantomAccidental.setStyle({
            fillStyle: "transparent",
            strokeStyle: "transparent",
          });
          vfNote.addModifier(phantomAccidental);
        }

        // Annotation
        vfNote.addModifier(
          new Annotation(pitch.name())
            .setFont("Arial", 10, "")
            .setVerticalJustification(Annotation.VerticalJustify.BOTTOM),
          0,
        );
      }

      // Handle Ties
      if (pendingTieNote) {
        ties.push(
          new StaveTie({
            first_note: pendingTieNote,
            last_note: vfNote,
            first_indices: [0],
            last_indices: [0],
          }),
        );
        pendingTieNote = null;
      }

      if (note.attribute.isTiedStart) {
        pendingTieNote = vfNote;
      }

      // Determine last note for cross-measure tie
      lastNote = pendingTieNote;

      // Highlight
      if (
        selection &&
        selection.part === partIndex &&
        selection.measure === measureIndex &&
        selection.note === noteIndex
      ) {
        vfNote.setStyle({ fillStyle: "#e74c3c", strokeStyle: "#e74c3c" });
      }

      // Dots
      if (duration.includes("d")) {
        Dot.buildAndAttach([vfNote]);
      }

      vfNotes.push(vfNote);
    });

    return { vfNotes, ties, lastNote };
  }

  private createVoice(
    timeSignature: import("./model").TimeSignature,
    notes: Vex.StaveNote[],
  ): Vex.Voice {
    const numBeats = timeSignature.beats;
    const beatValue = Math.round(4 / timeSignature.beatType.val);
    const voice = new Voice({ num_beats: numBeats, beat_value: beatValue });
    voice.addTickables(notes);
    return voice;
  }

  /**
   * スペーシング調整用の「見えないボイス（Ghost Voice）」を作成する関数
   */
  private createGhostVoice(timeSignature: TimeSignature): Vex.Voice {
    // アプリのモデルから VexFlow 用の数値に変換
    const numBeats = timeSignature.beats;
    const beatValue = Math.round(4 / timeSignature.beatType.val); // 例: 0.25 -> 16, 1 -> 4

    const duration = "8"; // 8分音符で埋める

    // 必要な音符の数を計算: (拍数 / 拍の単位) * 8
    const numNotes = (numBeats / beatValue) * 8;

    const ghostNotes: Vex.StaveNote[] = [];
    for (let i = 0; i < numNotes; i++) {
      const note = new StaveNote({
        keys: ["b/4"],
        duration: duration,
        clef: "treble",
      });
      // 透明にする
      note.setStyle({ fillStyle: "transparent", strokeStyle: "transparent" });
      ghostNotes.push(note);
    }

    const ghostVoice = new Voice({
      num_beats: numBeats,
      beat_value: beatValue,
    });
    ghostVoice.setStrict(false);
    ghostVoice.addTickables(ghostNotes);

    return ghostVoice;
  }

  private formatAndDrawVoices(
    voices: Vex.Voice[],
    staves: Vex.Stave[],
    width: number,
    timeSignature: TimeSignature,
  ) {
    const formatter = new Formatter();

    // 1. Ghost Voice の作成
    const ghostVoice = this.createGhostVoice(timeSignature);
    const allVoicesToFormat = [...voices, ghostVoice];
    allVoicesToFormat.forEach((v) => {
      formatter.joinVoices([v]);
    });

    // Align staves
    const startOffset = staves[0].getNoteStartX() - staves[0].getX();
    const rightPadding = 5;
    const availableWidth = width - startOffset - rightPadding;

    // Ghost Voiceありでformat
    formatter.format(allVoicesToFormat, availableWidth, { align_rests: true });
    // Ghost Voiceは描画しない
    voices.forEach((voice, pIndex) => {
      const stave = staves[pIndex];
      const notes = voice.getTickables() as Vex.StaveNote[];
      const beams = Beam.generateBeams(notes);

      voice.draw(this.context, stave);
      beams.forEach((b) => {
        b.setContext(this.context).draw();
      });
    });
  }

  private drawConnectors(allStaves: Vex.Stave[][]) {
    if (allStaves.length === 0) return;
    const numMeasures = allStaves[0].length;
    const lastPartIdx = allStaves.length - 1;

    for (let i = 0; i < numMeasures; i++) {
      const top = allStaves[0][i];
      const bottom = allStaves[lastPartIdx][i];

      if (i === 0) {
        new StaveConnector(top, bottom)
          .setType(StaveConnector.type.SINGLE)
          .setContext(this.context)
          .draw();
      }
      const type =
        i === numMeasures - 1
          ? StaveConnector.type.BOLD_DOUBLE_RIGHT
          : StaveConnector.type.SINGLE_RIGHT;
      new StaveConnector(top, bottom)
        .setType(type)
        .setContext(this.context)
        .draw();
    }
  }

  private createMeasureOverlay(
    x: number,
    y: number,
    width: number,
    height: number,
  ): SVGElement {
    const rect = document.createElementNS("http://www.w3.org/2000/svg", "rect");
    rect.setAttribute("x", String(x));
    rect.setAttribute("y", String(y));
    rect.setAttribute("width", String(width));
    rect.setAttribute("height", String(height));
    rect.setAttribute("fill", "transparent");
    rect.setAttribute("stroke", "transparent");
    rect.style.cursor = "pointer";

    const svg = this.div.querySelector("svg");
    if (svg) svg.appendChild(rect);

    return rect;
  }

  private highlightMeasure(
    x: number,
    y: number,
    width: number,
    height: number,
  ) {
    const rect = document.createElementNS("http://www.w3.org/2000/svg", "rect");
    rect.setAttribute("x", String(x));
    rect.setAttribute("y", String(y));
    rect.setAttribute("width", String(width));
    rect.setAttribute("height", String(height));
    rect.setAttribute("fill", "rgba(100, 149, 237, 0.2)");
    rect.setAttribute("stroke", "transparent");
    rect.style.cursor = "pointer";

    const svg = this.div.querySelector("svg");
    if (svg) svg.appendChild(rect);
  }
}
