// Review Comment:
// backend22のモデルを移植する場所です。
// 音楽理論的なプリミティブはUIや編集ロジックから独立しているべきです。
// cf: backend22/src/main/scala/model/elements/Elements.scala

export type Key = {
  tonic: number; // TODO
  mode: "major" | "minor";
};

export enum NoteNameEnum {
  C = 0,
  D = 1,
  E = 2,
  F = 3,
  G = 4,
  A = 5,
  B = 6,
}

export class NoteName {
  constructor(public readonly value: NoteNameEnum) {}

  static fromString(pname: string): NoteName {
    switch (pname.toLowerCase()) {
      case "c":
        return new NoteName(NoteNameEnum.C);
      case "d":
        return new NoteName(NoteNameEnum.D);
      case "e":
        return new NoteName(NoteNameEnum.E);
      case "f":
        return new NoteName(NoteNameEnum.F);
      case "g":
        return new NoteName(NoteNameEnum.G);
      case "a":
        return new NoteName(NoteNameEnum.A);
      case "b":
        return new NoteName(NoteNameEnum.B);
      default:
        throw new Error(`Invalid pname: ${pname}`);
    }
  }

  toString(): string {
    return NoteNameEnum[this.value].toLowerCase();
  }

  next(): NoteName {
    return new NoteName((this.value + 1) % 7);
  }

  prev(): NoteName {
    return new NoteName((this.value + 6) % 7);
  }
}

export class Octave {
  constructor(public readonly value: number) {}

  next(): Octave {
    return new Octave(this.value + 1);
  }

  prev(): Octave {
    return new Octave(this.value - 1);
  }
}

export class Pitch {
  octave: Octave;
  noteName: NoteName;

  constructor(octave: Octave | number, noteName: NoteName | string) {
    this.octave = typeof octave === "number" ? new Octave(octave) : octave;
    this.noteName =
      typeof noteName === "string" ? NoteName.fromString(noteName) : noteName;
  }

  stepUp(): Pitch {
    const nextNote = this.noteName.next();
    // If going from B to C, octave increases
    if (
      this.noteName.value === NoteNameEnum.B &&
      nextNote.value === NoteNameEnum.C
    ) {
      return new Pitch(this.octave.next(), nextNote);
    }
    return new Pitch(this.octave, nextNote);
  }

  stepDown(): Pitch {
    const prevNote = this.noteName.prev();
    // If going from C to B, octave decreases
    if (
      this.noteName.value === NoteNameEnum.C &&
      prevNote.value === NoteNameEnum.B
    ) {
      return new Pitch(this.octave.prev(), prevNote);
    }
    return new Pitch(this.octave, prevNote);
  }
}
