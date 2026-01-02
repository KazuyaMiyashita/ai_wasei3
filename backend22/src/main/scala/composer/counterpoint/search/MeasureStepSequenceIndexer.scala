package composer.counterpoint.search

import composer.counterpoint.model.MeasureRythmnPattern
import model.elements.Interval.IntervalStep
import RythmnApplyer.tryApplyRythmn
import composer.counterpoint.search.MeasureStepSequence.inversionNormalized

enum Operator {
  case EQUAL
  case IN
  case IS_SUBSET_OF
  case AND
  case OR
  case GREATER_THAN_OR_EQUAL
  case LESS_THAN_OR_EQUAL
}

enum SearchField {
  case NUM_NOTES_IN_MEASURE
  case NEXT_MEASURE_STEP
  case FIRST_NOTE_INTERVAL_STEP
  case USED_HARMONIC_STEPS
  case IS_TIED_TO_NEXT_MEASURE_REQUIRED
  case RYTHMN_PATTERNS
  case MIN_STEP
  case MAX_STEP
}

sealed trait Condition {
  def and(other: Condition): Condition
  def or(other: Condition): Condition
}

case class LeafCondition(field: SearchField, op: Operator, value: Any) extends Condition {
  def and(other: Condition): Condition = CompositeCondition(Operator.AND, List(this, other))
  def or(other: Condition): Condition  = CompositeCondition(Operator.OR, List(this, other))
}

case class CompositeCondition(op: Operator, conditions: List[Condition]) extends Condition {
  def and(other: Condition): Condition =
    if (op == Operator.AND) copy(conditions = conditions :+ other)
    else CompositeCondition(Operator.AND, List(this, other))
  def or(other: Condition): Condition =
    if (op == Operator.OR) copy(conditions = conditions :+ other)
    else CompositeCondition(Operator.OR, List(this, other))
}

case class QueryField(field: SearchField) {
  def equal(value: Any): LeafCondition      = LeafCondition(field, Operator.EQUAL, value)
  def isIn(value: Any): LeafCondition       = LeafCondition(field, Operator.IN, value)
  def isSubsetOf(value: Any): LeafCondition = LeafCondition(field, Operator.IS_SUBSET_OF, value)
  def ge(value: Any): LeafCondition         = LeafCondition(field, Operator.GREATER_THAN_OR_EQUAL, value)
  def le(value: Any): LeafCondition         = LeafCondition(field, Operator.LESS_THAN_OR_EQUAL, value)
}

object Q {
  def apply(field: SearchField): QueryField = QueryField(field)
}

class MeasureStepSequenceIndexer(
    sequences: List[MeasureStepSequence],
    allRythmnPatterns: List[MeasureRythmnPattern],
) {

  private val sequenceMap                 = sequences.zipWithIndex.map { case (seq, i) => seq -> i }.toMap
  private val (index, idToRythmnPatterns) = buildIndex(sequences, allRythmnPatterns)

  private def buildIndex(
      sequences: List[MeasureStepSequence],
      allRythmnPatterns: List[MeasureRythmnPattern],
  ): (Map[SearchField, Map[Any, List[Int]]], Map[Int, Set[MeasureRythmnPattern]]) = {
    // logger.info("MeasureStepSequenceのインデックス構築を開始します")

    // Mutable maps for building
    val tempIndex = scala.collection.mutable.Map[SearchField, scala.collection.mutable.Map[Any, List[Int]]]()
    SearchField.values.foreach(f => tempIndex(f) = scala.collection.mutable.Map[Any, List[Int]]().withDefaultValue(Nil))

    val tempIdToPatterns = scala.collection.mutable.Map[Int, Set[MeasureRythmnPattern]]().withDefaultValue(Set.empty)

    sequences.zipWithIndex.foreach { case (result, i) =>
      def add(field: SearchField, value: Any): Unit = {
        val map = tempIndex(field)
        map(value) = map(value) :+ i
      }

      add(SearchField.NUM_NOTES_IN_MEASURE, result.numNotesInMeasure)
      add(SearchField.NEXT_MEASURE_STEP, result.nextMeasureStep)
      add(SearchField.FIRST_NOTE_INTERVAL_STEP, result.firstNoteIntervalStepOfMeasure)
      add(SearchField.USED_HARMONIC_STEPS, result.usedHarmonicSteps)
      add(SearchField.IS_TIED_TO_NEXT_MEASURE_REQUIRED, result.isTiedToNextMeasureRequired)
      add(SearchField.MIN_STEP, result.minStep)
      add(SearchField.MAX_STEP, result.maxStep)

      for (rythmnPattern <- allRythmnPatterns) {
        if (tryApplyRythmn(result, rythmnPattern).isDefined) {
          add(SearchField.RYTHMN_PATTERNS, rythmnPattern)
          tempIdToPatterns(i) = tempIdToPatterns(i) + rythmnPattern
        }
      }
    }

    // Convert to immutable
    val immutableIndex        = tempIndex.map { case (k, v) => k -> v.toMap }.toMap
    val immutableIdToPatterns = tempIdToPatterns.toMap

    // logger.info("MeasureStepSequenceのインデックス構築を完了しました")

    (immutableIndex, immutableIdToPatterns)
  }

  def getCompatibleRythmnPatterns(sequence: MeasureStepSequence): Set[MeasureRythmnPattern] = {
    sequenceMap.get(sequence).flatMap(idToRythmnPatterns.get).getOrElse(Set.empty)
  }

  def find(condition: Condition = null): List[MeasureStepSequence] = {
    if (condition == null) return sequences
    val indices = evaluateCondition(condition)
    indices.toList.sorted.map(sequences)
  }

  private def evaluateCondition(condition: Condition): Set[Int] = {
    condition match {
      case LeafCondition(field, op, value) =>
        val fieldIndex = index.getOrElse(field, Map.empty)

        op match {
          case Operator.EQUAL =>
            fieldIndex.getOrElse(value, Nil).toSet
          case Operator.IN =>
            val values = value.asInstanceOf[Iterable[Any]]
            values.flatMap(v => fieldIndex.getOrElse(v, Nil)).toSet
          case Operator.IS_SUBSET_OF =>
            val targetSet = value.asInstanceOf[Set[IntervalStep]].map(_.inversionNormalized)
            require(targetSet.contains(IntervalStep(0)), "available_harmonic_steps must always contain IntervalStep(0)")

            fieldIndex.flatMap { case (key, indices) =>
              val keySet = key.asInstanceOf[Set[IntervalStep]]
              if (keySet.subsetOf(targetSet)) indices else Nil
            }.toSet
          case Operator.GREATER_THAN_OR_EQUAL =>
            val targetVal = value.asInstanceOf[IntervalStep]
            fieldIndex.flatMap { case (key, indices) =>
              val keyVal = key.asInstanceOf[IntervalStep]
              if (keyVal.value >= targetVal.value) indices else Nil
            }.toSet
          case Operator.LESS_THAN_OR_EQUAL =>
            val targetVal = value.asInstanceOf[IntervalStep]
            fieldIndex.flatMap { case (key, indices) =>
              val keyVal = key.asInstanceOf[IntervalStep]
              if (keyVal.value <= targetVal.value) indices else Nil
            }.toSet
          case _ => throw new IllegalArgumentException(s"Unsupported operator: $op")
        }

      case CompositeCondition(op, conditions) =>
        if (op == Operator.AND) {
          val sets = conditions.map(evaluateCondition)
          if (sets.isEmpty) Set.empty
          else sets.reduceLeft(_.intersect(_))
        } else if (op == Operator.OR) {
          conditions.map(evaluateCondition).reduceLeftOption(_.union(_)).getOrElse(Set.empty)
        } else {
          throw new IllegalArgumentException(s"Unsupported operator for composite: $op")
        }
    }
  }
}
