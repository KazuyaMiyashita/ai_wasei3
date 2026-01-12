package server

import zio.*
import zio.http.*
import zio.json.*
import zio.stream.ZStream
import composer.counterpoint.CounterpointGenerator
import composer.counterpoint.model.Species
import model.elements.{Key, Pitch, Part}
import sheet.MeiScore
import scala.xml.PrettyPrinter
import java.util.Base64

case class FileReadyData(count: Int, id: String, content_base64: String, filename: String)
object FileReadyData {
  implicit val encoder: JsonEncoder[FileReadyData] = DeriveJsonEncoder.gen[FileReadyData]
}

case class CompleteData(total: Int)
object CompleteData {
  implicit val encoder: JsonEncoder[CompleteData] = DeriveJsonEncoder.gen[CompleteData]
}

case class ErrorData(message: String)
object ErrorData {
  implicit val encoder: JsonEncoder[ErrorData] = DeriveJsonEncoder.gen[ErrorData]
}

object CounterpointRoutes {

  val printer = new PrettyPrinter(Int.MaxValue, 2)

  val routes = Routes(
    Method.POST / "counterpoint" -> handler { (req: Request) =>
      (for {
        form  <- req.body.asMultipartForm.mapError(e => new Exception(s"Multipart parse error: ${e.getMessage}"))
        limit <- ZIO
          .attempt {
            form.get("limit").flatMap {
              // limitのContent-Typeがtext/plain の場合
              case f if f.stringValue.isDefined => f.stringValue
              // Content-Typeが指定されていない場合
              case FormField.Binary(_, data, _, _, _) =>
                Some(new String(data.toArray, java.nio.charset.StandardCharsets.UTF_8))
              // ストリーミングバイナリの場合が該当するが、このlimitフィールドの大きさでは起こらないだろう。
              case _ => None
            }

          }
          .flatMap {
            case Some(value) =>
              value.toIntOption match {
                case Some(l) if l >= 1 && l <= 100 => ZIO.succeed(l)
                case Some(_) => ZIO.fail(new Exception("Parameter 'limit' must be between 1 and 100"))
                case None    => ZIO.fail(new Exception(s"Parameter 'limit' is not a valid number: $value"))
              }
            case None =>
              ZIO.fail(
                new Exception(
                  s"Parameter 'limit' is missing. Available fields: ${form.formData.map(_.name).mkString(", ")}",
                ),
              )
          }

        _ <- ZIO.attempt {
          if (form.get("file").isEmpty) throw new Exception("Field 'file' is missing")
        }

        stream: ZStream[Any, Nothing, ServerSentEvent[String]] = ZStream
          .fromIterator(
            CounterpointGenerator(
              cantusFirmus = "C4 A3 G3 E3 F3 A3 G3 E3 D3 C3 ".split(" ").map(Pitch.parse).toList,
              cfPart = Part.of("Bass"),
              key = Key.parse("C Major"),
              species = Species.FIFTH_SPECIES,
              part = Part.of("Tenor"),
            ).generateScores.take(limit).zipWithIndex,
          )
          .mapZIO { case (sheet, index) =>
            // Simulate processing time
            ZIO.sleep(Duration.fromMillis(scala.util.Random.between(50, 500))) *> ZIO.attempt {
              val mei    = MeiScore.fromSheetMusic(sheet.copy(title = Some(s"generated ${index + 1}")))
              val xmlStr = printer.format(mei)
              val base64 = Base64.getEncoder.encodeToString(xmlStr.getBytes(java.nio.charset.StandardCharsets.UTF_8))

              val data = FileReadyData(
                count = index + 1,
                id = java.util.UUID.randomUUID().toString,
                content_base64 = base64,
                filename = s"counterpoint_${index + 1}.mei",
              )

              ServerSentEvent(
                data = data.toJson,
                eventType = Some("file_ready"),
              )
            }
          }
          .catchAll { (e: Throwable) =>
            ZStream.succeed(
              ServerSentEvent(
                data = ErrorData(e.getMessage).toJson,
                eventType = Some("error"),
              ),
            )
          } ++ ZStream.succeed(
          ServerSentEvent(
            data = CompleteData(limit).toJson,
            eventType = Some("complete"),
          ),
        )

      } yield Response.fromServerSentEvents(stream))
        .catchAll {
          case e if e.getMessage.contains("limit") || e.getMessage.contains("missing") =>
            ZIO.succeed(
              Response.json(ErrorResponse(e.getMessage).toJson).status(Status.BadRequest),
            )
          case e =>
            ZIO.logErrorCause("Error in counterpoint generation", Cause.fail(e)) *>
              ZIO.succeed(
                Response.json(ErrorResponse(e.getMessage).toJson).status(Status.InternalServerError),
              )
        }
    },
  )
}
