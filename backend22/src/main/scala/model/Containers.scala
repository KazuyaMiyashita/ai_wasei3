package model

// 音符 Note, 旋律 Melody, 和音 Chord の定義。これらは任意の要素 A を持つコンテナとして表現する。
// また、MelodyのChord, ChordのMelodyを相互変換するための Grid と、そのための Slice, Sliceable を定義する。

/** 音価を持つということを表す型クラス */
trait HasDuration[A] {
  def getDuration(a: A): Duration

  /** Durationを持つクラスは、その値は0ではいけない。 */
  def validate(a: A): Unit = {
    val duration = getDuration(a)
    assert(duration > Duration.of(0), s"For classes that have Duration, the value cannot be 0. duration: ${duration}")
  }
}

object HasDuration {
  // x.duration で呼び出せるようにするためのユーティリティ
  implicit class HasDurationOps[A](val a: A)(implicit ev: HasDuration[A]) {
    def duration: Duration = ev.getDuration(a)
  }

}

case class Note[A, Attr](value: A, duration: Duration, attribute: Attr) {

  implicitly[HasDuration[Note[A, Attr]]].validate(this)

  def map[B](f: A => B) = copy(value = f(value))

  override def toString: String = s"Note($value, $duration)"
}

object Note {

  // Note に対する定義
  implicit def noteHasDuration[A, Attr]: HasDuration[Note[A, Attr]] = new HasDuration[Note[A, Attr]] {
    def getDuration(n: Note[A, Attr]): Duration = n.duration
  }

}

case class Melody[A: HasDuration](elems: List[A]) {

  import HasDuration.HasDurationOps

  implicitly[HasDuration[Melody[A]]].validate(this)

  def map[B: HasDuration](f: A => B): Melody[B] = Melody(elems.map(f))

  /** このMelodyの各要素にfを適用し、結果のMelodyを結合して新しいMelodyを生成する */
  def flatMap[B: HasDuration](f: A => Melody[B]): Melody[B] = {
    val nestedMelodies: List[Melody[B]] = elems.map(f)
    val flattenedElems: List[B]         = nestedMelodies.flatMap(_.elems)
    Melody(flattenedElems)
  }

  /** Melody[Melody[B]] のようなネストしたMelodyを Melody[B] に平坦化する */
  def flatten[B: HasDuration](implicit ev: A <:< Melody[B]): Melody[B] = {
    val nestedMelodies: List[Melody[B]] = elems.map(ev.apply) // AをMelody[B]にキャスト
    val flattenedElems: List[B]         = nestedMelodies.flatMap(_.elems)
    Melody(flattenedElems)
  }

  /** 開始OffsetをキーとするMapを返す */
  def offsetNotes: Map[Offset, A] = {
    var res  = Map.empty[Offset, A]
    var curr = Offset.of(0)
    for (n <- elems) {
      res = res + (curr -> n)
      curr = curr + n.duration
    }
    res
  }

  /** 指定されたOffsetの時点にある音符と、その開始Offsetを返す */
  def at(target: Offset): (Offset, A) = {

    @annotation.tailrec
    def loop(remainingNotes: List[A], currentOffset: Offset): (Offset, A) = remainingNotes match {
      case Nil =>
        // リストを最後まで探しても見つからなかった場合
        throw new IllegalArgumentException(s"offset $target not found")
      case head :: tail =>
        val endOffset = currentOffset + head.duration
        if (currentOffset <= target && target < endOffset) {
          (currentOffset, head)
        } else {
          loop(tail, endOffset)
        }
    }

    loop(elems, Offset.of(0))
  }

}

/** 旋律。音価を持つものの列 */
object Melody {

  def of[A: HasDuration](elems: A*): Melody[A] = {
    Melody(elems.toList)
  }

  implicit def melodyHasDuration[Id, A: HasDuration]: HasDuration[Melody[A]] = new HasDuration[Melody[A]] {
    import HasDuration.HasDurationOps
    def getDuration(a: Melody[A]): Duration = {
      val ds: List[Duration] = a.elems.map(_.duration)
      ds.reduce(_ + _)
    }
  }

}

/** 和音。空でない集合・識別子Idと値Aの組みを持つ。
  *
  * 値に音価を持つものが指定された場合、その音価は全て等しくなければならず、この和音も音価を持つ。
  */
case class Chord[Id, A](keyElems: Set[(Id, A)])(implicit constraint: Chord.ChordConstraint[Id, A]) {

  assert(keyElems.nonEmpty)

  // もしA: HasDurationならDurationは0でなく、全てのdurationが等しいという検証
  constraint.validate(this)

  def mapKeys[NewId](f: Id => NewId): Chord[NewId, A] =
    Chord(keyElems.map { case (k, v) => (f(k), v) })

  def map[B](f: A => B): Chord[Id, B] =
    Chord(keyElems.map { case (k, v) => (k, f(v)) })

}

object Chord {

  def of[A](values: A*): Chord[Unit, A] = {
    val voices = values.map(a => ((), a)).toSet
    Chord(voices)
  }

  def identified[Id, A](values: (Id, A)*): Chord[Id, A] = {
    Chord(values.toSet)
  }

  implicit def chordHasDuration[Id, A: HasDuration]: HasDuration[Chord[Id, A]] = new HasDuration[Chord[Id, A]] {
    import HasDuration.HasDurationOps
    def getDuration(a: Chord[Id, A]): Duration = {
      a.keyElems.head._2.duration
    }
  }

  // Chordに関する制約。 A: HasDuration かによって制約が変わるため、型クラスで表現される。
  trait ChordConstraint[Id, A] {
    def validate(chord: Chord[Id, A]): Unit
  }

  object ChordConstraint extends LowPriorityConstraint {
    // A: HasDuration の場合の制約
    implicit def validateIfHasDuration[Id, A](implicit ev: HasDuration[A]): ChordConstraint[Id, A] =
      new ChordConstraint[Id, A] {
        def validate(chord: Chord[Id, A]): Unit = {

          // 音価は0でないという HasDuration 側のバリデーション
          ev.validate(chord.keyElems.head._2)

          // 要素の duration が等しいかチェック
          assert(
            chord.keyElems.map { case (_, v) => ev.getDuration(v) }.size == 1,
            s"Duration mismatch in Chord: All notes must have length.",
          )
        }
      }
  }

  // A: HasDuration ではない場合、特に制約はない。
  trait LowPriorityConstraint {
    implicit def allowAnything[Id, A]: ChordConstraint[Id, A] = new ChordConstraint[Id, A] {
      def validate(chord: Chord[Id, A]): Unit = ()
    }
  }

}

// ==========================================
// Slice & Sliceable
// ==========================================

// Slice: Holds a value (usually Note) and connection info
case class Slice[A](value: A, leftConnected: Boolean, rightConnected: Boolean) {

  def map[B](f: A => B): Slice[B] = {
    Slice(f(value), leftConnected, rightConnected)
  }

  override def toString: String = {
    val l = if (leftConnected) "<" else "|"
    val r = if (rightConnected) ">" else "|"
    s"$l$value$r"
  }
}

abstract class Sliceable[A] {

  def slice(target: A, offset: Offset, isContinued: Boolean): (Slice[A], Option[A])
  def join(a: Slice[A], b: Slice[A]): Slice[A]
  def canJoin(a: A, b: A): Boolean

  /** 音符のリストと分割点を与えて、連続したスライスのリストを生成する
    * @param notes
    *   音符のリスト (隙間なく並んでいる前提)
    * @param splitPoints
    *   分割する絶対時刻を与えるイレテータ (0始まり、昇順)。
    *   イテレータを渡した場合、notesが尽きた時点で処理を終了する。
    */
  def sliceList(notes: A*)(splitPoints: Iterator[Offset]): List[Slice[A]] = {

    if (!splitPoints.hasNext) return Nil

    // 最初の点 (通常は 0)
    var prevPoint = splitPoints.next()

    @scala.annotation.tailrec
    def loop(
        remainingNotes: List[A],    // まだ処理していない音符
        currentFragment: Option[A], // 前回のsliceで余った部分 (あれば)
        acc: List[Slice[A]],        // 結果の蓄積
    ): List[Slice[A]] = {

      if (remainingNotes.isEmpty && currentFragment.isEmpty) {
        // 全ての音符を処理し終わったら終了
        acc.reverse
      } else if (!splitPoints.hasNext) {
        // 分割点が尽きた場合
        // 残りの音符はスライスできないため、終了する (または残りを1つのスライスにする等の仕様も考えられるが、ここでは終了とする)
        acc.reverse
      } else {
        val nextPoint = splitPoints.next()
        val cutLen    = nextPoint - prevPoint
        prevPoint = nextPoint // 次のために更新

        // 「処理すべき音符」を特定する
        // fragment(余り)があればそれを、なければリストの先頭を使う
        val (target, nextNotes, isCont) = currentFragment match {
          case Some(frag) => (frag, remainingNotes, true) // 前からの続き
          case None       =>
            if (remainingNotes.isEmpty) throw new RuntimeException("Notes ran out before split points!")
            (remainingNotes.head, remainingNotes.tail, false) // 新しい音
        }

        // sliceを実行
        // target: 切られる音符
        // cutLen: 切る長さ
        // isCont: 左側が繋がっているか(LeftConnected)のフラグ
        val (sliceResult, remainderOpt) = slice(target, cutLen, isCont)

        // 結果を蓄積して次へ
        loop(
          remainingNotes = nextNotes,
          currentFragment = remainderOpt, // 余りがあれば次回のtargetになる
          acc = sliceResult :: acc,
        )
      }
    }

    loop(notes.toList, None, Nil)
  }

}

object Sliceable {

  def sliceList[A](elements: A*)(splitPoints: Iterator[Offset])(implicit ev: Sliceable[A]): List[Slice[A]] = {
    ev.sliceList(elements*)(splitPoints)
  }

  implicit def noteSliceable[A, Attr]: Sliceable[Note[A, Attr]] = new Sliceable[Note[A, Attr]] {

    def slice(
        target: Note[A, Attr],
        offset: Offset,
        isContinued: Boolean,
    ): (Slice[Note[A, Attr]], Option[Note[A, Attr]]) = {
      val (slicedNote, remainingOpt) = if (offset.asDuration >= target.duration) {
        (target, None)
      } else {
        val headNote = target.copy(duration = offset.asDuration)
        val tail     = target.copy(duration = target.duration - offset.asDuration)
        (headNote, Some(tail))
      }

      val sliceObj = Slice(
        value = slicedNote,
        leftConnected = isContinued,
        rightConnected = remainingOpt.isDefined,
      )

      (sliceObj, remainingOpt)
    }

    def join(a: Slice[Note[A, Attr]], b: Slice[Note[A, Attr]]): Slice[Note[A, Attr]] = {
      // Assume values match. In real app, check equality.
      val newNote = a.value.copy(duration = a.value.duration + b.value.duration)
      Slice(newNote, a.leftConnected, b.rightConnected)
    }

    def canJoin(a: Note[A, Attr], b: Note[A, Attr]): Boolean = {
      a.value == b.value && a.attribute == b.attribute
    }

  }

  implicit def melodySliceable[A](implicit ev: Sliceable[A], evDur: HasDuration[A]): Sliceable[Melody[A]] =
    new Sliceable[Melody[A]] {

      import HasDuration.HasDurationOps

      def slice(target: Melody[A], offset: Offset, isContinued: Boolean): (Slice[Melody[A]], Option[Melody[A]]) = {

        @scala.annotation.tailrec
        def loop(
            remaining: List[A],
            currentOffset: Offset,
            acc: List[A],
        ): (List[A], Option[A], List[A]) = { // (HeadList, SplitItem, TailList)
          remaining match {
            case Nil          => (acc.reverse, None, Nil)
            case head :: tail =>
              val nextOffset = currentOffset + head.duration
              if (nextOffset > offset) {
                // Split happens within this note
                // val cutLen = offset - currentOffset
                // If currentOffset is exactly offset, it means split point is at start of this note.
                // But condition > means strictly inside or past end.
                // Wait, if nextOffset == offset, then split is at END of this note.
                // Logic:
                //   curr .... next
                //        ^ offset
                (acc.reverse, Some(head), tail)
              } else if (nextOffset == offset) {
                // Exact boundary
                ((head :: acc).reverse, None, tail)
              } else {
                // offset is further
                loop(tail, nextOffset, head :: acc)
              }
          }
        }

        val (headList, splitItemOpt, tailList) = loop(target.elems, Offset.of(0), Nil)

        val (finalHeadList, finalTailList, rightConn) = splitItemOpt match {
          case Some(item) =>
            // item is the element where the split happens.
            // Calculate cut length based on accumulated duration up to this item.
            val accDuration = headList.map(_.duration).foldLeft(Duration.of(0))(_ + _)
            val cutLen      = offset - Offset(accDuration.value)

            val itemIsLeftContinued      = (headList.isEmpty && isContinued)
            val (itemSlice, itemRestOpt) = ev.slice(item, cutLen, itemIsLeftContinued)

            // The element `itemSlice.value` goes to Head Melody.
            // The element `itemRestOpt.get` (if exists) goes to Tail Melody.
            // The resulting head melody is "right connected" because we split an item.

            val hList = headList :+ itemSlice.value
            val tList = itemRestOpt.map(_ :: tailList).getOrElse(tailList)
            (hList, tList, true)

          case None =>
            // Exact boundary logic
            // Check if we ran out of notes before reaching the offset
            val totalDur = headList.map(_.duration).foldLeft(Duration.of(0))(_ + _)
            if (totalDur < offset.asDuration) {
              // Offset is beyond melody length.
              (headList, Nil, false)
            } else {
              (headList, tailList, false) // Exact cut, no item split
            }
        }

        val headMelody = Melody(finalHeadList)
        val slice      = Slice(headMelody, isContinued, rightConn)

        val restOpt = if (finalTailList.nonEmpty || rightConn) {
          // If rightConn is true, it implies there is a remainder (the other half of the split item),
          // so we should return a tail slice even if the list looks empty (though logically it shouldn't be empty if split happened).
          Some(Melody(finalTailList))
        } else {
          None
        }

        (slice, restOpt)
      }

      def join(a: Slice[Melody[A]], b: Slice[Melody[A]]): Slice[Melody[A]] = {
        val joinedElems = a.value.elems ++ b.value.elems

        // If two slices are connected (a.rightConnected && b.leftConnected),
        // we attempt to merge the boundary elements (the last of a and first of b).
        val finalElems = if (a.rightConnected && b.leftConnected && a.value.elems.nonEmpty && b.value.elems.nonEmpty) {
          val lastA  = a.value.elems.last
          val firstB = b.value.elems.head

          // Reconstruct temporary Slices to delegate to element-level join logic
          val sliceA = Slice(lastA, leftConnected = false, rightConnected = true)
          val sliceB = Slice(firstB, leftConnected = true, rightConnected = false)

          if (ev.canJoin(lastA, firstB)) {
            val joined = ev.join(sliceA, sliceB).value
            a.value.elems.dropRight(1) ++ (joined :: b.value.elems.tail)
          } else {
            joinedElems
          }
        } else {
          joinedElems
        }

        Slice(Melody(finalElems), a.leftConnected, b.rightConnected)
      }

      def canJoin(a: Melody[A], b: Melody[A]): Boolean = {
        // Melodies can always be joined in sequence?
        // Or maybe strictly if they are continuous in time?
        // Sliceable context usually implies they are adjacent slices.
        true
      }
    }
}

/** 楽譜を時間軸のマス目に区切った表現。 各行を横に見るとパートの水平方向の進行を表し、各列を垂直に見るとある時間の和音となる。
  *
  * Idに音高を指定してピアノロールの表現を行ったり、 Idに声部を指定して Melody[Chord[Id, A]] と Chord[Id, Melody[A]] の変換の中間の役割を行う。
  *
  * @param rowParts
  *   各行に対応する Id の指定
  * @param colDurations
  *   各列に対応する Duration の指定
  * @param cells
  *   二次元配列で要素を格納する。 cells[k][i] が パート k のインデックス i における値を表す。
  */
case class Grid[Id, A](
    rowParts: Vector[Id],
    colDurations: Vector[Duration],
    cells: Vector[Vector[A]],
) {
  require(cells.size == rowParts.size)
  require(cells.forall(_.size == colDurations.size))

  private val keyToIndex: Map[Id, Int] = rowParts.zipWithIndex.toMap

  def rowOf(key: Id): Vector[A]    = cells(keyToIndex(key))
  def columnOf(i: Int): Map[Id, A] = {
    rowParts
      .zip(cells)
      .map { case (part, row) =>
        (part, row(i))
      }
      .toMap
  }

  def map[B](f: A => B): Grid[Id, B] = {
    val newCells = cells.map(_.map(f))
    Grid(rowParts, colDurations, newCells)
  }

  /** このGridを和音の列として Melody で返す */
  def toChordsMelody: Melody[Note[Chord[Id, A], Unit]] = {
    val chords: Vector[Note[Chord[Id, A], Unit]] = colDurations.zipWithIndex.map { case (dur, i) =>
      val voices = columnOf(i).toSet
      Note(Chord(voices), dur, ())
    }
    Melody.of(chords*)
  }

}

object Grid {

  implicit class SliceGridOps[Id, A](val grid: Grid[Id, Slice[A]]) extends AnyVal {
    def toMelodies(implicit ev: Sliceable[A], evDur: HasDuration[A]): Map[Id, Melody[A]] = {
      grid.rowParts.map { k =>
        val slices = grid.rowOf(k)
        val notes  =
          if (slices.isEmpty) Nil
          else {
            slices.tail
              .foldLeft(List(slices.head)) { (acc, curr) =>
                val prev = acc.last
                if (prev.rightConnected && curr.leftConnected && ev.canJoin(prev.value, curr.value)) {
                  acc.dropRight(1) :+ ev.join(prev, curr)
                } else {
                  acc :+ curr
                }
              }
              .map(_.value)
          }
        k -> Melody(notes)
      }.toMap
    }
  }

  /** 和音の一覧からGridを作成する ここでは各和音のパートは一致している前提で、最初の和音のパートを利用する。
    */
  def fromChords[Id, A: HasDuration](chords: Chord[Id, A]*): Grid[Id, A] = {
    import HasDuration.HasDurationOps
    val rowParts     = chords.head.keyElems.map(_._1).toVector
    val colDurations = chords.map(_.duration).toVector
    val cells        = rowParts.map { partId =>
      chords.map { chord =>
        chord.keyElems.collectFirst { case (p, v) if p == partId => v }.get
      }.toVector
    }

    Grid(rowParts, colDurations, cells)
  }

  /** パートと旋律の一覧からGridを作成する。 ここでは各旋律のリズムは一致している前提で、最初の旋律のリズムを利用する。
    */
  def fromMelodiesChord[Id, A: HasDuration](
      melodiesChord: Chord[Id, Melody[A]],
      ordering: Ordering[Id],
  ): Grid[Id, A] = {
    import HasDuration.HasDurationOps
    val melodies: List[(Id, Melody[A])] = melodiesChord.keyElems.toList.sortBy(_._1)(using ordering)
    val rowParts                        = melodies.map(_._1).toVector
    val colDurations                    = melodies.head._2.elems.map(_.duration).toVector
    val cells                           = melodies.map(_._2.elems.toVector).toVector
    Grid(rowParts, colDurations, cells)
  }

  /** パートと旋律の一覧からGridを作成する。 ここでは各旋律のリズムが異なることを考慮し、いずれかの旋律が移動したタイミングでスライス処理を行い、Gridを作成する。
    *
    * A には典型的には Note[Pitch] が入る。しかしGridにした時にNoteの情報があるとGridが持つcolDurationsとの矛盾が生じうるため、 Gridには Slice[Pitch] を渡したい。
    * また、Gridの用途によってはSliceは不要の場合もある。 そのため、Slice[A] => B を渡し、どのようにAからスライスと音価の情報を剥がすかを指定する。
    */
  def fromPolyphonicMelodiesChord[Id, A, B](
      melodiesChord: Chord[Id, Melody[A]],
      ordering: Ordering[Id],
  )(
      project: Slice[A] => B,
  )(implicit ev: Sliceable[A], ev2: HasDuration[A]): Grid[Id, B] = {
    import HasDuration.HasDurationOps

    val melodies: List[(Id, Melody[A])] = melodiesChord.keyElems.toList.sortBy(_._1)(using ordering)

    // 1. 全パートのイベント時刻(Offset)を収集
    // scanLeftで各ノートの終了時刻を累積していく
    val timePoints = melodies
      .flatMap { case (_, melody) =>
        melody.elems.scanLeft(Offset.of(0)) { (acc, note) => acc + note.duration }
      }
      .toSet
      .toVector
      .sorted

    // 2. 列ごとのDuration (Offset差分)
    // 隣り合う時刻の差が各列の長さになる
    val colDurations = timePoints.zip(timePoints.tail).map { case (curr, next) => (next - curr).asDuration }

    // 3. 各パートをスライス
    // sliceList は Offset のイテレータを期待している
    val cells = melodies.map { case (_, melody) =>
      ev.sliceList(melody.elems*)(timePoints.iterator).map(project).toVector
    }.toVector

    val rowParts = melodies.map(_._1).toVector

    Grid(rowParts, colDurations, cells)
  }

}
