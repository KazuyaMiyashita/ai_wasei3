package composer.counterpoint.validation

import composer.counterpoint.model.{AnnotatedMeasure, AnnotatedNote, ToneType}
import model.elements.{Duration, Interval, Offset, Pitch}
import model.elements.Interval.IntervalStep
import model.containers.{Note, Melody}

object Validator {

  def validate(
      previousMeasure: Option[AnnotatedMeasure],
      currentMeasure: AnnotatedMeasure,
      previousCf: Option[Pitch],
      currentCf: Pitch,
  ): Boolean = {
    validateInterval(previousMeasure, currentMeasure, previousCf, currentCf) &&
    validateMelody(previousMeasure, currentMeasure)
  }

  def validateAll(completedMeasures: List[AnnotatedMeasure]): Boolean = {
    validatePartTotalRange(completedMeasures)
  }

  /** 連続・並達に関するバリデーション。禁則があれば False を返す */
  private def validateInterval(
      previousMeasure: Option[AnnotatedMeasure],
      currentMeasure: AnnotatedMeasure,
      previousCf: Option[Pitch],
      currentCf: Pitch,
  ): Boolean = {
    // 冒頭小節には直前の小節が存在しないため、連続は起こり得ない。
    if (previousMeasure.isEmpty) return true
    val prevM  = previousMeasure.get
    val prevCf = previousCf.get

    // 2つの声部が同時に動いている場合の確認 (CFが全音符なので小節を跨いだタイミングのみ)
    val prevMeasureLastPitch  = prevM.elems.last.value.value
    val currMeasureFirstPitch = currentMeasure.elems.head.value.value

    if (prevMeasureLastPitch.isDefined && currMeasureFirstPitch.isDefined) {
      // 連続
      val seq1 = (prevCf, currentCf)
      val seq2 = (prevMeasureLastPitch.get, currMeasureFirstPitch.get)

      if (checkIsParallelViolation(seq1, seq2)) return false
      // 並達
      if (checkIsHiddenIntervalViolation(seq1, seq2)) return false
    }

    // 間接の連続の確認
    // 便宜上前の小節と現在の小節を繋げた1小節を考え、Offset.of(4)以降のものに対して確認をする
    //
    // 間接の連続は、全音符1個に相当する長さが隔てられていれば許される。またもっと近くにあっても
    // 同時に打音されいるのではなく、かつ、反行している場合かいずれかの音が非和声音である場合は許される。
    //
    // すなわち、以下を満たした場合、禁則となる。
    // ある声部の Offset の差が Duration.of(4) 以下の異なる2音のうち、
    // ある他の声部の、それらの音に同時になっている2音を選び、
    // それら2声部の音が直接の連続の規則として禁則であり、
    // かつ、not (後続の5度・8度をなす音が同時に打音されていない and (反行している または いずれかの音が非和声音))

    // 簡単のため、小節と現在の小節を繋げた1小節を考え、Offset.of(4)以降のものに対して確認をする
    val cfMelody = Melody(
      List(
        Note(prevCf, Duration.of(4), model.elements.Part.Root, ()),
        Note(currentCf, Duration.of(4), model.elements.Part.Root, ()),
      ),
      (),
    )

    // Combine measures
    val realizeNotes  = prevM.elems ++ currentMeasure.elems
    val realizeMelody = Melody(realizeNotes, ())

    val realizeOffsetNotes = calculateOffsetNotes(realizeMelody)

    val hasViolation = realizeOffsetNotes.exists { case (realizeCurrentOffset, realizeCurrentNote) =>
      if (realizeCurrentOffset >= Offset.of(4)) {
        realizeOffsetNotes.exists { case (realizePreviousOffset, realizePreviousNote) =>
          val diff = realizeCurrentOffset - realizePreviousOffset
          if (diff > Duration.of(0) && diff <= Duration.of(4)) {
            val realizeCurrentPitch  = realizeCurrentNote.value.value
            val realizePreviousPitch = realizePreviousNote.value.value

            if (realizeCurrentPitch.isDefined && realizePreviousPitch.isDefined) {

              val (cfCurrentStartOffset, cfCurrentNote) = getNoteAt(cfMelody, realizeCurrentOffset)

              val (_, cfPreviousNote) = getNoteAt(cfMelody, realizePreviousOffset)

              // (現在は定旋律に対して確認しているので必ず音高が取得できる)

              val cfCurrentPitch = cfCurrentNote.value

              val cfPreviousPitch = cfPreviousNote.value

              // 直接の連続の規則として連続である

              val isParallelViolation = checkIsParallelViolation(
                (cfPreviousPitch, cfCurrentPitch),
                (realizePreviousPitch.get, realizeCurrentPitch.get),
              )

              // 後続の5度・8度をなす音が同時に打音されている

              val hasFollowingNotesSameOffsetVal = cfCurrentStartOffset == realizeCurrentOffset

              // 2声が反行している

              val isContraryMotion = checkIsContraryMotion(
                (cfPreviousPitch, cfCurrentPitch),
                (realizePreviousPitch.get, realizeCurrentPitch.get),
              )

              // いずれかの音が非和声音

              // (現在は定旋律に対して確認しているので実施声部のみを確認する)

              val nonHarmonicToneExists =
                realizeCurrentNote.value.annotation.toneType != ToneType.HARMONIC_TONE ||
                  realizePreviousNote.value.annotation.toneType != ToneType.HARMONIC_TONE

              isParallelViolation && !(!hasFollowingNotesSameOffsetVal && (isContraryMotion || nonHarmonicToneExists))

            } else false
          } else false
        }
      } else false
    }

    !hasViolation
  }

  /** 2つの旋律の進行が並行しているかどうかを返す */
  private def checkIsParallelMotion(seq1: (Pitch, Pitch), seq2: (Pitch, Pitch)): Boolean = {
    val (s1Start, s1End) = seq1
    val (s2Start, s2End) = seq2
    if (s1Start == s1End || s2Start == s2End) return false

    val dir1Up = s1End.num.value > s1Start.num.value
    val dir2Up = s2End.num.value > s2Start.num.value
    dir1Up == dir2Up
  }

  /** 2つの旋律の進行が反行しているかどうかを返す */
  private def checkIsContraryMotion(seq1: (Pitch, Pitch), seq2: (Pitch, Pitch)): Boolean = {
    val (s1Start, s1End) = seq1
    val (s2Start, s2End) = seq2
    if (s1Start == s1End || s2Start == s2End) return false

    val dir1Up = s1End.num.value > s1Start.num.value
    val dir2Up = s2End.num.value > s2Start.num.value
    dir1Up != dir2Up
  }

  /** 連続5度・8度の禁則が含まれているかどうか。
    * 並行・反行のいずれも禁則とする。(斜行と同時保留はOK)
    */
  private def checkIsParallelViolation(seq1: (Pitch, Pitch), seq2: (Pitch, Pitch)): Boolean = {
    if (!checkIsParallelMotion(seq1, seq2) && !checkIsContraryMotion(seq1, seq2)) return false

    val firstInterval  = Interval.of(seq1._1, seq2._1).normalize
    val secondInterval = Interval.of(seq1._2, seq2._2).normalize

    if (firstInterval == secondInterval && firstInterval == Interval.P1) return true
    if (firstInterval == secondInterval && firstInterval == Interval.P5) return true
    if (firstInterval == Interval.d5 && secondInterval == Interval.P5) return true
    if (firstInterval == Interval.P5 && secondInterval == Interval.d5) return true

    false
  }

  /** 並達5度・8度の禁則が含まれているかどうか */
  private def checkIsHiddenIntervalViolation(seq1: (Pitch, Pitch), seq2: (Pitch, Pitch)): Boolean = {
    if (!checkIsParallelMotion(seq1, seq2)) return false
    val secondInterval = Interval.of(seq1._2, seq2._2).normalize
    Set(Interval.P1, Interval.P5).contains(secondInterval)
  }

  /** 旋律に関するバリデーション
    *
    * DONE:
    *   - 分散和音をしない
    *   - 3音符で形成される7度・9度は順次進行を含める
    *
    * 優先して実装したい:
    *   - 完全8度の跳躍はできるだけその前後に反対方向の進行を伴う
    *   - 旋律の対称系や繰り返し(特に同一音への3度続く回帰)
    *
    * 後回し?:
    *   - できるだけ非順次進行を避ける(どの程度?)
    *   - 小節線をはさんだ非順次進行を避ける(どの程度?)
    *   - 3,4個の音符で形成される増4度は同方向の順次進行で先行または後続させる
    */
  private def validateMelody(previousMeasure: Option[AnnotatedMeasure], currentMeasure: AnnotatedMeasure): Boolean = {
    validateMelodyArpeggiio(previousMeasure, currentMeasure) &&
    validateMelodyArpeggiioExtra(previousMeasure, currentMeasure) &&
    validateMelodyInterval79(previousMeasure, currentMeasure)
  }

  /** 前の小節の末尾から num 音取得し、 note_buffer と繋げたリストを返す */
  private def extendedNoteBuffer(
      previousMeasure: Option[AnnotatedMeasure],
      currentMeasure: AnnotatedMeasure,
      num: Int,
  ): List[Note[AnnotatedNote, ?]] = {
    val prevNotes = previousMeasure.map(_.elems.takeRight(num)).getOrElse(Nil)
    prevNotes ++ currentMeasure.elems
  }

  /** 分散和音のバリデーション。旋律が分散和音の形になっているときFalseを返す
    *
    * TODO: 反転の分散和音はOKとしている
    */
  private def validateMelodyArpeggiio(
      previousMeasure: Option[AnnotatedMeasure],
      currentMeasure: AnnotatedMeasure,
  ): Boolean = {
    val notes   = extendedNoteBuffer(previousMeasure, currentMeasure, 2)
    val pitches = notes.flatMap(_.value.value)

    val arpeggiioStepsList = List(
      List(IntervalStep.idx_1(3), IntervalStep.idx_1(5)),
      List(IntervalStep.idx_1(-3), IntervalStep.idx_1(-5)),
      List(IntervalStep.idx_1(3), IntervalStep.idx_1(6)),
      List(IntervalStep.idx_1(-3), IntervalStep.idx_1(-6)),
      List(IntervalStep.idx_1(4), IntervalStep.idx_1(6)),
      List(IntervalStep.idx_1(-4), IntervalStep.idx_1(-6)),
    )

    pitches.sliding(3).forall { ps =>
      if (ps.length < 3) true
      else {
        val base  = ps(0)
        val p1    = ps(1)
        val p2    = ps(2)
        val steps = List((p1 - base).step, (p2 - base).step)
        !arpeggiioStepsList.contains(steps)
      }
    }
  }

  /** 特殊な形態のいくつかの分散和音を禁止する。
    *
    *   - (A-1): [C4 G4 C5], [G4 C5 G5], [G4, C4, C5] といった第3音を伴わない分散和音(反転なし)
    *
    * ---
    * 以下も考えられるが、現在は認めている
    *
    *   - (A-2): [G4, C4, C5] といった第3音を伴わない分散和音(反転あり)
    *     - (しかしこれは [G4 A4 *G4 *C4 | *C5 B4 A4 G4] といった認めたくなるケースがある
    *   - (B)] [C4 C5 C4] といったオクターブの移動
    *     - (しかしこれは困難な場合には例外として許される)
    *     - (できるだけ非順次進行を避けるといった規則で対応されるかもしれない)
    *   - (C): [C4 G4 C4 C4] や [C5 G4 C5 G4] といった4度・5度の反復
    *     - (できるだけ非順次進行を避けるといった規則で対応されるかもしれない)
    */
  private def validateMelodyArpeggiioExtra(
      previousMeasure: Option[AnnotatedMeasure],
      currentMeasure: AnnotatedMeasure,
  ): Boolean = {
    val notes   = extendedNoteBuffer(previousMeasure, currentMeasure, 2)
    val pitches = notes.flatMap(_.value.value)

    val arpeggiioStepsList = List(
      List(IntervalStep.idx_1(5), IntervalStep.idx_1(8)),
      List(IntervalStep.idx_1(-5), IntervalStep.idx_1(-8)),
      List(IntervalStep.idx_1(4), IntervalStep.idx_1(8)),
      List(IntervalStep.idx_1(-4), IntervalStep.idx_1(-8)),
    )

    pitches.sliding(3).forall { ps =>
      if (ps.length < 3) true
      else {
        val base  = ps(0)
        val p1    = ps(1)
        val p2    = ps(2)
        val steps = List((p1 - base).step, (p2 - base).step)
        !arpeggiioStepsList.contains(steps)
      }
    }
  }

  /** 3音符で形成される7度・9度は順次進行を含める必要がある。そうなっていなければFalseを返す
    * (9度より大きい音程になることは別の規則で禁止されそうだが、この規則で扱う)
    */
  private def validateMelodyInterval79(
      previousMeasure: Option[AnnotatedMeasure],
      currentMeasure: AnnotatedMeasure,
  ): Boolean = {
    val notes   = extendedNoteBuffer(previousMeasure, currentMeasure, 2)
    val pitches = notes.flatMap(_.value.value)

    pitches.sliding(3).forall { ps =>
      if (ps.length < 3) true
      else {
        val p1     = ps(0)
        val p2     = ps(1)
        val p3     = ps(2)
        val step13 = (p1 - p3).abs.step

        if (step13 == IntervalStep.idx_1(7) || step13.value > IntervalStep.idx_1(9).value) {
          val step12 = (p1 - p2).abs.step
          val step23 = (p2 - p3).abs.step
          step12 == IntervalStep.idx_1(2) || step23 == IntervalStep.idx_1(2)
        } else {
          true
        }
      }
    }
  }

  /** 各声部の音域は同一課題中において11度を越えてはならない。越えた場合 False
    *
    * 順次進行が長く続く場合には例外として12度が認められるが、ここでは禁止としている。
    */
  private def validatePartTotalRange(completedMeasures: List[AnnotatedMeasure]): Boolean = {
    val allPitches = completedMeasures.flatMap(_.elems).flatMap(_.value.value)
    if (allPitches.isEmpty) return true

    val pMin = allPitches.minBy(_.num.value)
    val pMax = allPitches.maxBy(_.num.value)

    (pMax - pMin).step.value <= IntervalStep.idx_1(11).value
  }

  private def calculateOffsetNotes[A, B, S <: model.containers.Score[A, B]](melody: Melody[A, B, S]): Map[Offset, S] = {
    var curr = Offset.of(0)
    melody.elems.map { note =>
      val entry = curr -> note
      curr = curr + note.duration
      entry
    }.toMap
  }

  private def getNoteAt[A, B, S <: model.containers.Score[A, B]](
      melody: Melody[A, B, S],
      offset: Offset,
  ): (Offset, S) = {
    var curr      = Offset.of(0)
    val foundNote = melody.elems.find { note =>
      val end = curr + note.duration
      if (curr <= offset && offset < end) true
      else {
        curr = end
        false
      }
    }

    foundNote match {
      case Some(note) => (curr, note)
      case None       =>
        throw new IllegalArgumentException(s"offset $offset not found in melody of duration ${melody.duration}")
    }
  }
}
