import logging
import traceback
from fractions import Fraction as PyFraction
from typing import Any

from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware

import my_project.model as domain
from my_project.counterpoint.counterpoint_generator import CounterpointGenerator
from my_project.counterpoint.model import Species
from my_project.logging_utils import PackagePathFilter
from my_project.server import generated_models as gen

logger = logging.getLogger(__name__)
logging.basicConfig(
    level="DEBUG",
    format="[%(levelname)s] %(asctime)s.%(msecs)03d - %(short_name)s -- %(message)s",
    datefmt="%H:%M:%S",
)
root_logger = logging.getLogger()
for handler in root_logger.handlers:
    handler.addFilter(PackagePathFilter())

app = FastAPI()

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


# --- Converters ---


def to_gen_fraction(f: PyFraction) -> gen.Fraction:
    return gen.Fraction(numerator=f.numerator, denominator=f.denominator)


def to_gen_notename(n: domain.NoteName) -> gen.NoteName:
    return gen.NoteName(value=n.value)


def to_gen_octave(o: domain.Octave) -> gen.Octave:
    return gen.Octave(value=o.value)


def to_gen_pitch(p: domain.Pitch) -> gen.Pitch:
    return gen.Pitch(octave=to_gen_octave(p.octave), note_name=to_gen_notename(p.note_name))


def to_gen_duration(d: domain.Duration) -> gen.Duration:
    return gen.Duration(value=to_gen_fraction(d.value))


def to_gen_score_attrs(a: Any) -> gen.ScoreAttrs:
    # If attribute is None or doesn't have is_tied_start, default to False
    if a is None or not hasattr(a, "is_tied_start"):
        return gen.ScoreAttrs(is_tied_start=False)
    return gen.ScoreAttrs(is_tied_start=a.is_tied_start)


def to_gen_note(n: domain.Note[Any, Any]) -> gen.Note:
    gen_value: gen.Pitch | gen.Rest
    if n.value is None:
        gen_value = gen.Rest()
    else:
        gen_value = to_gen_pitch(n.value)

    return gen.Note(value=gen_value, duration=to_gen_duration(n.duration), attribute=to_gen_score_attrs(n.attribute))


def to_gen_measure(m: domain.Measure[Any]) -> gen.Measure:
    return gen.Measure(notes=[to_gen_note(n) for n in m.notes])


def to_gen_mode(m: domain.Mode) -> gen.Mode:
    if m == domain.Mode.MAJOR:
        return gen.Mode.Major
    return gen.Mode.Minor


def to_gen_key(k: domain.Key) -> gen.Key:
    return gen.Key(tonic=to_gen_notename(k.tonic), mode=to_gen_mode(k.mode))


def to_gen_time_signature(ts: domain.TimeSignature) -> gen.TimeSignature:
    return gen.TimeSignature(beats=ts.beats, beat_type=to_gen_duration(ts.beat_type))


def to_gen_full_score(fs: domain.FullScore[Any]) -> gen.FullScore:
    parts_dict = {}
    for pid, measures in fs.body.parts.items():
        # domain.PartId is Enum. Use name (e.g., "SOPRANO") as key.
        parts_dict[pid.name] = [to_gen_measure(m) for m in measures]

    return gen.FullScore(
        key=to_gen_key(fs.key),
        time_signature=to_gen_time_signature(fs.time_signature),
        body=gen.Score(parts=parts_dict),
    )


def to_domain_species(s: gen.Species) -> Species:
    if s == gen.Species.first:
        return Species.FIRST_SPECIES
    elif s == gen.Species.second:
        return Species.SECOND_SPECIES
    elif s == gen.Species.third:
        return Species.THIRD_SPECIES
    elif s == gen.Species.fourth:
        return Species.FOURTH_SPECIES
    elif s == gen.Species.fifth:
        return Species.FIFTH_SPECIES
    # Fallback, though Pydantic should prevent invalid values
    return Species.THIRD_SPECIES


# --- Endpoints ---


@app.post("/counterpoint", response_model=gen.FullScore)
def generate_counterpoint(request: gen.CounterpointRequest) -> gen.FullScore:
    try:
        # Parse inputs for harmony solve (placeholder for counterpoint)
        cf_sequence = [domain.Pitch.parse(p) for p in request.cf]

        # Key from string (e.g. "C Major")
        key = domain.Key.parse(request.key)

        cf_part_id = domain.PartId[request.cf_part_id.value]
        part_id = domain.PartId[request.part_id.value]

        species = to_domain_species(request.species)

        generator = CounterpointGenerator.default(
            cantus_firmus=cf_sequence,
            cf_part_id=cf_part_id,
            key=key,
            species=species,
            part_id=part_id,
        )
        # Get the first result
        try:
            result_score = next(generator.generate_scores())
            return to_gen_full_score(result_score)
        except StopIteration:
            raise HTTPException(status_code=404, detail="No solution found for the given input.")

    except HTTPException:
        raise
    except Exception as e:
        # Log error for debugging
        logger.error(f"Error processing request: {e}")
        traceback.print_exc()
        raise HTTPException(status_code=400, detail=str(e))
