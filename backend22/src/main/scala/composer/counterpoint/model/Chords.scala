package composer.counterpoint.model

import model.elements.Degree
import model.elements.Degree.DegreeStep

enum Inversion {
  case ROOT
  case FIRST
  case SECOND
}

case class HarmonicChord[T](elements: Set[T]) {
  def contains(elem: T): Boolean = elements.contains(elem)
}

case class ChordWithBass[T](chord: HarmonicChord[T], bass: T) {
  require(chord.contains(bass), "The bass must be included in the chord.")
}

type HarmonicDegreeStepChord = HarmonicChord[DegreeStep]
type HarmonicDegreeChord     = HarmonicChord[Degree]

object DegreeStepChord {

  def of(args: DegreeStep*): HarmonicDegreeStepChord = HarmonicChord(args.toSet)

  extension (c: HarmonicDegreeStepChord) {
    def isTriad: Boolean = {
      if (c.elements.size != 3) false
      else getRoot(c).isDefined
    }

    def root: Option[DegreeStep] = getRoot(c)
  }

  def getRoot(c: HarmonicDegreeStepChord): Option[DegreeStep] = {
    if (c.elements.size != 3) return None
    c.elements.find { step =>
      c.elements.contains(step + DegreeStep(2)) && c.elements.contains(step + DegreeStep(4))
    }
  }

  def triadFromRoot(root: DegreeStep): HarmonicDegreeStepChord = {
    HarmonicChord(Set(root, root + DegreeStep(2), root + DegreeStep(4)))
  }

  def triadFromBassAsFirstInversion(bass: DegreeStep): HarmonicDegreeStepChord = {
    val root = bass - DegreeStep(2)
    triadFromRoot(root)
  }

  def triadsContaining(step: DegreeStep): List[HarmonicDegreeStepChord] = {
    // 1. step が根音の場合
    val asRoot = triadFromRoot(step)
    // 2. step が第3音の場合 (ルートは step - 2)
    val asThird = triadFromRoot(step - DegreeStep(2))
    // 3. step が第5音の場合 (ルートは step - 4)
    val asFifth = triadFromRoot(step - DegreeStep(4))

    List(asRoot, asThird, asFifth)
  }
}

case class DegreeStepChordWithBass(chord: HarmonicDegreeStepChord, bass: DegreeStep) {
  require(chord.contains(bass), "The bass must be included in the chord.")

  def inversionType: Inversion = {
    val rootOpt = DegreeStepChord.getRoot(chord)
    if (rootOpt.isEmpty) {
      throw new IllegalArgumentException("Not a triad chord")
    }
    val root = rootOpt.get

    // Modulo 7 の世界なので差分で判定する
    val diff = model.elements.Math.mod(bass.value - root.value, 7)

    if (diff == 0) Inversion.ROOT
    else if (diff == 2) Inversion.FIRST
    else if (diff == 4) Inversion.SECOND
    else throw new IllegalArgumentException(s"Unexpected bass relationship: $diff")
  }
}

object DegreeStepChordWithBass {
  def getBassForInversion(chord: HarmonicDegreeStepChord, inv: Inversion): DegreeStep = {
    val rootOpt = DegreeStepChord.getRoot(chord)
    if (rootOpt.isEmpty) throw new IllegalArgumentException("Not a triad chord")
    val root = rootOpt.get

    inv match {
      case Inversion.ROOT   => root
      case Inversion.FIRST  => root + DegreeStep(2)
      case Inversion.SECOND => root + DegreeStep(4)
    }
  }
}

object DegreeChord {

  def of(args: Degree*): HarmonicDegreeChord = HarmonicChord(args.toSet)

  val I: HarmonicDegreeChord           = DegreeChord.of(Degree.idx1(1, 0), Degree.idx1(3, 0), Degree.idx1(5, 0))
  val II: HarmonicDegreeChord          = DegreeChord.of(Degree.idx1(2, 0), Degree.idx1(4, 0), Degree.idx1(6, 0))
  val V: HarmonicDegreeChord           = DegreeChord.of(Degree.idx1(5, 0), Degree.idx1(7, 0), Degree.idx1(2, 0))
  val V_leading: HarmonicDegreeChord   = DegreeChord.of(Degree.idx1(5, 0), Degree.idx1(7, 1), Degree.idx1(2, 0))
  val VII: HarmonicDegreeChord         = DegreeChord.of(Degree.idx1(7, 0), Degree.idx1(2, 0), Degree.idx1(4, 0))
  val VII_leading: HarmonicDegreeChord = DegreeChord.of(Degree.idx1(7, 1), Degree.idx1(2, 0), Degree.idx1(4, 0))
}
