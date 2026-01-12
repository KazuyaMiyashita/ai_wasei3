package composer.counterpoint.skeleton

import composer.counterpoint.model._
import composer.counterpoint.validation.Validator
import model.elements.{Degree, Duration, Key, Pitch}
import model.elements.Interval.IntervalStep
import model.elements.Degree.DegreeStep
import model.elements.Pitch.NoteName
import model.containers.{Melody, Note}
import model.elements.Key.Mode
import model.elements.Part

case class PitchWithBass(pitch: Pitch, bass: ChordWithBass[NoteName])

case class Skeleton(
    measures: List[Melody[PitchWithBass, Note[PitchWithBass]]],
)

class SkeletonGenerator(
    cantusFirmus: List[Pitch],
    key: Key,
    pitchRange: (Pitch, Pitch),
    rand: scala.util.Random,
    measureLength: Int,
) {

  private val MAX_RETRIES = 100

  def generateSkeleton(): Skeleton = {
    var attempt = 0
    while (attempt < MAX_RETRIES) {
      try {
        return generateSkeletonImpl()
      } catch {
        case e: Exception =>
          if (attempt == MAX_RETRIES - 1) throw e
      }
      attempt += 1
    }
    throw new RuntimeException("Unexpected error in generateSkeleton")
  }

  private def generateSkeletonImpl(): Skeleton = {
    var measures: List[Melody[PitchWithBass, Note[PitchWithBass]]] = Nil
    val chordPalette                                               = getChordPalette
    val candidatePitchesPool                                       = getAllPitchesInRange

    for ((cfPitch, measureIdx) <- cantusFirmus.zipWithIndex) {
      val cfDegree          = Degree.fromNoteNameKey(cfPitch.noteName, key)
      val currentMeasurePos = currentMeasurePosition(measureIdx)

      var chordCandidates: List[HarmonicDegreeChord] = Nil

      if (Set(MeasurePosition.FIRST, MeasurePosition.LAST).contains(currentMeasurePos)) {
        chordCandidates = List(DegreeChord.I)
      } else if (currentMeasurePos == MeasurePosition.PENULTIMATE) {
        if (cfDegree.step == DegreeStep.idx1(5)) {
          if (key.mode == Mode.Major) chordCandidates = List(DegreeChord.V)
          else chordCandidates = List(DegreeChord.V_leading)
        } else if (cfDegree.step == DegreeStep.idx1(2)) {
          if (key.mode == Mode.Major) chordCandidates = List(DegreeChord.II, DegreeChord.VII)
          else chordCandidates = List(DegreeChord.VII_leading)
        } else if (cfDegree.step == DegreeStep.idx1(7)) {
          if (key.mode == Mode.Major) chordCandidates = List(DegreeChord.V)
          else chordCandidates = List(DegreeChord.V_leading)
        } else {
          chordCandidates = filterChordsContaining(chordPalette, cfDegree)
        }
      } else {
        chordCandidates = filterChordsContaining(chordPalette, cfDegree)
      }

      if (chordCandidates.isEmpty) {
        chordCandidates = filterChordsContaining(chordPalette, cfDegree)
        if (chordCandidates.isEmpty) {
          throw new RuntimeException(s"No valid chord found for CF note $cfPitch ($cfDegree) at measure $measureIdx")
        }
      }

      chordCandidates = rand.shuffle(chordCandidates)
      var measureSucceeded = false

      val chordIterator = chordCandidates.iterator
      while (chordIterator.hasNext && !measureSucceeded) {
        val selectedChord  = chordIterator.next()
        val chordNoteNames = selectedChord.elements.map(_.noteName(key)).toList

        var validCandidates: List[(Pitch, ChordWithBass[NoteName])] = Nil
        for (p <- candidatePitchesPool) {
          if (chordNoteNames.contains(p.noteName)) {
            val interval          = (p - cfPitch).abs
            val intervalSemitones = interval.num.value

            if (intervalSemitones <= 24) {
              val isUnison   = intervalSemitones == 0
              var isValidPos = true

              if (currentMeasurePos == MeasurePosition.FIRST) {
                val deg = Degree.fromNoteNameKey(p.noteName, key)
                if (!Set(0, 4).contains(deg.step.value)) isValidPos = false
              } else if (currentMeasurePos == MeasurePosition.LAST) {
                val deg = Degree.fromNoteNameKey(p.noteName, key)
                if (deg.step.value != 0) isValidPos = false
              } else if (currentMeasurePos == MeasurePosition.PENULTIMATE) {
                if (isUnison) isValidPos = false
              } else {
                if (isUnison) isValidPos = false
              }

              if (isValidPos) {
                val bassPitch     = if (cfPitch.num.value < p.num.value) cfPitch else p
                val chordWithBass = ChordWithBass(HarmonicChord(chordNoteNames.toSet), bassPitch.noteName)

                val dsElements      = chordNoteNames.map(nn => Degree.fromNoteNameKey(nn, key).step).toSet
                val bassDs          = Degree.fromNoteNameKey(bassPitch.noteName, key).step
                val dsChordWithBass = DegreeStepChordWithBass(HarmonicChord(dsElements), bassDs)

                var isInversionValid = true
                try {
                  if (dsChordWithBass.inversionType == Inversion.SECOND) isInversionValid = false
                } catch {
                  case _: IllegalArgumentException => // Not a triad
                }

                if (isInversionValid) {
                  var isValidatorValid = true
                  if (measures.nonEmpty) {
                    val previousPitch   = measures.head.elems.head.value.pitch
                    val previousCfPitch = cantusFirmus(measureIdx - 1)

                    val prevMeasure = createAnnotatedMeasure(previousPitch)
                    val currMeasure = createAnnotatedMeasure(p)

                    if (!Validator.validate(Some(prevMeasure), currMeasure, Some(previousCfPitch), cfPitch)) {
                      isValidatorValid = false
                    }
                  }

                  if (isValidatorValid) {
                    validCandidates ::= (p, chordWithBass)
                  }
                }
              }
            }
          }
        }

        if (validCandidates.nonEmpty) {
          val (chosenPitch, chosenChord) = validCandidates(rand.nextInt(validCandidates.length))
          measures ::= createMeasure(chosenPitch, chosenChord)
          measureSucceeded = true
        }
      }

      if (!measureSucceeded) {
        throw new RuntimeException(
          s"No valid chord and pitch combination found for CF note $cfPitch at measure $measureIdx",
        )
      }
    }

    measures = measures.reverse

    val allAnnotatedMeasures = measures.map(m => createAnnotatedMeasure(m.elems.head.value.pitch))
    if (!Validator.validateAll(allAnnotatedMeasures)) {
      throw new RuntimeException("Failed all measure validation (e.g. total range)")
    }

    Skeleton(measures)
  }

  private def getChordPalette: List[HarmonicDegreeChord] = {
    var palette: List[HarmonicDegreeChord] = Nil

    def makeChord(roots: List[Int]): HarmonicDegreeChord = {
      DegreeChord.of(roots.map(r => getDiatonicDegree(r))*)
    }

    palette ::= DegreeChord.I
    palette ::= DegreeChord.II
    palette ::= makeChord(List(3, 5, 7))
    palette ::= makeChord(List(4, 6, 8)) // 8 is 1 octave up

    if (key.mode == Mode.Major) palette ::= DegreeChord.V
    else palette ::= DegreeChord.V_leading

    palette ::= makeChord(List(6, 8, 10))

    if (key.mode == Mode.Major) palette ::= DegreeChord.VII
    else palette ::= DegreeChord.VII_leading

    palette.reverse
  }

  private def getDiatonicDegree(stepIdx1: Int): Degree = {
    val intervalStep = IntervalStep.idx_1(stepIdx1)
    val p            = key.diatonicScalePitch(intervalStep)
    Degree.fromNoteNameKey(p.noteName, key)
  }

  private def filterChordsContaining(pool: List[HarmonicDegreeChord], degree: Degree): List[HarmonicDegreeChord] = {
    pool.filter(_.elements.contains(degree))
  }

  private def getAllPitchesInRange: List[Pitch] = {
    val (minP, maxP)         = pitchRange
    var pitches: List[Pitch] = Nil
    for (octVal <- (minP.octave.value - 1) to (maxP.octave.value + 1)) {
      for (nnVal <- -15 to 19) {
        val p = Pitch.of(octVal, nnVal)
        if (p.num.value >= minP.num.value && p.num.value <= maxP.num.value) {
          pitches ::= p
        }
      }
    }
    pitches.reverse
  }

  private def currentMeasurePosition(currentMeasureIdx: Int): MeasurePosition = {
    if (currentMeasureIdx == 0) MeasurePosition.FIRST
    else if (currentMeasureIdx == measureLength - 1) MeasurePosition.LAST
    else if (currentMeasureIdx == measureLength - 2) MeasurePosition.PENULTIMATE
    else MeasurePosition.MIDDLE
  }

  private def createMeasure(
      pitch: Pitch,
      chord: ChordWithBass[NoteName],
  ): Melody[PitchWithBass, Note[PitchWithBass]] = {
    val note = Note(PitchWithBass(pitch, chord), Duration.of(4), Part.Root)
    Melody(List(note))
  }

  private def createAnnotatedMeasure(pitch: Pitch): AnnotatedMeasure = {
    val note = Note(
      AnnotatedNote(Some(pitch), NoteAnnotation(isTiedStart = false, toneType = ToneType.HARMONIC_TONE)),
      Duration.of(4),
      Part.Root,
    )
    Melody(List(note))
  }
}
