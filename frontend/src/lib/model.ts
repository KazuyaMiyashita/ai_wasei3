// frontend/src/lib/model.ts
import type { components } from "./schema";

// --- Type Aliases from Generated Schema ---
type Schemas = components["schemas"];

export type FullScoreJSON = Schemas["FullScore"];
export type NoteNameJSON = Schemas["NoteName"];
export type OctaveJSON = Schemas["Octave"];
export type PitchJSON = Schemas["Pitch"];
export type RestJSON = Schemas["Rest"];
export type DurationJSON = Schemas["Duration"];
export type ScoreAttrsJSON = Schemas["ScoreAttrs"];
export type NoteJSON = Schemas["Note"];
export type MeasureJSON = Schemas["Measure"];
export type KeyJSON = Schemas["Key"];
export type TimeSignatureJSON = Schemas["TimeSignature"];

// --- Constants ---

export const C_MAJOR_FIFTHS = [0, 2, 4, -1, 1, 3, 5];
export const C_MAJOR_BASE_OCTAVES = [0, -1, -2, 1, 0, -1, -2];

const _STEP_TO_BASE_FIFTH: Record<string, number> = {
  C: 0,
  D: 2,
  E: 4,
  F: -1,
  G: 1,
  A: 3,
  B: 5,
};

const _STEP_TO_BASE_OCTAVE: Record<string, number> = {
  C: 0,
  D: -1,
  E: -2,
  F: 1,
  G: 0,
  A: -1,
  B: -2,
};

const _BASE_FIFTH_TO_STEP: Record<number, string> = Object.fromEntries(
  Object.entries(_STEP_TO_BASE_FIFTH).map(([k, v]) => [v, k]),
);

// --- Value Objects (Immutable) ---

export class NoteName {
  constructor(public readonly value: number) {
    if (value < -15 || value > 19) {
      throw new Error(`NoteName must be between -15 and 19. Got ${value}`);
    }
  }

  static parse(name: string): NoteName {
    const match = name.match(/^([A-G])([#b]*)$/);
    if (!match) throw new Error(`Invalid note name format: ${name}`);

    const stepStr = match[1];
    const accidentalStr = match[2];
    const alter =
      (accidentalStr.match(/#/g) || []).length -
      (accidentalStr.match(/b/g) || []).length;

    const baseFifth = _STEP_TO_BASE_FIFTH[stepStr];
    return new NoteName(baseFifth + alter * 7);
  }

  static fromJSON(json: NoteNameJSON): NoteName {
    return new NoteName(json.value);
  }

  toJSON(): NoteNameJSON {
    return { value: this.value };
  }

  name(): string {
    const { step, alter } = this.internationalPitchNotation();
    return `${step}${alter > 0 ? "#".repeat(alter) : "b".repeat(-alter)}`;
  }

  add(other: NoteName): NoteName {
    return new NoteName(this.value + other.value);
  }

  sub(other: NoteName): NoteName {
    return new NoteName(this.value - other.value);
  }

  internationalPitchNotation(): { step: string; alter: number } {
    const baseFifths = [-1, 0, 1, 2, 3, 4, 5];
    for (const baseFifth of baseFifths) {
      if ((this.value - baseFifth) % 7 === 0) {
        const alter = (this.value - baseFifth) / 7;
        const step = _BASE_FIFTH_TO_STEP[baseFifth];
        return { step, alter };
      }
    }
    throw new Error("unreachable");
  }

  static fromInternalPitchNotation(step: string, alter: number): NoteName {
    const baseFifth = _STEP_TO_BASE_FIFTH[step];
    if (baseFifth === undefined) throw new Error(`Invalid step: ${step}`);
    return new NoteName(baseFifth + alter * 7);
  }

  equals(other: NoteName): boolean {
    return this.value === other.value;
  }

  transpose(semitones: number): NoteName {
    // Simplified transposition logic for chromatic alteration
    // semitones > 0: sharp direction (add 7 fifths)
    // semitones < 0: flat direction (subtract 7 fifths)
    // Note: This is not semitones, but alter steps (sharp/flat)
    const delta = semitones * 7;
    return new NoteName(this.value + delta);
  }
}

export class Octave {
  constructor(public readonly value: number) {}

  static fromJSON(json: OctaveJSON): Octave {
    return new Octave(json.value);
  }

  toJSON(): OctaveJSON {
    return { value: this.value };
  }

  add(other: Octave): Octave {
    return new Octave(this.value + other.value);
  }

  sub(other: Octave): Octave {
    return new Octave(this.value - other.value);
  }

  equals(other: Octave): boolean {
    return this.value === other.value;
  }
}

export class IntervalStep {
  constructor(public readonly value: number) {}

  add(other: IntervalStep): IntervalStep {
    return new IntervalStep(this.value + other.value);
  }

  sub(other: IntervalStep): IntervalStep {
    return new IntervalStep(this.value - other.value);
  }

  abs(): IntervalStep {
    return new IntervalStep(Math.abs(this.value));
  }
}

export class IntervalAlter {
  constructor(public readonly value: number) {}

  static PERFECT = new IntervalAlter(0);
  static MAJOR = new IntervalAlter(1);
  static MINOR = new IntervalAlter(-1);
  static AUGMENTED = new IntervalAlter(2);
  static DIMINISHED = new IntervalAlter(-2);
}

export class Interval {
  constructor(
    public readonly octave: number,
    public readonly fifth: number,
  ) {}

  static fromStepAlter(step: IntervalStep, alter: IntervalAlter): Interval {
    const s = step.value;
    const a = alter.value;

    const fClass = ((2 * s) % 7) % 7;
    const normalizedFClass = (fClass + 7) % 7;

    const stepSgn = s < 0 ? -1 : 1;
    const fBaseSharp = ((normalizedFClass - 6) % 7) + 6;
    const fBaseFlat = ((normalizedFClass - 2) % 7) - 12;

    let f = 0;

    if (a === 0) {
      const fMap: Record<number, number> = { 0: 0, 1: 1, 6: -1 };
      if (!(normalizedFClass in fMap))
        throw new Error(`Invalid Perfect interval step: ${s}`);
      f = fMap[normalizedFClass];
    } else if (a === 1) {
      if (![2, 3, 4, 5].includes(normalizedFClass))
        throw new Error(`Invalid Major interval step: ${s}`);
      f = stepSgn === 1 ? normalizedFClass : normalizedFClass - 7;
    } else if (a === -1) {
      if (![2, 3, 4, 5].includes(normalizedFClass))
        throw new Error(`Invalid Minor interval step: ${s}`);
      f = stepSgn === 1 ? normalizedFClass - 7 : normalizedFClass;
    } else if (a >= 2) {
      const k = a - 2;
      f = stepSgn === 1 ? fBaseSharp + 7 * k : fBaseFlat - 7 * k;
    } else if (a <= -2) {
      const k = -a - 2;
      f = stepSgn === 1 ? fBaseFlat - 7 * k : fBaseSharp + 7 * k;
    }

    const residual = s - 4 * f;
    const octave = Math.floor(residual / 7);

    return new Interval(octave, f);
  }

  step(): IntervalStep {
    return new IntervalStep(4 * this.fifth + 7 * this.octave);
  }

  alter(): IntervalAlter {
    const stepVal = this.step().value;
    const fifth = this.fifth;

    const absFifth = Math.abs(fifth);
    const stepSgn = stepVal < 0 ? -1 : 1;
    const fifthSgn = fifth < 0 ? -1 : 1;
    const sgn = stepSgn * fifthSgn;

    let val = 0;
    if (absFifth <= 1) {
      val = 0;
    } else if (absFifth <= 5) {
      val = sgn * 1;
    } else {
      val = sgn * (2 + Math.floor((absFifth - 6) / 7));
    }
    return new IntervalAlter(val);
  }

  add(other: Interval): Interval {
    return new Interval(this.octave + other.octave, this.fifth + other.fifth);
  }

  sub(other: Interval): Interval {
    return new Interval(this.octave - other.octave, this.fifth - other.fifth);
  }

  name(): string {
    const stepVal = this.step().value;
    const sgn = stepVal >= 0 ? "" : "-";
    const num = Math.abs(stepVal) + 1;
    const alterVal = this.alter().value;

    let alph = "";
    if (alterVal === 0) alph = "P";
    else if (alterVal === 1) alph = "M";
    else if (alterVal === -1) alph = "m";
    else if (alterVal >= 2) alph = "A".repeat(alterVal - 1);
    else alph = "d".repeat(-alterVal - 1);

    return `${sgn}${alph}${num}`;
  }
}

export class Pitch {
  constructor(
    public readonly noteName: NoteName,
    public readonly octave: Octave,
  ) {}

  static fromJSON(json: PitchJSON): Pitch {
    return new Pitch(
      NoteName.fromJSON(json.note_name),
      Octave.fromJSON(json.octave),
    );
  }

  toJSON(): PitchJSON {
    return {
      note_name: this.noteName.toJSON(),
      octave: this.octave.toJSON(),
    };
  }

  static parse(name: string): Pitch {
    const match = name.match(/^([A-G][#b]*)(\d+)$/);
    if (!match) throw new Error(`Invalid pitch name format: ${name}`);

    const noteNameStr = match[1];
    const octaveNum = Number.parseInt(match[2], 10);

    const noteName = NoteName.parse(noteNameStr);
    const { step, alter } = noteName.internationalPitchNotation();
    const baseOctave = _STEP_TO_BASE_OCTAVE[step];

    const octaveVal = baseOctave + alter * -4 + octaveNum - 4;

    return new Pitch(noteName, new Octave(octaveVal));
  }

  name(): string {
    const { step, alter, octave } = this.internationalPitchNotation();
    return `${step}${alter > 0 ? "#".repeat(alter) : "b".repeat(-alter)}${octave}`;
  }

  add(interval: Interval): Pitch {
    return new Pitch(
      this.noteName.add(new NoteName(interval.fifth)),
      this.octave.add(new Octave(interval.octave)),
    );
  }

  sub(other: Pitch): Interval {
    return new Interval(
      this.octave.value - other.octave.value,
      this.noteName.value - other.noteName.value,
    );
  }

  internationalPitchNotation(): {
    step: string;
    alter: number;
    octave: number;
  } {
    const { step, alter } = this.noteName.internationalPitchNotation();
    const baseOctave = _STEP_TO_BASE_OCTAVE[step];
    const notationOctave = this.octave.value - baseOctave + 4 * alter + 4;
    return { step, alter, octave: notationOctave };
  }

  static fromInternalPitchNotation(
    step: string,
    alter: number,
    octave: number,
  ): Pitch {
    const noteName = NoteName.fromInternalPitchNotation(step, alter);
    const baseOctave = _STEP_TO_BASE_OCTAVE[step];
    const octaveVal = baseOctave + alter * -4 + octave - 4;
    return new Pitch(noteName, new Octave(octaveVal));
  }

  getIntervalStepFromC4(): number {
    const noteVal = this.noteName.value;
    const r = noteVal;
    const alterVal = Math.round((r - 2) / 7);
    const r0 = r - 7 * alterVal;
    const stepVal = (((4 * r0) % 7) + 7) % 7;

    const baseOctave = C_MAJOR_BASE_OCTAVES[stepVal];
    const octaveShift = this.octave.value - baseOctave + 4 * alterVal;

    return octaveShift * 7 + stepVal;
  }

  equals(other: Pitch): boolean {
    return (
      this.noteName.equals(other.noteName) && this.octave.equals(other.octave)
    );
  }

  transposeChromatic(direction: number): Pitch | null {
    // direction: 1 (sharp) or -1 (flat)
    let val = this.noteName.value;
    let oct = this.octave.value;

    if (direction > 0) {
      val += 7;
      oct -= 4;
    } else {
      val -= 7;
      oct += 4;
    }

    if (val < -15 || val > 19) return null;
    return new Pitch(new NoteName(val), new Octave(oct));
  }
}

export class Duration {
  constructor(
    public readonly numerator: number,
    public readonly denominator: number,
  ) {}

  static fromJSON(json: DurationJSON): Duration {
    return new Duration(json.value.numerator, json.value.denominator);
  }

  toJSON(): DurationJSON {
    return {
      value: { numerator: this.numerator, denominator: this.denominator },
    };
  }

  get val(): number {
    return this.numerator / this.denominator;
  }

  static fromValue(value: number): Duration {
    const q = Math.round(value * 16);
    return new Duration(q, 16).simplify();
  }

  add(other: Duration): Duration {
    const num =
      this.numerator * other.denominator + other.numerator * this.denominator;
    const den = this.denominator * other.denominator;
    return new Duration(num, den).simplify();
  }

  sub(other: Duration): Duration {
    const num =
      this.numerator * other.denominator - other.numerator * this.denominator;
    const den = this.denominator * other.denominator;
    return new Duration(num, den).simplify();
  }

  simplify(): Duration {
    const gcd = (a: number, b: number): number => (b === 0 ? a : gcd(b, a % b));
    const common = gcd(Math.abs(this.numerator), this.denominator);
    return new Duration(this.numerator / common, this.denominator / common);
  }

  equals(other: Duration): boolean {
    return this.val === other.val;
  }

  gt(other: Duration): boolean {
    return this.val > other.val;
  }

  lt(other: Duration): boolean {
    return this.val < other.val;
  }
}

export class Mode {
  constructor(
    public readonly name: "Major" | "Minor",
    public readonly offset: number,
  ) {}

  static MAJOR = new Mode("Major", 0);
  static MINOR = new Mode("Minor", -3);

  static parse(name: string): Mode {
    if (name === "Major") return Mode.MAJOR;
    if (name === "Minor") return Mode.MINOR;
    throw new Error(`Invalid mode: ${name}`);
  }
}

export class Key {
  constructor(
    public readonly tonic: NoteName,
    public readonly mode: Mode,
  ) {}

  static fromJSON(json: KeyJSON): Key {
    const mode = json.mode === "Major" ? Mode.MAJOR : Mode.MINOR;
    return new Key(NoteName.fromJSON(json.tonic), mode);
  }

  toJSON(): KeyJSON {
    return {
      tonic: this.tonic.toJSON(),
      mode: this.mode.name as "Major" | "Minor",
    };
  }

  static parse(name: string): Key {
    const parts = name.split(" ");
    if (parts.length !== 2) throw new Error(`Invalid key format: ${name}`);
    return new Key(NoteName.parse(parts[0]), Mode.parse(parts[1]));
  }

  name(): string {
    return `${this.tonic.name()} ${this.mode.name}`;
  }

  calculateScalePitch(intervalStepVal: number): Pitch {
    const signatureNum = this.tonic.value + this.mode.offset;

    const invStep = ((intervalStepVal % 7) + 7) % 7;

    const rPos = ((signatureNum % 7) + 7) % 7;
    const qPos = Math.floor(signatureNum / 7);

    const currentAlters = [];
    for (let i = 0; i < 7; i++) {
      currentAlters.push(i < rPos ? qPos + 1 : qPos);
    }

    const stepsMap = [3, 0, 4, 1, 5, 2, 6];
    let targetAlter = 0;

    for (let idx = 0; idx < stepsMap.length; idx++) {
      if (stepsMap[idx] === invStep) {
        targetAlter = currentAlters[idx];
        break;
      }
    }

    const baseFifth = C_MAJOR_FIFTHS[invStep];
    const baseOctave = C_MAJOR_BASE_OCTAVES[invStep];
    const octaveShift = Math.floor(intervalStepVal / 7);

    const finalFifth = baseFifth + 7 * targetAlter;
    const finalOctave = baseOctave + octaveShift + -4 * targetAlter;

    return new Pitch(new NoteName(finalFifth), new Octave(finalOctave));
  }

  calculateDegree(note: NoteName): { step: number; alter: number } {
    const r = note.value - this.tonic.value;
    const alterVal = Math.round((r - this.mode.offset - 2) / 7);
    const r0 = r - 7 * alterVal;
    const stepVal = (((4 * r0) % 7) + 7) % 7;
    return { step: stepVal, alter: alterVal };
  }
}

export class TimeSignature {
  constructor(
    public readonly beats: number,
    public readonly beatType: Duration,
  ) {}

  static fromJSON(json: TimeSignatureJSON): TimeSignature {
    return new TimeSignature(json.beats, Duration.fromJSON(json.beat_type));
  }

  toJSON(): TimeSignatureJSON {
    return {
      beats: this.beats,
      beat_type: this.beatType.toJSON(),
    };
  }

  toString(): string {
    const durationVal = this.beatType.val;
    const beatVal = Math.round(4 / durationVal);
    return `${this.beats}/${beatVal}`;
  }
}

export class ScoreAttrs {
  constructor(public readonly isTiedStart: boolean) {}

  static fromJSON(json: ScoreAttrsJSON): ScoreAttrs {
    return new ScoreAttrs(json.is_tied_start);
  }

  toJSON(): ScoreAttrsJSON {
    return { is_tied_start: this.isTiedStart };
  }
}

export class Accidental {
  constructor(public readonly type: "n" | "#" | "b" | "##" | "bb") {}

  static fromAlter(alter: number): Accidental | null {
    if (alter === 0) return new Accidental("n");
    if (alter === 1) return new Accidental("#");
    if (alter === -1) return new Accidental("b");
    if (alter === 2) return new Accidental("##");
    if (alter === -2) return new Accidental("bb");
    return null;
  }
}

export class Rest {
  static fromJSON(_json: unknown): Rest {
    return new Rest();
  }
  toJSON(): RestJSON {
    return {};
  }
}

export class Note {
  constructor(
    public readonly value: Pitch | Rest,
    public readonly duration: Duration,
    public readonly attribute: ScoreAttrs,
  ) {}

  static fromJSON(json: NoteJSON): Note {
    let value: Pitch | Rest;
    const val = json.value as PitchJSON; // Type assertion to PitchJSON for clearer access
    if (val.note_name?.value !== undefined && val.octave?.value !== undefined) {
      value = Pitch.fromJSON(val);
    } else {
      value = Rest.fromJSON(val);
    }
    return new Note(
      value,
      Duration.fromJSON(json.duration),
      ScoreAttrs.fromJSON(json.attribute),
    );
  }

  toJSON(): NoteJSON {
    return {
      value: this.value.toJSON(),
      duration: this.duration.toJSON(),
      attribute: this.attribute.toJSON(),
    };
  }

  isRest(): boolean {
    return this.value instanceof Rest;
  }

  withDuration(newDuration: Duration): Note {
    return new Note(this.value, newDuration, this.attribute);
  }

  withValue(newValue: Pitch | Rest): Note {
    return new Note(newValue, this.duration, this.attribute);
  }

  withAttribute(newAttribute: ScoreAttrs): Note {
    return new Note(this.value, this.duration, newAttribute);
  }
}

export class Measure {
  constructor(public readonly notes: Note[]) {}

  static fromJSON(json: MeasureJSON): Measure {
    return new Measure(json.notes.map((n) => Note.fromJSON(n)));
  }

  toJSON(): MeasureJSON {
    return { notes: this.notes.map((n) => n.toJSON()) };
  }

  calculateAccidentals(key: Key): (Accidental | null)[] {
    const results: (Accidental | null)[] = [];
    const context: Record<number, number> = {};

    for (const note of this.notes) {
      if (note.isRest()) {
        results.push(null);
        continue;
      }

      const pitch = note.value as Pitch;
      const step = pitch.getIntervalStepFromC4();
      const { alter } = pitch.internationalPitchNotation();

      let needsAccidental = false;

      if (step in context) {
        const prevAlter = context[step];
        if (prevAlter !== alter) {
          needsAccidental = true;
        }
      } else {
        const signatureNum = key.tonic.value + key.mode.offset;
        const { step: stepStr } = pitch.internationalPitchNotation();
        const stepIndexMap: Record<string, number> = {
          C: 0,
          D: 1,
          E: 2,
          F: 3,
          G: 4,
          A: 5,
          B: 6,
        };
        const stepIndex = stepIndexMap[stepStr];

        const rPos = ((signatureNum % 7) + 7) % 7;
        const qPos = Math.floor(signatureNum / 7);
        const alters = [];
        for (let i = 0; i < 7; i++) {
          alters.push(i < rPos ? qPos + 1 : qPos);
        }

        const stepToAltersIndex = [1, 3, 5, 0, 2, 4, 6];
        const expectedAlter = alters[stepToAltersIndex[stepIndex]];

        if (expectedAlter !== alter) {
          needsAccidental = true;
        }
      }

      if (needsAccidental) {
        results.push(Accidental.fromAlter(alter));
        context[step] = alter;
      } else {
        results.push(null);
        context[step] = alter;
      }
    }
    return results;
  }

  replaceNote(index: number, newNote: Note): Measure {
    if (index < 0 || index >= this.notes.length) return this;
    const newNotes = [...this.notes];
    newNotes[index] = newNote;
    return new Measure(newNotes);
  }

  insertNote(index: number, note: Note): Measure {
    const newNotes = [...this.notes];
    newNotes.splice(index, 0, note);
    return new Measure(newNotes);
  }

  removeNote(index: number): Measure {
    const newNotes = [...this.notes];
    newNotes.splice(index, 1);
    return new Measure(newNotes);
  }

  withNotes(newNotes: Note[]): Measure {
    return new Measure(newNotes);
  }

  // --- Domain Logic ---

  changeDuration(index: number, newDuration: Duration): Measure {
    if (index < 0 || index >= this.notes.length) return this;
    const note = this.notes[index];
    const oldDuration = note.duration;

    if (newDuration.equals(oldDuration)) return this;

    const newNotes = [...this.notes];

    // 短くする場合（隙間を休符で埋める）
    if (newDuration.lt(oldDuration)) {
      const updatedNote = note.withDuration(newDuration);
      newNotes[index] = updatedNote;

      // 1. 現在の音符の開始位置（拍数）を計算する
      let currentPos = 0;
      for (let i = 0; i < index; i++) {
        currentPos += this.notes[i].duration.val;
      }

      // 2. 埋めるべき空白の範囲
      // 例: 全音符(4.0) -> 8分音符(0.5) の場合、0.5 から 4.0 までを埋める
      let gapStart = currentPos + newDuration.val;
      const gapEnd = currentPos + oldDuration.val;
      const epsilon = 0.001; // 浮動小数点の誤差対策

      const newRests: Note[] = [];
      // 試行する休符のリスト（大きい順）: 全, 2分, 4分, 8分, 16分, 32分
      const standardDurations = [4.0, 2.0, 1.0, 0.5, 0.25, 0.125];

      while (gapStart < gapEnd - epsilon) {
        let found = false;

        for (const dur of standardDurations) {
          // 条件A: その休符が残り時間に入りきること
          if (gapStart + dur > gapEnd + epsilon) continue;

          // 条件B: グリッドに合っていること（Start位置が Duration の倍数であること）
          // 例: 4分休符(1.0)は、1.0, 2.0, 3.0... の位置にしか置けない
          const remainder = gapStart % dur;
          const isAligned =
            Math.abs(remainder) < epsilon ||
            Math.abs(remainder - dur) < epsilon;

          if (isAligned) {
            newRests.push(
              new Note(
                new Rest(),
                Duration.fromValue(dur),
                new ScoreAttrs(false),
              ),
            );
            gapStart += dur;
            found = true;
            break; // 次の休符を探すへ
          }
        }

        // 安全策: どの標準休符もハマらなかった場合（連符など）、残りをそのまま埋める
        if (!found) {
          const remaining = gapEnd - gapStart;
          if (remaining > epsilon) {
            newRests.push(
              new Note(
                new Rest(),
                Duration.fromValue(remaining),
                new ScoreAttrs(false),
              ),
            );
          }
          break;
        }
      }

      // 生成された休符を挿入
      newNotes.splice(index + 1, 0, ...newRests);
    } else {
      // 長くする場合（既存のロジックを維持）
      const diff = newDuration.sub(oldDuration);
      let needed = diff;
      const notesToRemove: number[] = [];

      for (let i = index + 1; i < newNotes.length && needed.val > 0.0001; i++) {
        const nextNote = newNotes[i];
        if (nextNote.duration.val <= needed.val + 0.0001) {
          needed = needed.sub(nextNote.duration);
          notesToRemove.push(i);
        } else {
          const newNextDur = nextNote.duration.sub(needed);
          const modNext = nextNote.withDuration(newNextDur);
          newNotes[i] = modNext;
          needed = new Duration(0, 1);
        }
      }

      if (needed.val > 0.0001) {
        return this; // 足りない場合は変更キャンセル
      }

      const updatedNote = note.withDuration(newDuration);
      newNotes[index] = updatedNote;

      for (let i = notesToRemove.length - 1; i >= 0; i--) {
        newNotes.splice(notesToRemove[i], 1);
      }
    }

    return new Measure(newNotes);
  }

  toggleDot(index: number): Measure {
    if (index < 0 || index >= this.notes.length) return this;
    const note = this.notes[index];

    // ★修正3: 厳密な判定とDuration演算に変更
    // 付点がついているかの判定:
    // 単純化された Duration において、分子が3の倍数であれば付点音符である可能性が高い
    // (例: 付点4分=1.5=3/2, 付点2分=3=3/1, 付点8分=0.75=3/4)
    // ただし、連符などが混ざるとこの判定は不完全だが、通常の2進的な音価ではこれで十分機能する

    const num = note.duration.numerator;
    const isDotted = num % 3 === 0;

    let newDuration: Duration;

    if (isDotted) {
      // 付点を外す: 現在の長さを 2/3 倍にする
      // (Durationクラスには掛け算がないため、分子分母を操作して作成)
      // Duration(n, d) * 2/3 = Duration(n*2, d*3)
      const base = new Duration(
        note.duration.numerator * 2,
        note.duration.denominator * 3,
      );
      newDuration = base.simplify();
    } else {
      // 付点をつける: 現在の長さを 3/2 倍にする
      // Duration(n, d) * 3/2 = Duration(n*3, d*2)
      const base = new Duration(
        note.duration.numerator * 3,
        note.duration.denominator * 2,
      );
      newDuration = base.simplify();
    }

    return this.changeDuration(index, newDuration);
  }

  resolvePitch(targetPitch: Pitch, noteIndex: number): Pitch {
    // Resolves the target pitch (usually diatonic) against the context of the measure.
    // Finds if any previous note in the measure shares the same diatonic step/octave but has a different accidental.

    const targetStep = targetPitch.getIntervalStepFromC4();
    let foundAlter: number | null = null;

    for (let i = 0; i < noteIndex; i++) {
      const prevNote = this.notes[i];
      if (!prevNote.isRest()) {
        const prevPitch = prevNote.value as Pitch;
        const prevStep = prevPitch.getIntervalStepFromC4();
        if (prevStep === targetStep) {
          const { alter } = prevPitch.internationalPitchNotation();
          foundAlter = alter;
        }
      }
    }

    if (foundAlter !== null) {
      // Reconstruct pitch with foundAlter
      // Note: This logic replicates calculateScalePitch's accidental application
      // But here we force the accidental.

      const invStep = ((targetStep % 7) + 7) % 7;
      const baseFifth = C_MAJOR_FIFTHS[invStep];
      const baseOctave = C_MAJOR_BASE_OCTAVES[invStep];
      const octaveShift = Math.floor(targetStep / 7);

      const finalFifth = baseFifth + 7 * foundAlter;
      const finalOctave = baseOctave + octaveShift + -4 * foundAlter;

      return new Pitch(new NoteName(finalFifth), new Octave(finalOctave));
    }

    return targetPitch;
  }
}

export class FullScore {
  constructor(
    public readonly parts: Record<string, Measure[]>,
    public readonly key: Key,
    public readonly timeSignature: TimeSignature,
  ) {}

  static fromJSON(json: FullScoreJSON): FullScore {
    const parts: Record<string, Measure[]> = {};
    for (const [key, value] of Object.entries(json.body.parts)) {
      if (value) {
        parts[key] = value.map((m) => Measure.fromJSON(m));
      }
    }
    return new FullScore(
      parts,
      Key.fromJSON(json.key),
      TimeSignature.fromJSON(json.time_signature),
    );
  }

  toJSON(): FullScoreJSON {
    const parts: Record<string, MeasureJSON[]> = {};
    for (const [key, value] of Object.entries(this.parts)) {
      parts[key] = value.map((m) => m.toJSON());
    }
    return {
      body: { parts },
      key: this.key.toJSON(),
      time_signature: this.timeSignature.toJSON(),
    };
  }

  getPart(partKey: string): Measure[] | undefined {
    return this.parts[partKey];
  }

  getMeasure(partKey: string, measureIndex: number): Measure | undefined {
    return this.parts[partKey]?.[measureIndex];
  }

  getNote(
    partKey: string,
    measureIndex: number,
    noteIndex: number,
  ): Note | undefined {
    return this.parts[partKey]?.[measureIndex]?.notes[noteIndex];
  }

  withPart(partKey: string, measures: Measure[]): FullScore {
    return new FullScore(
      { ...this.parts, [partKey]: measures },
      this.key,
      this.timeSignature,
    );
  }

  withKey(key: Key): FullScore {
    return new FullScore(this.parts, key, this.timeSignature);
  }

  updateMeasure(
    partKey: string,
    measureIndex: number,
    newMeasure: Measure,
  ): FullScore {
    const measures = this.parts[partKey];
    if (!measures) return this;

    const newMeasures = [...measures];
    newMeasures[measureIndex] = newMeasure;
    return this.withPart(partKey, newMeasures);
  }
}
