import "@typespec/http";
import "@typespec/rest";

using TypeSpec.Http;
using TypeSpec.Rest;

@service(#{
  title: "Counterpoint Service",
})
namespace CounterpointService;

model NoteName {
  value: int32;
}

model Octave {
  value: int32;
}

model Pitch {
  octave: Octave;
  note_name: NoteName;
}

enum Mode {
  Major,
  Minor,
}

model Key {
  tonic: NoteName;
  mode: Mode;
}

model Fraction {
  numerator: int32;
  denominator: int32;
}

model Duration {
  value: Fraction;
}

model ScoreAttrs {
  is_tied_start: boolean;
}

model Rest {}

model Note {
  value: Pitch | Rest;
  duration: Duration;
  attribute: ScoreAttrs;
}

model Measure {
  notes: Note[];
}

// Representing the keys in the parts map. 
// Python Enum PartId (SOPRANO=1, etc) might be serialized as values or strings.
// We assume string keys for JSON friendless.
enum PartId {
  SOPRANO,
  ALTO,
  TENOR,
  BASS,
}

model Score {
  // Map of PartId to list of Measures
  parts: Record<Measure[]>;
}

model TimeSignature {
  beats: int32;
  beat_type: Duration;
}

model FullScore {
  key: Key;
  time_signature: TimeSignature;
  body: Score;
}

model CounterpointRequest {
  cf: string[];
  key: string;
  cf_part_id: PartId;
  part_id: PartId;
  species: Species;
}

enum Species {
  first,
  second,
  third,
  fourth,
  fifth,
}

@route("/counterpoint")
@post
op generateCounterpoint(@body request: CounterpointRequest): FullScore;
