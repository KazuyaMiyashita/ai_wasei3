package composer.counterpoint

import composer.counterpoint.model._
import composer.counterpoint.search.MeasureSearch
import composer.counterpoint.skeleton.{Skeleton, SkeletonGenerator}
import composer.counterpoint.util.PartUtil
import composer.counterpoint.validation.Validator
import _root_.model.elements._
import _root_.model.containers.{Melody, Note}
import sheet._
import composer.counterpoint.model.Species
import composer.counterpoint.model.MeasureRythmnPattern
import scala.collection.immutable.SeqMap

/** 対位法生成器
  *
  * @param cantusFirmus
  *   定旋律
  * @param cfPart
  *   定旋律のパート
  * @param key
  *   調
  * @param species
  *   対位法の類
  * @param part
  *   対位旋律を生成するパート
  * @param seed
  *   乱数シード
  */
case class CounterpointGenerator(
    cantusFirmus: List[Pitch],
    cfPart: Part,
    key: Key,
    species: Species,
    part: Part,
    seed: Option[Long] = None,
) {

  private val rand = seed.map(s => new scala.util.Random(s)).getOrElse(new scala.util.Random())

  private val measureSearch = MeasureSearch.default
  private val pitchRange    = PartUtil.partRange(part)
  private val measureLength = cantusFirmus.length

  private val MAX_STEPS_PER_ATTEMPT            = 100 // 試行間のバリデーション失敗回数の上限。超えたら最初からやり直し
  private val MAX_VALID_CANDIDATES_PER_MEASURE = 3   // 失敗が連続した際に早く前に戻るために、小節候補を絞る。
  private var currentStepCount                 = 0

  private val skeletonGenerator = new SkeletonGenerator(
    cantusFirmus,
    key,
    pitchRange,
    rand,
    measureLength,
  )

  class AbortAttemptException        extends Exception
  class SucceededAndRestartException extends Exception

  def generateScores: Iterator[SheetMusic] = iterator

  private lazy val iterator: Iterator[SheetMusic] = new Iterator[SheetMusic] {

    var currentBatch: Iterator[SheetMusic] = Iterator.empty

    override def hasNext: Boolean = {
      try {
        if (currentBatch.hasNext) true
        else {
          // Try to generate next batch
          while (!currentBatch.hasNext) {
            currentStepCount = 0
            try {
              // 課題全体の骨格を作成
              val skeleton = skeletonGenerator.generateSkeleton()
              // スケルトンの旋律をロギング (TODO: Logger導入)
              // logger.debug(f"Attempt {attemptCount}: Choose skeleton: ...")
              // logger.debug(f"Attempt {attemptCount}: Start Generate Measures.")

              val initialStartPitch = skeleton.measures.head.elems.head.value.pitch
              currentBatch = generateRecursive(skeleton, Nil, 0, initialStartPitch, None)
              if (currentBatch.hasNext) return true
            } catch {
              case _: AbortAttemptException =>
              // Continue to next attempt
              // logger.debug(f"Attempt {attemptCount}: Aborted. Restarting from scratch.")
              case _: SucceededAndRestartException =>
              // logger.debug(f"Attempt {attemptCount}: Succeeded! Restarting from scratch.")
              // ...
            }
          }
          true
        }
      } catch {
        case _: AbortAttemptException =>
          currentBatch = Iterator.empty
          hasNext
      }
    }

    override def next(): SheetMusic = {
      if (!hasNext) throw new NoSuchElementException("next on empty iterator")
      currentBatch.next()
    }
  }

  private def generateRecursive(
      skeleton: Skeleton,
      completedMeasures: List[AnnotatedMeasure],
      measureIndex: Int,
      currentStartPitch: Pitch,
      previousRythmnPattern: Option[MeasureRythmnPattern],
  ): Iterator[SheetMusic] = {
    currentStepCount += 1
    if (currentStepCount > MAX_STEPS_PER_ATTEMPT) {
      throw new AbortAttemptException()
    }

    val previousMeasure = if (measureIndex > 0) Some(completedMeasures(measureIndex - 1)) else None
    val previousCf      = if (measureIndex > 0) Some(cantusFirmus(measureIndex - 1)) else None
    val currentCf       = cantusFirmus(measureIndex)

    if (measureIndex == measureLength - 1) {
      // 最終小節の場合
      val lastNote: Note[AnnotatedNote] = Note(
        AnnotatedNote(
          Some(skeleton.measures(measureIndex).elems.head.value.pitch),
          NoteAnnotation(isTiedStart = false, toneType = ToneType.HARMONIC_TONE),
        ),
        Duration.of(4),
        Part.Root,
      )
      val lastMeasureCandidate = Melody(List(lastNote))

      if (Validator.validate(previousMeasure, lastMeasureCandidate, previousCf, currentCf)) {
        // logger.debug(f"{indent}Measure {mnForLog}: [SUCCEED] Last measure created and validated.")
        val finalMeasures = completedMeasures :+ lastMeasureCandidate
        if (Validator.validateAll(finalMeasures)) {
          // logger.debug(f"{indent}Attempt succeeded: [SUCCEED] All measures passed validation.]")
          return Iterator(toScore(finalMeasures))
        } else {
          // logger.debug(f"{indent}Attempt failed: [FAILED] All measure validation.]")
          throw new AbortAttemptException()
        }
      } else {
        // logger.debug(f"{indent}Measure {mnForLog}: [FAILED] Last measure created but failed validation.]")
      }
      return Iterator.empty
    } else {
      // 最終小節以外の場合
      val availableRythmnPatterns = getAvailableRythmnPatterns(measureIndex, previousRythmnPattern)

      // Need to extract harmonic note names from skeleton
      // Skeleton measures are Melody[Note[Pitch, ChordWithBass[NoteName]]].
      // We need to extract the chord NoteNames.
      val chordNoteNames = skeleton.measures(measureIndex).elems.head.value.bass.chord.elements

      val startHarmonicPitch            = skeleton.measures(measureIndex).elems.head.value.pitch
      val nextMeasureStartHarmonicPitch = skeleton.measures(measureIndex + 1).elems.head.value.pitch

      val results = measureSearch.search(
        startPitch = currentStartPitch,
        startHarmonicPitch = startHarmonicPitch,
        nextMeasureStartHarmonicPitch = nextMeasureStartHarmonicPitch,
        harmonicNoteNames = chordNoteNames,
        key = key,
        measureRythmnPatterns = availableRythmnPatterns.toSet,
        pitchRange = pitchRange,
      )

      val shuffledResults = rand.shuffle(results)

      val validCandidates = shuffledResults.filter { chosenResult =>
        Validator.validate(previousMeasure, chosenResult.measure, previousCf, currentCf)
      }

      if (validCandidates.isEmpty) return Iterator.empty

      val topCandidates = validCandidates.take(MAX_VALID_CANDIDATES_PER_MEASURE)

      topCandidates.iterator.flatMap { candidate =>
        generateRecursive(
          skeleton,
          completedMeasures :+ candidate.measure,
          measureIndex + 1,
          candidate.nextMeasureStartPitch,
          Some(candidate.rythmnPattern),
        )
      }
    }
  }

  private def getAvailableRythmnPatterns(
      measureIndex: Int,
      previousRythmnPattern: Option[MeasureRythmnPattern],
  ): List[MeasureRythmnPattern] = {
    // We need to implement Rules.getMeasureRythmnPatterns logic.
    // I haven't implemented Rules.scala yet.
    // I can implement the logic here or in a separate object.
    // Given the imports, I should probably implement the logic here for simplicity or create Rules object.
    // Let's create a private helper for now, mimicking Rules.get_measure_rythmn_patterns.

    val currentMeasurePosition = this.currentMeasurePosition(measureIndex)
    var availablePatterns      = getPatternsForSpecies(species, currentMeasurePosition)

    if (previousRythmnPattern.isDefined) {
      val prev = previousRythmnPattern.get
      availablePatterns = availablePatterns.filterNot(_ == prev)

      val isPrevTiedToNext = prev.measureRythmn.isNextTied
      availablePatterns = availablePatterns.filter(p => p.measureRythmn.isPreviousTied == isPrevTiedToNext)
    }
    availablePatterns
  }

  private def currentMeasurePosition(currentMeasureIdx: Int): MeasurePosition = {
    if (currentMeasureIdx == 0) MeasurePosition.FIRST
    else if (currentMeasureIdx == measureLength - 1) MeasurePosition.LAST
    else if (currentMeasureIdx == measureLength - 2) MeasurePosition.PENULTIMATE
    else MeasurePosition.MIDDLE
  }

  private def getPatternsForSpecies(species: Species, position: MeasurePosition): List[MeasureRythmnPattern] = {
    // Port logic from rules.py
    species match {
      case Species.FIRST_SPECIES  => List(MeasureRythmnPattern.R_1)
      case Species.SECOND_SPECIES =>
        position match {
          case MeasurePosition.FIRST => List(MeasureRythmnPattern.R_rr2)
          case MeasurePosition.LAST  => List(MeasureRythmnPattern.R_1)
          case _                     => List(MeasureRythmnPattern.R_22)
        }
      case Species.THIRD_SPECIES =>
        position match {
          case MeasurePosition.FIRST => List(MeasureRythmnPattern.R_r444)
          case MeasurePosition.LAST  => List(MeasureRythmnPattern.R_1)
          case _                     => List(MeasureRythmnPattern.R_4444)
        }
      case Species.FOURTH_SPECIES =>
        position match {
          case MeasurePosition.FIRST  => List(MeasureRythmnPattern.R_rr2, MeasureRythmnPattern.R_rr2t)
          case MeasurePosition.MIDDLE =>
            List(
              MeasureRythmnPattern.R_22,
              MeasureRythmnPattern.R_t22,
              MeasureRythmnPattern.R_22t,
              MeasureRythmnPattern.R_t22t,
            )
          case MeasurePosition.PENULTIMATE => List(MeasureRythmnPattern.R_22, MeasureRythmnPattern.R_t22)
          case MeasurePosition.LAST        => List(MeasureRythmnPattern.R_1)
        }
      case Species.FIFTH_SPECIES =>
        position match {
          case MeasurePosition.FIRST =>
            List(
              MeasureRythmnPattern.R_rr2,
              MeasureRythmnPattern.R_rr2t,
              MeasureRythmnPattern.R_r444,
              MeasureRythmnPattern.R_r42,
            )
          case MeasurePosition.MIDDLE =>
            List(
              MeasureRythmnPattern.R_22,
              MeasureRythmnPattern.R_t22,
              MeasureRythmnPattern.R_22t,
              MeasureRythmnPattern.R_t22t,
              MeasureRythmnPattern.R_4444,
              MeasureRythmnPattern.R_244,
              MeasureRythmnPattern.R_442,
              MeasureRythmnPattern.R_t4444,
              MeasureRythmnPattern.R_t244,
              MeasureRythmnPattern.R_t442,
              MeasureRythmnPattern.R_4444t,
              MeasureRythmnPattern.R_244t,
              MeasureRythmnPattern.R_2488,
              MeasureRythmnPattern.R_4882,
              MeasureRythmnPattern.R_t2488,
              MeasureRythmnPattern.R_t4882,
              MeasureRythmnPattern.R_4882t,
              MeasureRythmnPattern.R_2d4,
              MeasureRythmnPattern.R_2d88,
            )
          case MeasurePosition.PENULTIMATE =>
            List(
              MeasureRythmnPattern.R_22,
              MeasureRythmnPattern.R_t22,
              MeasureRythmnPattern.R_4444,
              MeasureRythmnPattern.R_244,
              MeasureRythmnPattern.R_442,
              MeasureRythmnPattern.R_t4444,
              MeasureRythmnPattern.R_t244,
              MeasureRythmnPattern.R_t442,
              MeasureRythmnPattern.R_2488,
              MeasureRythmnPattern.R_4882,
              MeasureRythmnPattern.R_t2488,
              MeasureRythmnPattern.R_t4882,
              MeasureRythmnPattern.R_2d4,
              MeasureRythmnPattern.R_2d88,
            )
          case MeasurePosition.LAST => List(MeasureRythmnPattern.R_1)
        }
    }
  }

  private def toScore(completedMeasures: List[AnnotatedMeasure]): SheetMusic = {
    // Convert AnnotatedMeasure (Melody[AnnotatedNote, Unit, Note[AnnotatedNote, Unit]]) to sheet.Measure
    // AttributedValue(value: Pitch | Rest, attr: Option[ScoreAttrs])

    def convertMeasure(annotated: AnnotatedMeasure): sheet.Measure = {
      val newElems = annotated.elems.map { note =>
        val value: Pitch | Rest = note.value.value.getOrElse(Rest)
        val attrs               = ScoreAttrs(
          isTiedStart = note.value.annotation.isTiedStart,
          graces = Nil, // Grace notes not supported yet
        )
        Note(AttributedValue(value, Some(attrs)), note.duration, note.part)
      }
      sheet.Measure(Melody(newElems))
    }

    val partMeasures = completedMeasures.map(convertMeasure)

    val cfMeasures = cantusFirmus.map { pitch =>
      val note = Note(
        AttributedValue(pitch, Some(ScoreAttrs(false))),
        Duration.of(4),
        cfPart,
      )
      sheet.Measure(Melody(List(note)))
    }

    // 2/2 time signature as per Python code
    val timeSig = sheet.TimeSignature(2, Duration.of(2))

    val parts = Seq(
      cfPart -> cfMeasures,
      part   -> partMeasures,
    )

    val partOrdering = Part.ordering("Soprano", "Alto", "Tenor", "Bass")

    SheetMusic(
      key = key,
      timeSignature = timeSig,
      timeSignatureEvents = Nil,
      keySignatureEvents = Nil,
      body = PartMapScore(SeqMap.from(parts.sortBy(_._1)(using partOrdering))),
      title = None,
    )
  }
}
