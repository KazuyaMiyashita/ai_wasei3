package composer.counterpoint.search

import composer.counterpoint.model.ToneType
import model.elements.Duration
import model.elements.Interval.IntervalStep
import composer.counterpoint.search.MeasureStepSequence.inversionNormalized

object MeasureStepSequenceGenerator {

  /** 小節の最初の音(掛留音の場合は解決音)をIntervalStep.idx_1(1)とした時、
    * 和声音はその音に対して何度上のものを利用するかということを表す
    */
  enum HarmonicSteps {
    case S_1_3_5
    case S_1_3_6
    case S_1_4_6

    /** 協和音として利用できる音程。
      * ここに含まれる音のほかにも、それらのオクターブ違いのものも利用できる。
      * そのため、利用できるかどうかは確認したい音をユニゾン~7度までの範囲に正規化(inversion_normalized)した上で調べる必要がある。
      */
    def intervalSteps: Set[IntervalStep] = {
      val stepValues = this match {
        case S_1_3_5 => List(1, 3, 5)
        case S_1_3_6 => List(1, 3, 6)
        case S_1_4_6 => List(1, 4, 6)
      }
      stepValues.map(i => IntervalStep.idx_1(i)).toSet
    }
  }

  case class ExplorationState(
      melody: List[AnnotatedIntervalStep],
      isSuspensionUnresolved: Boolean,
      usedHarmonicSteps: Set[IntervalStep],
  )

  /** 以下の規則を満たす MeasureStepSequence を生成する。
    *
    * 和声音に関する規則:
    *   - 和声音として、 IntervalStep(0) が必ず利用される。
    *   - その他の和声音は IntervalStep の組み合わせ [0, 2, 4], [0, 2, 5], [0, 3, 5] のいずれかから部分的に選ばれる。
    *   - 和声音は異なる高さの和声音に跳躍して進行することができる。
    *
    * 非和声音に関する規則:
    *   - 掛留音は小節内のいずれかの位置で IntervalStep(0) に解決する。
    *   - 経過音は和声音とその2つ以上後の和声音の間を順次進行で埋める形で利用される。後者の音は次の小節の音でも良い。
    *   - 刺繍音は和声音とその2つ後の同じ高さの音を順次進行で埋める形で利用される。後者の音は次の小節の音でも良い。
    *
    * 旋律に関する規則:
    *   - 分散和音(例: [0, 2, 4])の音形は利用しない。ただし反転分散和音(例: [0, 4, 2])は長さ3までのものは利用する。
    *   - 同一音への3度の回帰(例: [0, 1, 0, -1 | 0])は利用しない。
    *   - 跳躍を伴う隣接2音の反復(例: [0, 2, 0, 2])は利用しない
    *   - 小節をまたぐ時、同一方向への跳躍(例 [0, 1, 2, 3 | 6]) は利用しない。
    */
  def generate(): List[MeasureStepSequence] = {
    // logger.info("MeasureStepSequenceの生成を開始します")

    // 探索中に生成された、長さが1〜4音の全ての旋律パターンを格納する
    var intermediateMelodies: List[List[AnnotatedIntervalStep]] = Nil

    // 1〜4音の各長さのメロディを生成する
    for (maxLen <- 1 to 4) {
      // 探索の起点となる初期状態のリスト
      val initialStates = List(
        ExplorationState(
          melody = List(createStep(IntervalStep(0), ToneType.HARMONIC_TONE)),
          isSuspensionUnresolved = false,
          usedHarmonicSteps = Set(IntervalStep.idx_1(1)), // 0 (idx1=1) is normalized 0.
        ),
        ExplorationState(
          melody = List(createStep(IntervalStep(1), ToneType.SUSPENDED_TONE)),
          isSuspensionUnresolved = true,
          usedHarmonicSteps = Set.empty,
        ),
        ExplorationState(
          melody = List(createStep(IntervalStep(-1), ToneType.SUSPENDED_TONE)),
          isSuspensionUnresolved = true,
          usedHarmonicSteps = Set.empty,
        ),
      )

      for (state <- initialStates) {
        intermediateMelodies ++= exploreMelodiesRecursive(state, maxLen).map(_.melody)
      }
    }

    val allPatterns = intermediateMelodies.flatMap { melody =>
      val nextOptions = generateNextMeasureStep(melody)
      nextOptions.map { nextMeasureStep =>
        MeasureStepSequence(melody, nextMeasureStep)
      }
    }

    val uniqueResults = allPatterns
      .filter(isValidPattern)
      .distinct
      .sortBy(_.name)

    // logger.info(f"MeasureStepSequenceの生成を完了しました。{len(unique_results)}個のユニークなパターンを生成しました。")
    uniqueResults
  }

  // 目的: 現在の状態から、操作によって伸長可能な次の状態を再帰的に探索し、結果をリストで返す
  private def exploreMelodiesRecursive(
      state: ExplorationState,
      maxMelodyLength: Int,
  ): List[ExplorationState] = {
    require(state.melody.length <= maxMelodyLength)

    // 終了条件: 旋律が指定の長さに達したら、その状態を結果として返す
    if (state.melody.length == maxMelodyLength) {
      // ただし、掛留音が未解決のまま終了するのは不適切
      if (state.isSuspensionUnresolved) return Nil
      return List(state)
    }

    if (state.isSuspensionUnresolved) {
      // 掛留音が未解決の場合、解決を試みる
      val options = opResolveSuspension(state, maxMelodyLength)
      options.flatMap(nextState => exploreMelodiesRecursive(nextState, maxMelodyLength))
    } else {
      // 通常の操作 (掛留音が解決済み、または元々ない場合)
      val res1 = opAddHarmonicTone(state, maxMelodyLength).flatMap(exploreMelodiesRecursive(_, maxMelodyLength))
      val res2 = opAddPassingTones(state, maxMelodyLength).flatMap(exploreMelodiesRecursive(_, maxMelodyLength))
      val res3 = opAddNeighborTone(state, maxMelodyLength).flatMap(exploreMelodiesRecursive(_, maxMelodyLength))
      res1 ++ res2 ++ res3
    }
  }

  // --- 操作関数 ---

  // 掛留音を解決するパターンを生成する。
  private def opResolveSuspension(state: ExplorationState, maxMelodyLength: Int): List[ExplorationState] = {
    if (state.melody.isEmpty) throw new IllegalArgumentException("Melody cannot be empty")

    val allHarmonicPatterns = HarmonicSteps.values.map(_.intervalSteps).toList
    val startNote           = state.melody.head
    val resolveStep         = IntervalStep(0)

    var results: List[ExplorationState] = Nil

    // 1. 基本的な解決 (1r, 0H)
    if (state.melody.length <= maxMelodyLength - 1) {
      val newUsedStepsSimple = state.usedHarmonicSteps + resolveStep // resolveStep is 0. 0 normalized is 0.
      if (allHarmonicPatterns.exists(p => newUsedStepsSimple.subsetOf(p))) {
        val newMelody = state.melody :+ createStep(resolveStep, ToneType.HARMONIC_TONE)
        results ::= ExplorationState(newMelody, false, newUsedStepsSimple)
      }
    }

    // 2. 解決の間に和声音に進行するパターン (1r, 4srh, 0H)
    if (state.melody.length <= maxMelodyLength - 2) {
      for (degree <- List(2, 3, 4, 5)) {
        val intermediateStep = IntervalStep(degree)
        // TODO: 利用した和声音に記録するが、srhとしてアノテーションするのが統一感が無い感じがする。
        val newUsedStepsJump = state.usedHarmonicSteps + resolveStep + intermediateStep.inversionNormalized
        if (allHarmonicPatterns.exists(p => newUsedStepsJump.subsetOf(p))) {
          val newMelody = state.melody ++ List(
            createStep(intermediateStep, ToneType.SUSPENDED_RESOLVING_HARMONIC_TONE),
            createStep(resolveStep, ToneType.HARMONIC_TONE),
          )
          results ::= ExplorationState(newMelody, false, newUsedStepsJump)
        }
      }
    }

    // 3. 特殊なパターン (1r, 0srh, -1br, 0H)
    // TODO: -1r, -2?, -1?, 0H を含めるか？ ([A Minor, I, G#(d=1), F#(d=1/2), G#(d=1/2), A(d=2)])
    if (state.melody.length <= maxMelodyLength - 3 && startNote.value.value == IntervalStep(1)) {
      val newUsedStepsSpecial = state.usedHarmonicSteps + resolveStep
      if (allHarmonicPatterns.exists(p => newUsedStepsSpecial.subsetOf(p))) {
        val newMelody = state.melody ++ List(
          createStep(IntervalStep(0), ToneType.SUSPENDED_RESOLVING_HARMONIC_TONE),
          // 音楽的にも、Degreeの判断の都合上も刺繍音とするのがちょうど良い。
          createStep(IntervalStep(-1), ToneType.NEIGHBOR_TONE),
          createStep(resolveStep, ToneType.HARMONIC_TONE),
        )
        results ::= ExplorationState(newMelody, false, newUsedStepsSpecial)
      }
    }

    results.reverse
  }

  // 現在の旋律に、有効な和声音を1つ追加した新しい旋律のリストを返す
  private def opAddHarmonicTone(state: ExplorationState, maxMelodyLength: Int): List[ExplorationState] = {
    if (state.melody.isEmpty || state.melody.length >= maxMelodyLength) {
      // throw new IllegalArgumentException("Invalid state for addHarmonicTone")
      // Just return Nil if invalid? logic in python raises ValueError.
      return Nil
    }
    val lastNote                        = state.melody.last
    var results: List[ExplorationState] = Nil

    val leapDegrees    = List(2, 3, 4, 5, 7, -2, -3, -4, -5, -7)
    val candidateSteps = leapDegrees.map(d => lastNote.value.value + IntervalStep(d)).toSet.toList.sorted

    val allHarmonicPatterns = HarmonicSteps.values.map(_.intervalSteps).toList

    for (step <- candidateSteps) {
      if (step != lastNote.value.value) {
        val newUsedSteps = state.usedHarmonicSteps + step.inversionNormalized
        if (allHarmonicPatterns.exists(p => newUsedSteps.subsetOf(p))) {
          val newMelody = state.melody :+ createStep(step, ToneType.HARMONIC_TONE)
          results ::= ExplorationState(newMelody, false, newUsedSteps)
        }
      }
    }
    results.reverse
  }

  // 現在の旋律に、経過音(群)とそれを解決する和声音を追加した新しい旋律のリストを返す
  private def opAddPassingTones(state: ExplorationState, maxMelodyLength: Int): List[ExplorationState] = {
    if (state.melody.isEmpty) return Nil
    val lastNote                        = state.melody.last
    var results: List[ExplorationState] = Nil
    val allHarmonicPatterns             = HarmonicSteps.values.map(_.intervalSteps).toList

    for (direction <- List(1, -1)) {
      for (numPassingTones <- 1 to 3) {
        // --- 経過音(群)とそれを解決する和声音を追加するパターン ---
        if (state.melody.length + numPassingTones < maxMelodyLength) {
          val passingNotes = (1 to numPassingTones).map { i =>
            createStep(lastNote.value.value + IntervalStep(i * direction), ToneType.PASSING_TONE)
          }.toList

          val targetStep   = lastNote.value.value + IntervalStep((numPassingTones + 1) * direction)
          val newUsedSteps = state.usedHarmonicSteps + targetStep.inversionNormalized

          if (allHarmonicPatterns.exists(p => newUsedSteps.subsetOf(p))) {
            val newMelody = state.melody ++ passingNotes :+ createStep(targetStep, ToneType.HARMONIC_TONE)
            results ::= ExplorationState(newMelody, false, newUsedSteps)
          }
        }

        // --- 経過音(群)が小節の最後まで続くパターン ---
        if (state.melody.length + numPassingTones == maxMelodyLength) {
          val passingNotes = (1 to numPassingTones).map { i =>
            createStep(lastNote.value.value + IntervalStep(i * direction), ToneType.PASSING_TONE)
          }.toList
          val newMelody = state.melody ++ passingNotes
          results ::= ExplorationState(newMelody, false, state.usedHarmonicSteps)
        }
      }
    }
    results.reverse
  }

  // 現在の旋律に、刺繍音とそれを解決する和声音を追加した新しい旋律のリストを返す
  private def opAddNeighborTone(state: ExplorationState, maxMelodyLength: Int): List[ExplorationState] = {
    if (state.melody.isEmpty || state.melody.length > maxMelodyLength - 2) return Nil
    val lastNote                        = state.melody.last
    var results: List[ExplorationState] = Nil

    for (direction <- List(1, -1)) {
      val brStep    = lastNote.value.value + IntervalStep(direction)
      val hStep     = lastNote.value.value
      val newMelody = state.melody ++ List(
        createStep(brStep, ToneType.NEIGHBOR_TONE),
        createStep(hStep, ToneType.HARMONIC_TONE),
      )
      results ::= ExplorationState(newMelody, false, state.usedHarmonicSteps)
    }
    results.reverse
  }

  // -- 小節が埋まってから実行する系

  /** 現在の旋律の音から、次の小節の音とタイの有無の可能な組み合わせを生成する。 */

  private def generateNextMeasureStep(melody: List[AnnotatedIntervalStep]): List[IntervalStep] = {

    val lastStep = melody.last

    var nextSteps: List[IntervalStep] = Nil

    lastStep.value.meta match {

      case ToneType.HARMONIC_TONE =>

        // 和声音で終わる場合の処理

        // タイありパターン

        nextSteps ::= lastStep.value.value

        // タイ無しパターン

        if (melody.length == 1) {

          // 1音のみの場合は上下2, 3, 4, 5, 6, 8度への跳躍が可能

          val leapDegrees = List(2, 3, 4, 5, 6, 8, -2, -3, -4, -5, -6, -8)

          for (degree <- leapDegrees) {

            nextSteps ::= lastStep.value.value + IntervalStep.idx_1(degree)

          }

        } else {

          // NOTE: 簡単に記述するために多少簡易な規則としている

          val secondToLastStep = melody(melody.length - 2)

          val diff = lastStep.value.value - secondToLastStep.value.value

          val direction = if (diff.toIdx1 > 0) 1 else -1

          // 同方向に2度進行

          nextSteps ::= lastStep.value.value + IntervalStep.idx_1(2 * direction)

          // 逆方向に2,3,4,5,6,8度進行

          val oppositeDegrees = List(2, 3, 4, 5, 6, 8)

          for (degree <- oppositeDegrees) {

            nextSteps ::= lastStep.value.value + IntervalStep.idx_1(degree * -direction)

          }

        }

      case ToneType.PASSING_TONE =>

        // 経過音で終わる場合は、順次進行で解決する

        if (melody.length < 2) throw new IllegalStateException("Passing tone must be preceded by another note")

        val secondToLastStep = melody(melody.length - 2)

        val diff = lastStep.value.value - secondToLastStep.value.value

        nextSteps ::= lastStep.value.value + diff

      case ToneType.NEIGHBOR_TONE =>

        // 刺繍音で終わる場合は、元の音に戻る

        if (melody.length < 2) throw new IllegalStateException("Neighbor tone must be preceded by another note")

        val secondToLastStep = melody(melody.length - 2)

        val diff = lastStep.value.value - secondToLastStep.value.value

        nextSteps ::= lastStep.value.value + (diff * -1)

      case _ =>

        // 掛留音で終わる場合は例外。探索中に解決されるはず

        throw new IllegalStateException(s"Unexpected tone type at end: ${lastStep.value.meta}")

    }

    nextSteps.reverse

  }

  private def isValidPattern(p: MeasureStepSequence): Boolean = {

    // 音域チェックなど、全てのパターンに共通するフィルタを適用する

    val allSteps = p.measureNotes.map(_.value.value) :+ p.nextMeasureStep

    val minIdx = allSteps.minBy(_.value).value

    val maxIdx = allSteps.maxBy(_.value).value

    (maxIdx - minIdx) <= (IntervalStep.idx_1(11).value)

  }

  private def createStep(intervalStep: IntervalStep, toneType: ToneType): AnnotatedIntervalStep = {
    AnnotatedIntervalStep(intervalStep, Duration.of(1), toneType)
  }
}
