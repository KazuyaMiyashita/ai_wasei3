import unittest

from my_project.counterpoint.measure_search.measure_step_sequence import MeasureStepSequence
from my_project.counterpoint.measure_search.rythmn_applyer import try_apply_rythmn
from my_project.counterpoint.model import MeasureRythmnPattern, NoteAnnotation, ToneType
from my_project.model import Duration, IntervalStep, Melody, Note


class TestTryApplyRythmn(unittest.TestCase):
    def test_successful_application(self) -> None:
        """正常系: 正しく適用されるケース"""
        seq = MeasureStepSequence.parse("0,1p,2p,3|0")
        pattern = MeasureRythmnPattern.R_4444
        result = try_apply_rythmn(seq, pattern)
        self.assertIsNotNone(result)
        expected_melody = Melody.of(
            Note(
                IntervalStep(0),
                Duration.of(1),
                NoteAnnotation(is_tied_start=False, tone_type=ToneType.HARMONIC_TONE),
            ),
            Note(
                IntervalStep(1),
                Duration.of(1),
                NoteAnnotation(is_tied_start=False, tone_type=ToneType.PASSING_TONE),
            ),
            Note(
                IntervalStep(2),
                Duration.of(1),
                NoteAnnotation(is_tied_start=False, tone_type=ToneType.PASSING_TONE),
            ),
            Note(
                IntervalStep(3),
                Duration.of(1),
                NoteAnnotation(is_tied_start=False, tone_type=ToneType.HARMONIC_TONE),
            ),
        )
        self.assertEqual(result, expected_melody)

    def test_note_count_mismatch(self) -> None:
        """異常系: 音符数が一致しないケース"""
        seq = MeasureStepSequence.parse("0,1p,2|0")  # 3 notes
        pattern = MeasureRythmnPattern.R_4444  # 4 notes
        result = try_apply_rythmn(seq, pattern)
        self.assertIsNone(result)

    def test_tie_mismatch(self) -> None:
        """異常系: タイの有無が一致しないケース"""
        # Sequence requires tie, but pattern does not have it
        seq = MeasureStepSequence.parse("0,1p,2|2")  # is_tied_to_next_measure_required is True
        pattern = MeasureRythmnPattern.R_244  # is_next_tied is False
        result = try_apply_rythmn(seq, pattern)
        self.assertIsNone(result)

        # Pattern has tie, but sequence does not require it
        seq = MeasureStepSequence.parse("0,1p,2|3")  # is_tied_to_next_measure_required is False
        pattern = MeasureRythmnPattern.R_244t  # is_next_tied is True
        result = try_apply_rythmn(seq, pattern)
        self.assertIsNone(result)

    def test_suspension_resolution(self) -> None:
        # 正常系: R_t22 は前にタイが付く。2分音符2つなので、3拍目に解決音が来る
        seq = MeasureStepSequence.parse("1r,0|1")
        pattern = MeasureRythmnPattern.R_t22
        result = try_apply_rythmn(seq, pattern)
        self.assertIsNotNone(result)
        expected_melody = Melody.of(
            Note(
                IntervalStep(1),
                Duration.of(2),
                NoteAnnotation(is_tied_start=False, tone_type=ToneType.SUSPENDED_TONE),
            ),
            Note(
                IntervalStep(0),
                Duration.of(2),
                NoteAnnotation(is_tied_start=False, tone_type=ToneType.HARMONIC_TONE),
            ),
        )
        self.assertEqual(result, expected_melody)

        # R_22 は前にタイが付かないのでNG
        pattern = MeasureRythmnPattern.R_22
        result = try_apply_rythmn(seq, pattern)
        self.assertIsNone(result)

        # 3拍目で解決しないのでNG
        seq = MeasureStepSequence.parse("1r,2srh,0|-1")
        pattern = MeasureRythmnPattern.R_t244
        result = try_apply_rythmn(seq, pattern)
        self.assertIsNone(result)


if __name__ == "__main__":
    unittest.main()
