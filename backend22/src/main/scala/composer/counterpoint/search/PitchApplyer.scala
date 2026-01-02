package composer.counterpoint.search

import composer.counterpoint.model.{AnnotatedMeasure, AnnotatedNote, ToneType, NoteAnnotation}
import model.containers.{Melody, Note}
import model.elements.{Degree, Key, Pitch, Part}
import model.elements.Degree.{DegreeStep, DegreeAlter}
import model.elements.Interval
import model.elements.Interval.IntervalStep

object PitchApplyer {

  case class PitchMeasureStepSequence(
      measure: AnnotatedMeasure,
      nextMeasureStartPitch: Pitch,
  )

  /** 調・和音・開始音と、与えられた音列(IntervalStepとToneTypeを持つ)に応じて、短音階の変位音を考慮して音高列の候補を得る。
    *
    * 音列によって、結果は0~2個となる。
    * 長調や、短調でvi,viiを含まない場合は結果は1つとなる。
    * vi,viiを含む短調の場合は結果が2個になる場合や、不適当と判断されて結果が0個になることがある。
    */
  def applyPitchCandidates(
      key: Key,
      chordDegrees: Set[Degree],
      startPitch: Pitch,
      measureStepSequence: MeasureStepSequence,
  ): List[PitchMeasureStepSequence] = {

    val annotatedIntervalSteps = measureStepSequence.measureNotes.map(n => (n.value, n.meta))

    val allIntervalSteps   = annotatedIntervalSteps.map(_._1) :+ measureStepSequence.nextMeasureStep
    val allDiatonicPitches = applyPitchDiatonic(key, startPitch, allIntervalSteps)
    val diatonicPitches    = allDiatonicPitches.init
    val nextDiatonicPitch  = allDiatonicPitches.last

    val degreeSteps           = diatonicPitches.map(p => Degree.fromNoteNameKey(p.noteName, key).step)
    val nextMeasureDegreeStep = Degree.fromNoteNameKey(nextDiatonicPitch.noteName, key).step

    val SIXTH   = DegreeStep.idx1(6)
    val SEVENTH = DegreeStep.idx1(7)

    if (
      key.mode == Key.Mode.Major || !((degreeSteps.toSet + nextMeasureDegreeStep)
        .intersect(Set(SIXTH, SEVENTH))
        .nonEmpty)
    ) {
      var newMeasureNotes: List[Note[AnnotatedNote, Unit]] = Nil
      var pitchIdx                                         = 0
      for (note <- measureStepSequence.measureNotes) {
        newMeasureNotes ::= Note(
          AnnotatedNote(Some(diatonicPitches(pitchIdx)), NoteAnnotation(isTiedStart = false, toneType = note.meta)),
          note.duration,
          Part.Root,
          (),
        )
        pitchIdx += 1
      }
      return List(
        PitchMeasureStepSequence(
          Melody(newMeasureNotes.reverse, ()),
          nextDiatonicPitch,
        ),
      )
    }

    val degreeNoteList = measureStepSequence.measureNotes.zip(degreeSteps).map { case (note, degStep) =>
      Note(degStep, note.duration, Part.Root, note.meta)
    }

    val possibleDegreeSequences = degreeCandidates(
      chordDegrees,
      degreeNoteList,
      nextMeasureDegreeStep,
    )

    possibleDegreeSequences.map { case (measureDegrees, nextDegree) =>
      var pitches: List[Pitch] = Nil
      var pitchIdx             = 0
      for (dNote <- measureDegrees) {
        val deg           = dNote.value
        val basePitch     = diatonicPitches(pitchIdx)
        val adjustedPitch = basePitch + (Interval.A1 * deg.alter.value)
        pitches ::= adjustedPitch
        pitchIdx += 1
      }
      pitches = pitches.reverse

      val nextPitchCandidate = nextDiatonicPitch + (Interval.A1 * nextDegree.alter.value)

      var finalNotes: List[Note[AnnotatedNote, Unit]] = Nil
      for (i <- measureStepSequence.measureNotes.indices) {
        val origNote = measureStepSequence.measureNotes(i)
        val pitch    = pitches(i)
        finalNotes ::= Note(
          AnnotatedNote(Some(pitch), NoteAnnotation(isTiedStart = false, toneType = origNote.meta)),
          origNote.duration,
          Part.Root,
          (),
        )
      }

      PitchMeasureStepSequence(
        Melody(finalNotes.reverse, ()),
        nextPitchCandidate,
      )
    }
  }
  // ... (rest of the file is unchanged, assume correct)

  /** 調・和音に応じて、一小節および次の音から小節の音列のDegreeを判断する。
    *
    * 音列によって、結果は0~2個となる。
    * 長調や、短調でvi,viiを含まない場合は結果は1つとなる。
    * vi,viiを含む短調の場合は結果が2個になる場合や、不適当と判断されて結果が0個になることがある。
    */
  private def degreeCandidates(
      chordDegrees: Set[Degree],
      measureDegreeNotes: List[Note[DegreeStep, ToneType]],
      nextMeasureDegreeStep: DegreeStep,
  ): List[(List[Note[Degree, ToneType]], Degree)] = {

    val elems = measureDegreeNotes.map(n => (n.value, n.meta))

    val SIXTH   = DegreeStep.idx1(6)
    val SEVENTH = DegreeStep.idx1(7)

    val baseCandidates = scala.collection.mutable.Map[Int, List[Degree]]()

    for (i <- elems.indices) {
      val (degreeStep, toneType) = elems(i)
      if (!Set(SIXTH, SEVENTH).contains(degreeStep)) {
        baseCandidates(i) = List(Degree(degreeStep, DegreeAlter(0)))
      } else {
        if (Set(ToneType.HARMONIC_TONE, ToneType.SUSPENDED_RESOLVING_HARMONIC_TONE).contains(toneType)) {
          val degrees = chordDegrees.filter(_.step == degreeStep).toList
          baseCandidates(i) = degrees
        } else if (toneType == ToneType.PASSING_TONE) {
          if (Set(Degree.idx1(6, 0), Degree.idx1(7, 0)).intersect(chordDegrees).nonEmpty) {
            baseCandidates(i) = List(Degree(degreeStep, DegreeAlter(0)))
          } else if (Set(Degree.idx1(6, 1), Degree.idx1(7, 1)).intersect(chordDegrees).nonEmpty) {
            baseCandidates(i) = List(Degree(degreeStep, DegreeAlter(1)))
          } else {
            baseCandidates(i) = List(Degree(degreeStep, DegreeAlter(0)), Degree(degreeStep, DegreeAlter(1)))
          }
        } else if (toneType == ToneType.SUSPENDED_TONE) {
          val resolveDegreeStep = elems.drop(i).find(_._2 == ToneType.HARMONIC_TONE).get._1
          if (degreeStep == SEVENTH && resolveDegreeStep == DegreeStep.idx1(1)) {
            baseCandidates(i) = List(Degree(degreeStep, DegreeAlter(1)))
          } else if (degreeStep == SEVENTH && resolveDegreeStep == SIXTH) {
            baseCandidates(i) = List(Degree(degreeStep, DegreeAlter(0)))
          } else if (degreeStep == SIXTH && resolveDegreeStep == DegreeStep.idx1(5)) {
            baseCandidates(i) = List(Degree(degreeStep, DegreeAlter(0)))
          } else {
            baseCandidates(i) = Nil
          }
        } else if (toneType == ToneType.NEIGHBOR_TONE) {
          val harmonicDs = elems(i - 1)._1
          val brDs       = degreeStep
          if (harmonicDs == DegreeStep.idx1(1) && brDs == SEVENTH) {
            baseCandidates(i) = List(Degree(degreeStep, DegreeAlter(1))) // A G# A
          } else if (harmonicDs == SIXTH && brDs == SEVENTH) {
            if (chordDegrees.contains(Degree.idx1(6, 0))) baseCandidates(i) = List(Degree(degreeStep, DegreeAlter(0)))
            else if (chordDegrees.contains(Degree.idx1(6, 1)))
              baseCandidates(i) = List(Degree(degreeStep, DegreeAlter(1)))
            else baseCandidates(i) = Nil
          } else if (harmonicDs == SEVENTH && brDs == SIXTH) {
            var cands: List[Degree] = Nil
            if (chordDegrees.contains(Degree.idx1(7, 0))) cands ::= Degree(degreeStep, DegreeAlter(0))
            if (chordDegrees.contains(Degree.idx1(7, 1))) cands ::= Degree(degreeStep, DegreeAlter(1))
            baseCandidates(i) = cands
          } else if (harmonicDs == DegreeStep.idx1(5) && brDs == SIXTH) {
            baseCandidates(i) = List(Degree(degreeStep, DegreeAlter(0)))
          } else {
            baseCandidates(i) = Nil
          }
        }
      }
    }

    val candidateLists = (0 until elems.length).map(baseCandidates).toList

    def sequenceCandidates(lists: List[List[Degree]]): List[List[Degree]] = lists match {
      case Nil          => List(Nil)
      case head :: tail =>
        for {
          h <- head
          t <- sequenceCandidates(tail)
        } yield h :: t
    }

    val allMeasureCandidates = sequenceCandidates(candidateLists)

    var validSequences: List[(List[Note[Degree, ToneType]], Degree)] = Nil

    for (degreesForMeasure <- allMeasureCandidates) {
      val possibleNextDegrees = getNextDegreeCandidates(nextMeasureDegreeStep, degreesForMeasure)

      for (nextDegree <- possibleNextDegrees) {
        val fullSeq = degreesForMeasure :+ nextDegree
        if (isValidAlterationCombination(fullSeq)) {
          val newMeasureNotes = measureDegreeNotes.zip(degreesForMeasure).map { case (orig, deg) =>
            Note(deg, orig.duration, Part.Root, orig.meta)
          }
          validSequences ::= (newMeasureNotes, nextDegree)
        }
      }
    }
    validSequences.reverse
  }

  /** 次の小節の開始音の候補を作成する。
    * 基本的に0(Natural)だが、Degree 6, 7の場合は旋律短音階の考慮が必要。
    */
  private def getNextDegreeCandidates(nextStep: DegreeStep, currentDegrees: List[Degree]): List[Degree] = {
    var candidates: List[Degree] = List(Degree(nextStep, DegreeAlter(0)))

    val SEVENTH = DegreeStep.idx1(7)
    val SIXTH   = DegreeStep.idx1(6)

    if (nextStep == SEVENTH) {
      candidates ::= Degree(nextStep, DegreeAlter(1))
    } else if (nextStep == SIXTH) {
      if (currentDegrees.nonEmpty && currentDegrees.last.step == DegreeStep.idx1(5)) {
        candidates ::= Degree(nextStep, DegreeAlter(1))
      }
    }
    candidates
  }

  /** 旋律全体の第6音と第7音の変位の整合性をチェックする。
    * 旋律短音階の上行形(^6, ^7)と下行形(6, 7)が不適切に混ざっていないかを確認する。
    */
  private def isValidAlterationCombination(fullSequence: List[Degree]): Boolean = {
    val SIXTH   = DegreeStep.idx1(6)
    val SEVENTH = DegreeStep.idx1(7)

    val alters6 = fullSequence.filter(_.step == SIXTH).map(_.alter.value).toSet
    val alters7 = fullSequence.filter(_.step == SEVENTH).map(_.alter.value).toSet

    if (alters6.contains(0) && alters7.contains(1)) return false
    if (alters6.contains(1) && alters7.contains(0)) return false
    true
  }

  private[search] def applyPitchDiatonic(
      key: Key,
      startPitch: Pitch,
      intervalSteps: List[IntervalStep],
  ): List[Pitch] = {
    val mcStartStep     = startPitch.asInterval.step
    val mcIntervalSteps = intervalSteps.map(step => mcStartStep + step)
    mcIntervalSteps.map(step => key.diatonicScalePitch(step))
  }

  extension (p: Pitch) {
    def asInterval: Interval = Interval(p.octave.value, p.noteName.value)
  }
}
