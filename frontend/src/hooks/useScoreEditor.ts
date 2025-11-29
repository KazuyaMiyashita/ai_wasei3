import { useCallback, useEffect, useState } from "react";
import demoJson from "../assets/demo.json";
import { CONFIG, PART_NAMES } from "../lib/constants";
import {
  Duration,
  FullScore,
  type FullScoreJSON,
  Key,
  Measure,
  type MeasureJSON,
  Mode,
  Note,
  type NoteJSON,
  NoteName,
  Octave,
  Pitch,
  type PitchJSON,
  Rest,
  ScoreAttrs,
  TimeSignature,
} from "../lib/model";

export interface Selection {
  part: number;
  measure: number;
  note: number;
}

// --- Helper: Convert JSON to Domain Model ---

function loadScoreFromJSON(json: FullScoreJSON): FullScore {
  const parts: Record<string, Measure[]> = {};

  const jsonParts = json.body.parts;
  for (const partKey in jsonParts) {
    const measureList = jsonParts[partKey];
    if (measureList) {
      parts[partKey] = measureList.map((mJson: MeasureJSON) => {
        const notes = mJson.notes.map((nJson: NoteJSON) => {
          let val: Pitch | Rest;
          const vPitch = nJson.value as PitchJSON;
          if (
            vPitch.note_name?.value !== undefined &&
            vPitch.octave?.value !== undefined
          ) {
            val = new Pitch(
              new NoteName(vPitch.note_name.value),
              new Octave(vPitch.octave.value),
            );
          } else {
            val = new Rest();
          }

          const dur = new Duration(
            nJson.duration.value.numerator,
            nJson.duration.value.denominator,
          );
          const attr = new ScoreAttrs(nJson.attribute.is_tied_start);
          return new Note(val, dur, attr);
        });
        return new Measure(notes);
      });
    }
  }

  const keyTonicVal = json.key.tonic.value;
  const keyModeStr = json.key.mode;
  const key = new Key(
    new NoteName(keyTonicVal),
    keyModeStr === "Major" ? Mode.MAJOR : Mode.MINOR,
  );

  const tsBeats = json.time_signature.beats;
  const tsType = new Duration(
    json.time_signature.beat_type.value.numerator,
    json.time_signature.beat_type.value.denominator,
  );
  const timeSignature = new TimeSignature(tsBeats, tsType);

  return new FullScore(parts, key, timeSignature);
}

export function useScoreEditor() {
  const [score, setScore] = useState<FullScore | null>(null);
  const [selection, setSelection] = useState<Selection | null>(null);

  const initScore = useCallback(() => {
    try {
      const newScore = loadScoreFromJSON(demoJson as unknown as FullScoreJSON);
      console.log("Loaded demo score:", newScore);
      setScore(newScore);
    } catch (e) {
      console.error("Failed to load score", e);
    }
  }, []);

  useEffect(() => {
    initScore();
  }, [initScore]);

  const updateSelectionPart = useCallback((delta: number) => {
    setSelection((prev) => {
      if (!prev) return prev;
      const newPart = prev.part + delta;
      if (newPart >= 0 && newPart < PART_NAMES.length) {
        return { ...prev, part: newPart };
      }
      return prev;
    });
  }, []);

  const updateSelectionNote = useCallback(
    (delta: number) => {
      if (!score) return;
      setSelection((prev) => {
        if (!prev) return prev;
        const partKey = PART_NAMES[prev.part].toUpperCase();
        const measures = score.parts[partKey];
        if (!measures) return prev;

        const measure = measures[prev.measure];

        if (prev.note === -1) {
          if (delta > 0) return { ...prev, note: 0 };
          return prev;
        }

        const newNoteIdx = prev.note + delta;

        if (newNoteIdx >= 0 && newNoteIdx < measure.notes.length) {
          return { ...prev, note: newNoteIdx };
        }

        if (newNoteIdx >= measure.notes.length) {
          if (prev.measure < CONFIG.measureCount - 1) {
            return { part: prev.part, measure: prev.measure + 1, note: 0 };
          }
        } else if (newNoteIdx < 0) {
          if (prev.measure > 0) {
            const prevMeasure = measures[prev.measure - 1];
            return {
              part: prev.part,
              measure: prev.measure - 1,
              note: prevMeasure.notes.length - 1,
            };
          }
        }
        return prev;
      });
    },
    [score],
  );

  // --- Core Mutation Logic ---

  const modifyMeasure = useCallback(
    (modifier: (measure: Measure) => Measure) => {
      if (!score || !selection) return;
      const partKey = PART_NAMES[selection.part].toUpperCase();
      const currentMeasure = score.getMeasure(partKey, selection.measure);
      if (!currentMeasure) return;

      const newMeasure = modifier(currentMeasure);
      const newScore = score.updateMeasure(
        partKey,
        selection.measure,
        newMeasure,
      );
      setScore(newScore);
    },
    [score, selection],
  );

  const modifyNote = useCallback(
    (modifier: (note: Note) => Note | null) => {
      if (!score || !selection || selection.note === -1) return;
      const partKey = PART_NAMES[selection.part].toUpperCase();
      const measure = score.getMeasure(partKey, selection.measure);
      const note = score.getNote(partKey, selection.measure, selection.note);

      if (!measure || !note) return;

      const newNote = modifier(note);
      if (newNote) {
        const newMeasure = measure.replaceNote(selection.note, newNote);
        const newScore = score.updateMeasure(
          partKey,
          selection.measure,
          newMeasure,
        );
        setScore(newScore);
      }
    },
    [score, selection],
  );

  const updateNoteDuration = useCallback(
    (targetVal: number) => {
      if (!selection) return;
      const noteIndex = selection.note;
      const newDuration = Duration.fromValue(targetVal);
      modifyMeasure((m) => m.changeDuration(noteIndex, newDuration));
    },
    [modifyMeasure, selection],
  );

  const toggleDot = useCallback(() => {
    if (!selection) return;
    const noteIndex = selection.note;
    modifyMeasure((m) => m.toggleDot(noteIndex));
  }, [modifyMeasure, selection]);

  const updateNotePitch = useCallback(
    (delta: number) => {
      if (!score || !selection || selection.note === -1) return;
      const partKey = PART_NAMES[selection.part].toUpperCase();
      const measure = score.getMeasure(partKey, selection.measure);
      if (!measure) return;

      const note = measure.notes[selection.note];
      if (note.isRest()) return;
      const pitch = note.value as Pitch;

      const currentStep = pitch.getIntervalStepFromC4();
      const newStep = currentStep + delta;

      // 1. Calculate Diatonic Pitch
      const diatonicPitch = score.key.calculateScalePitch(newStep);

      // 2. Resolve Context (Accidentals)
      const resolvedPitch = measure.resolvePitch(diatonicPitch, selection.note);

      const newNote = note.withValue(resolvedPitch);
      const newMeasure = measure.replaceNote(selection.note, newNote);
      const newScore = score.updateMeasure(
        partKey,
        selection.measure,
        newMeasure,
      );
      setScore(newScore);
    },
    [score, selection],
  );

  const updateNoteFixedPitch = useCallback(
    (stepName: string) => {
      if (!score || !selection || selection.note === -1) return;
      const partKey = PART_NAMES[selection.part].toUpperCase();
      const measure = score.getMeasure(partKey, selection.measure);
      if (!measure) return;
      const note = measure.notes[selection.note];

      const stepMap: Record<string, number> = {
        c: 0,
        d: 1,
        e: 2,
        f: 3,
        g: 4,
        a: 5,
        b: 6,
      };
      const targetBaseStep = stepMap[stepName.toLowerCase()];
      if (targetBaseStep === undefined) return;

      let currentStep = 0;
      if (!note.isRest()) {
        currentStep = (note.value as Pitch).getIntervalStepFromC4();
      } else {
        currentStep = 0; // Default C4
      }

      const k = Math.round((currentStep - targetBaseStep) / 7);
      const newStep = targetBaseStep + 7 * k;

      const diatonicPitch = score.key.calculateScalePitch(newStep);
      const resolvedPitch = measure.resolvePitch(diatonicPitch, selection.note);

      const newNote = note.withValue(resolvedPitch);
      const newMeasure = measure.replaceNote(selection.note, newNote);
      setScore(score.updateMeasure(partKey, selection.measure, newMeasure));
    },
    [score, selection],
  );

  const updateNoteAlter = useCallback(
    (direction: number) => {
      modifyNote((note) => {
        if (note.isRest()) return null;
        const pitch = note.value as Pitch;
        const newPitch = pitch.transposeChromatic(direction);
        return newPitch ? note.withValue(newPitch) : null;
      });
    },
    [modifyNote],
  );

  const updateNoteAccidental = useCallback(
    (accidental: number) => {
      modifyNote((note) => {
        if (note.isRest()) return null;
        const pitch = note.value as Pitch;
        const { step, octave } = pitch.internationalPitchNotation();
        const newPitch = Pitch.fromInternalPitchNotation(
          step,
          accidental,
          octave,
        );
        return note.withValue(newPitch);
      });
    },
    [modifyNote],
  );

  const updateKey = useCallback(
    (tonicVal: number, modeName: "Major" | "Minor") => {
      if (!score) return;
      const newKey = new Key(
        new NoteName(tonicVal),
        modeName === "Major" ? Mode.MAJOR : Mode.MINOR,
      );
      setScore(score.withKey(newKey));
    },
    [score],
  );

  const updateNoteOctave = useCallback(
    (delta: number) => {
      modifyNote((note) => {
        if (note.isRest()) return null;
        const pitch = note.value as Pitch;
        return note.withValue(
          new Pitch(pitch.noteName, pitch.octave.add(new Octave(delta))),
        );
      });
    },
    [modifyNote],
  );

  const toggleTie = useCallback(() => {
    modifyNote((note) => {
      if (note.isRest()) return null;
      const newAttr = new ScoreAttrs(!note.attribute.isTiedStart);
      return note.withAttribute(newAttr);
    });
  }, [modifyNote]);

  const updateNoteToRest = useCallback(() => {
    modifyNote((note) => {
      if (note.isRest()) return null;
      return note.withValue(new Rest());
    });
  }, [modifyNote]);

  return {
    score,
    setScore,
    selection,
    setSelection,
    updateSelectionPart,
    updateSelectionNote,
    updateNotePitch,
    updateNoteFixedPitch,
    updateNoteAlter,
    updateNoteAccidental,
    updateNoteOctave,
    updateNoteDuration,
    toggleDot,
    toggleTie,
    updateNoteToRest,
    initScore,
    updateKey,
  };
}
