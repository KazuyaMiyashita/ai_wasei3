package composer.counterpoint.util

import model.elements.{Part, Pitch}

object PartUtil {

  private val SOPRANO = Part.of("Soprano")
  private val ALTO    = Part.of("Alto")
  private val TENOR   = Part.of("Tenor")
  private val BASS    = Part.of("Bass")

  def partRange(part: Part): (Pitch, Pitch) = {
    // Basic implementation assuming the Part hierarchy matches what's used in Python's PartId.
    // In Python: PartId is Enum. Here Part is List[String] wrapper.
    // I will try to match by name.
    val name = part.hierarchy.headOption.getOrElse("")

    // Using contains to be loose, or exact match?
    // Python used Enum.
    // Let's assume standard names.
    if (part == SOPRANO || name.equalsIgnoreCase("Soprano")) (Pitch.parse("C4"), Pitch.parse("A5"))
    else if (part == ALTO || name.equalsIgnoreCase("Alto")) (Pitch.parse("F3"), Pitch.parse("D5"))
    else if (part == TENOR || name.equalsIgnoreCase("Tenor")) (Pitch.parse("C3"), Pitch.parse("A4"))
    else if (part == BASS || name.equalsIgnoreCase("Bass")) (Pitch.parse("F2"), Pitch.parse("D4"))
    else {
      // Default fallback or throw?
      // For now, default to a wide range to avoid blocking, or throw if strict.
      // Throwing seems safer to detect configuration issues.
      throw new IllegalArgumentException(s"Unknown part for range: $part")
    }
  }

  def isInPartRange(pitch: Pitch, part: Part): Boolean = {
    val (min, max) = partRange(part)
    pitch.num.value >= min.num.value && pitch.num.value <= max.num.value
  }

  def comparePartRanges(part1: Part, part2: Part): Int = {
    val order = List("Bass", "Tenor", "Alto", "Soprano")

    val p1Name = part1.hierarchy.headOption.getOrElse("")
    val p2Name = part2.hierarchy.headOption.getOrElse("")

    val idx1 = order.indexWhere(n => p1Name.equalsIgnoreCase(n))
    val idx2 = order.indexWhere(n => p2Name.equalsIgnoreCase(n))

    if (idx1 == -1 || idx2 == -1)
      throw new IllegalArgumentException(s"Invalid Part provided for comparison: $part1, $part2")

    idx1.compare(idx2)
  }
}
