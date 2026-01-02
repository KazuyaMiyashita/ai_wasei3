package performer

import java.lang.{Math => JMath}

import scala.collection.mutable.ListBuffer

import model.containers.{Note, PartwiseScore}
import model.elements.{Pitch, Rest}
import sheet.NoteInfo

/** /performer/begin
  * /performer/1/intensity 34.594105 -33.526672
  * /performer/1/pitch 36.094105 0.
  * /performer/1/intensity 42.594105 -33.112022
  * /performer/1/intensity 50.594105 -31.145025
  * /performer/1/pitch 51.094105 0.
  * /performer/1/intensity 58.594105 -30.944626
  * /performer/end
  */
case class PerformerEvent(
    address: String,
    args: List[Any],
)

object Performer {

  def perform(partwizeScore: PartwiseScore[NoteInfo, Unit], tempo: Double): List[PerformerEvent] = {
    val events = ListBuffer[PerformerEvent]()
    // 1 beat (quarter note) = 60000 / tempo ms
    val msPerBeat = 60000.0 / tempo

    val partSize = partwizeScore.elems.size

    // Assign unique ID for each part (1, 2, 3...) based on iteration order
    partwizeScore.elems.zipWithIndex.foreach { case (melody, index) =>
      val partId          = index + 1
      var currentTime     = 0.0
      val phraseBuffer    = ListBuffer[Note[NoteInfo, Unit]]()
      var phraseStartTime = 0.0

      val notes: List[Note[Option[NoteInfo], Unit]] = melody.elems
      notes.foreach { note =>
        val durationMs = note.duration.value.toDouble * msPerBeat

        note.value match {
          case Some(info) =>
            info.value match {
              case _: Pitch =>
                if (phraseBuffer.isEmpty) {
                  phraseStartTime = currentTime
                }
                phraseBuffer += note.mapValue(_ => info)
              case Rest =>
                // End current phrase if any
                if (phraseBuffer.nonEmpty) {
                  val vibPhase = (index.toDouble / partSize) * 2 * JMath.PI // Avoid having the same phase
                  events ++= generatePhraseEvents(phraseStartTime, phraseBuffer.toList, partId, msPerBeat, vibPhase)
                  phraseBuffer.clear()
                }
                // Add noteon/noteoff for Rest
                events += PerformerEvent(
                  s"/metadata/$partId/noteon",
                  List(currentTime.toFloat, info.id),
                )
                events += PerformerEvent(
                  s"/metadata/$partId/noteoff",
                  List((currentTime + durationMs).toFloat, info.id),
                )

                // Send next note's pitch with min intensity to prevent portamento interpolation on the receiver side
                val nextPitchNote: Option[Note[NoteInfo, Unit]] =
                  notes.dropWhile(_ != note).tail.collectFirst {
                    case n if n.value.isDefined && n.value.get.value.isInstanceOf[Pitch] => n.mapValue(_.get)
                  }
                nextPitchNote.foreach { nextNote =>
                  val nextFreq = pitchToFreq(nextNote.value.value.asInstanceOf[Pitch])
                  events += PerformerEvent(
                    s"/performer/$partId/intensity",
                    List(currentTime.toFloat, -96.0f), // intMinDb
                  )
                  events += PerformerEvent(
                    s"/performer/$partId/pitch",
                    List(currentTime.toFloat, nextFreq.toFloat),
                  )
                }
            }

          // If this part is blank, do nothing
          case None => ()
        }

        currentTime += durationMs
      }

      // End remaining phrase at the end of part
      if (phraseBuffer.nonEmpty) {
        val vibPhase = (index.toDouble / partSize) * 2 * JMath.PI // Avoid having the same phase
        events ++= generatePhraseEvents(phraseStartTime, phraseBuffer.toList, partId, msPerBeat, vibPhase)
        phraseBuffer.clear()
      }
    }

    // Sort by time
    val sortedEvents = events.toList.sortBy(_.args.head.asInstanceOf[Float])

    val beginEvent = PerformerEvent("/performer/begin", List())
    val endEvent   = PerformerEvent("/performer/end", List())

    beginEvent :: sortedEvents ::: List(endEvent)
  }

  private def pitchToFreq(pitch: Pitch): Double = {
    val num = pitch.num.value
    440.0 * JMath.pow(2.0, (num - 9.0) / 12.0)
  }

  private def generatePhraseEvents(
      startTime: Double,
      notes: List[Note[NoteInfo, Unit]],
      partId: Int,
      msPerBeat: Double,
      vibPhase: Double,
  ): List[PerformerEvent] = {
    val eventBuffer = ListBuffer[PerformerEvent]()
    val interval    = 2.0 // sampling interval ms

    val intMinDb = -96.0
    val intMaxDb = -18.0

    // Parameters for same-note articulation (bigger dip)
    val articDipDb    = 12.0
    val articDipDecay = 50.0
    val articDipRise  = 5.0

    // Parameters for note transition (smaller dip)
    val transDipDb    = 3.0
    val transDipDecay = 20.0
    val transDipRise  = 2.0

    val vibRate        = 4.0
    val vibDepthCents  = 4.0
    val portamentoTime = 4.0

    var currentNoteStartTime = startTime

    // Iterate through notes in the phrase
    for (i <- notes.indices) {
      val note         = notes(i)
      val info         = note.value
      val isTieStarted = info.isTieStarted
      val isTieEnded   = info.isTieEnded
      val durationMs   = note.duration.value.toDouble * msPerBeat
      val freq         = pitchToFreq(info.value.asInstanceOf[Pitch])
      val prevFreq     = if (i > 0) pitchToFreq(notes(i - 1).value.value.asInstanceOf[Pitch]) else freq
      val isSameAsPrev = i > 0 && notes(i - 1).value.value == note.value.value
      val isSameAsNext = i < notes.length - 1 && notes(i + 1).value.value == note.value.value

      // Add noteon event
      eventBuffer += PerformerEvent(
        s"/metadata/$partId/noteon",
        List(currentNoteStartTime.toFloat, info.id),
      )

      // Sample points within the note
      var t = 0.0
      while (t < durationMs) {
        val absTime = currentNoteStartTime + t

        // Portamento calculation
        var currentFreq = freq
        if (i > 0 && t < portamentoTime && !isTieEnded) {
          val phase     = t / portamentoTime
          val logPrev   = JMath.log(prevFreq)
          val logTarget = JMath.log(freq)
          val logCurr   = logPrev + (logTarget - logPrev) * phase
          currentFreq = JMath.exp(logCurr)
        }

        // Vibrato calculation
        val tSec        = absTime / 1000.0
        val deltaCents  = vibDepthCents * JMath.sin(2 * JMath.PI * vibRate * tSec + vibPhase)
        val vibratoFreq = currentFreq * JMath.pow(2, deltaCents / 1200.0)

        // Calculate Intensity
        var currentDb = intMaxDb

        val timeUntilEnd = durationMs - t

        // 3. Note Transition (Dip)

        // Rise at note start
        if (t < articDipRise || t < transDipRise) {
          if (!isTieEnded) {
            if (i == 0 || isSameAsPrev) {
              // Articulation dip (or phrase start)
              if (t < articDipRise) {
                val risePhase = t / articDipRise
                val riseVal   = (intMaxDb - articDipDb) + articDipDb * risePhase
                currentDb = JMath.min(currentDb, riseVal)
              }
            } else {
              // Transition dip (different pitch)
              if (t < transDipRise) {
                val risePhase = t / transDipRise
                val riseVal   = (intMaxDb - transDipDb) + transDipDb * risePhase
                currentDb = JMath.min(currentDb, riseVal)
              }
            }
          }
        }

        // Decay at note end
        if (timeUntilEnd < articDipDecay || timeUntilEnd < transDipDecay) {
          if (!isTieStarted) {
            if (i == notes.length - 1 || isSameAsNext) {
              // Articulation dip (or phrase end)
              if (timeUntilEnd < articDipDecay) {
                val decayPhase = (articDipDecay - timeUntilEnd) / articDipDecay
                val decayVal   = intMaxDb - articDipDb * decayPhase
                currentDb = JMath.min(currentDb, decayVal)
              }
            } else {
              // Transition dip (different pitch)
              if (timeUntilEnd < transDipDecay) {
                val decayPhase = (transDipDecay - timeUntilEnd) / transDipDecay
                val decayVal   = intMaxDb - transDipDb * decayPhase
                currentDb = JMath.min(currentDb, decayVal)
              }
            }
          }
        }

        eventBuffer += PerformerEvent(
          s"/performer/$partId/intensity",
          List(absTime.toFloat, currentDb.toFloat),
        )
        eventBuffer += PerformerEvent(
          s"/performer/$partId/pitch",
          List(absTime.toFloat, vibratoFreq.toFloat),
        )

        t += interval
      }

      // Add noteoff event
      eventBuffer += PerformerEvent(
        s"/metadata/$partId/noteoff",
        List((currentNoteStartTime + durationMs).toFloat, info.id),
      )

      currentNoteStartTime += durationMs
    }

    // Add a final point at the very end of the phrase
    val phraseEndTime = currentNoteStartTime
    eventBuffer += PerformerEvent(
      s"/performer/$partId/intensity",
      List(phraseEndTime.toFloat, intMinDb.toFloat),
    )

    eventBuffer.toList
  }

}
