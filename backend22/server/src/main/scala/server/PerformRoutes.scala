package server

import zio.*
import zio.http.*
import zio.json.*
import sheet.meicmn.MeiXML
import sheet.MeiScore
import performer.Performer
import model.containers.PartwiseScore

import scala.xml.XML

object PerformRoutes {
  val routes = Routes(
    Method.POST / "perform" -> handler { (req: Request) =>
      (for {
        contentType <- ZIO
          .fromOption(req.header(Header.ContentType))
          .orElseFail(new Exception("Content-Type header missing"))

        boundary <- ZIO
          .fromOption(contentType.boundary)
          .orElseFail(new Exception("Boundary missing in Content-Type"))

        boundaryStr = boundary.id

        // Create new MediaType with boundary parameter
        newMediaType = contentType.mediaType
          .copy(parameters = contentType.mediaType.parameters + ("boundary" -> boundaryStr))

        bytes <- req.body.asChunk

        newBody = Body.fromChunk(bytes, newMediaType)
        form <- newBody.asMultipartForm

        content <- ZIO.attempt {
          form.get("file") match {
            case Some(field) =>
              field match {
                case b: FormField.Binary =>
                  new String(b.data.toArray, java.nio.charset.StandardCharsets.UTF_8)
                case t: FormField.Text =>
                  t.value
                case s: FormField.Simple =>
                  s.value
                case _: FormField.StreamingBinary =>
                  throw new Exception("Unexpected StreamingBinary field")
              }
            case None =>
              throw new Exception("Field 'file' is missing")
          }
        }
        xml          <- ZIO.attempt(XML.loadString(content))
        meiStructure <- ZIO.attempt(MeiXML.load(xml))
        meiScore     <- ZIO.attempt(MeiScore(meiStructure))
        events       <- ZIO.attempt(
          Performer.perform(PartwiseScore.partwise(meiScore.toScore), meiScore.tempo.getOrElse(80.0)),
        )
        resultString <- ZIO.succeed(
          events
            .map { event =>
              s"${event.address} ${event.args.mkString(" ")}"
            }
            .mkString("\n"),
        )
      } yield Response.text(resultString))
        .catchAll { e =>
          ZIO.logErrorCause("Error performing score", Cause.fail(e)) *>
            ZIO.succeed(
              Response
                .json(ErrorResponse(s"演奏ファイルを作成できませんでした。 ${e.getMessage}").toJson)
                .status(Status.InternalServerError),
            )
        }
    },
  )
}
