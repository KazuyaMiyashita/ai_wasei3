from my_project.model import (
    Duration,
    Interval,
    Measure,
    Melody,
    Note,
    PartId,
    Pitch,
    Score,
)


def _create_test_data() -> tuple[
    tuple[Melody[Note[Pitch | None, None]], Melody[Note[Pitch | None, None]], Melody[Note[Pitch | None, None]]],
    tuple[Melody[Note[Pitch | None, None]], Melody[Note[Pitch | None, None]], Melody[Note[Pitch | None, None]]],
]:
    soprano_bar1: Melody[Note[Pitch | None, None]] = Melody.of(
        Note(None, Duration.of(1), None),
        Note(Pitch.parse("G4"), Duration.of(1), None),
        Note(Pitch.parse("C5"), Duration.of(2), None),
    )

    soprano_bar2: Melody[Note[Pitch | None, None]] = Melody.of(
        Note(Pitch.parse("C5"), Duration.of(1), None),
        Note(Pitch.parse("D5"), Duration.of(1), None),
        Note(Pitch.parse("B4"), Duration.of(2), None),
    )

    soprano_bar3: Melody[Note[Pitch | None, None]] = Melody.of(
        Note(Pitch.parse("C5"), Duration.of(2), None),
        Note(None, Duration.of(2), None),
    )

    alto_bar1: Melody[Note[Pitch | None, None]] = Melody.of(
        Note(Pitch.parse("C4"), Duration.of(2), None),
        Note(Pitch.parse("E4"), Duration.of(2), None),
    )

    alto_bar2: Melody[Note[Pitch | None, None]] = Melody.of(
        Note(Pitch.parse("D4"), Duration.of(2), None),
        Note(Pitch.parse("G4"), Duration.of(1), None),
        Note(Pitch.parse("F4"), Duration.of(1), None),
    )

    alto_bar3: Melody[Note[Pitch | None, None]] = Melody.of(
        Note(Pitch.parse("E4"), Duration.of(2), None),
        Note(None, Duration.of(2), None),
    )
    return (soprano_bar1, soprano_bar2, soprano_bar3), (alto_bar1, alto_bar2, alto_bar3)


def test_score_measures() -> None:
    (s1, s2, s3), (a1, a2, a3) = _create_test_data()

    # Create Score with explicit measures
    score: Score[PartId, Pitch | None, None] = Score(
        {
            PartId.SOPRANO: [Measure(s1), Measure(s2), Measure(s3)],
            PartId.ALTO: [Measure(a1), Measure(a2), Measure(a3)],
        }
    )

    # 1. Horizontal access
    # Access measure 2 of Soprano
    assert score.part(PartId.SOPRANO)[1].melody == s2

    # 2. Vertical access (Full score)
    vertical_view = score.to_vertical_view()
    assert len(vertical_view) == 9

    # Check specific moment (Measure 2 start) -> Index 3
    # Measure 1 (3 moments) + Measure 2 start
    # M1: 0-1, 1-2, 2-4 (3 moments)
    # M2: 4-5 (1st moment of M2) -> Index 3
    moment_m2_start = vertical_view[3]
    assert moment_m2_start.duration == Duration.of(1)
    assert moment_m2_start.get(PartId.SOPRANO) == Pitch.parse("C5")
    assert moment_m2_start.get(PartId.ALTO) == Pitch.parse("D4")

    # 3. Measure slicing and vertical access
    # Extract only Measure 2
    score_m2 = score.measure(1)
    assert score_m2.num_measures == 1

    vertical_view_m2 = score_m2.to_vertical_view()

    # Measure 2:
    # Sop: [C5(1), D5(1), B4(2)]
    # Alt: [D4(2),        G4(1), F4(1)]
    # Slices:
    # 0-1: C5, D4
    # 1-2: D5, D4
    # 2-3: B4, G4
    # 3-4: B4, F4
    # -> 4 moments
    assert len(vertical_view_m2) == 4

    moment_0_of_m2 = vertical_view_m2[0]
    assert moment_0_of_m2.get(PartId.SOPRANO) == Pitch.parse("C5")
    assert moment_0_of_m2.get(PartId.ALTO) == Pitch.parse("D4")

    # 5. Check vertical intervals of Measure 2
    actual_intervals = []
    expected_intervals = ["m7", "P8", "M3", "A4"]
    for moment in vertical_view_m2:
        sop_pitch = moment.get(PartId.SOPRANO)
        alto_pitch = moment.get(PartId.ALTO)

        if sop_pitch is not None and alto_pitch is not None:
            # Determine base and target for interval calculation
            if sop_pitch.num().value >= alto_pitch.num().value:
                base_pitch = alto_pitch
                target_pitch = sop_pitch
            else:
                base_pitch = sop_pitch
                target_pitch = alto_pitch
            actual_intervals.append(Interval.of(base_pitch, target_pitch).name())
        else:
            # For this test, we assume no rests within the active parts of moments
            actual_intervals.append("N/A")

    assert actual_intervals == expected_intervals

    # 4. Reconstruction (Flattened)
    reconstructed_m2 = vertical_view_m2.to_flat_score()
    # Since to_flat_score returns 1 measure containing the whole melody
    assert len(reconstructed_m2.part(PartId.SOPRANO)) == 1
    assert reconstructed_m2.part(PartId.SOPRANO)[0].melody == s2
