import zio.*
import zio.http.*
import zio.json.*
import zio.logging.backend.SLF4J

import scala.xml.XML
import sheet.meicmn.MeiXML
import sheet.MeiScore
import performer.Performer
import scala.xml.PrettyPrinter

case class ErrorResponse(message: String)
object ErrorResponse {
  implicit val encoder: JsonEncoder[ErrorResponse] = DeriveJsonEncoder.gen[ErrorResponse]
}

case class ScoreEntry(path: String, name: String)
object ScoreEntry {
  implicit val encoder: JsonEncoder[ScoreEntry] = DeriveJsonEncoder.gen[ScoreEntry]
}

object GreetingServer extends ZIOAppDefault {

  override val bootstrap = Runtime.removeDefaultLoggers >>> SLF4J.slf4j

  val routes =
    Routes(
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
          events       <- ZIO.attempt(Performer.perform(meiScore.toScore.partwise, meiScore.tempo.getOrElse(80.0)))
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
      Method.POST / "partwise" -> handler { (req: Request) =>
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
          xml             <- ZIO.attempt(XML.loadString(content))
          meiStructure    <- ZIO.attempt(MeiXML.load(xml))
          meiScore        <- ZIO.attempt(MeiScore(meiStructure))
          convertedMeiXml <- ZIO.attempt(new PrettyPrinter(Int.MaxValue, 2).format(MeiXML.toXml(meiScore.partwise)))
        } yield Response.text(convertedMeiXml).addHeader(Header.ContentType(MediaType.application.xml)))
          .catchAll(e =>
            ZIO.logErrorCause("Error transform to partwise score", Cause.fail(e)) *>
              ZIO.succeed(Response.internalServerError(e.getMessage)),
          )
      },
      Method.GET / "score" -> handler {
        val resourcePath = "/data/mei"
        Option(getClass.getResource(resourcePath)) match {
          case Some(u) if u.getProtocol == "file" =>
            ZIO
              .attempt {
                val dir   = new java.io.File(u.toURI)
                val files = dir.listFiles()
                if (files != null) {
                  val entries = files
                    .filter(f => f.isFile && f.getName.endsWith(".mei"))
                    .map(_.getName)
                    .sorted
                    .map { name =>
                      val id = name.stripSuffix(".mei")
                      ScoreEntry(s"/score/$id", s"challan-$id")
                    }
                  Response.json(entries.toJson)
                } else {
                  Response.internalServerError("Failed to list files")
                }
              }
              .catchAll(e => ZIO.succeed(Response.internalServerError(e.getMessage)))
          case Some(u) if u.getProtocol == "jar" =>
            ZIO
              .attempt {
                val path    = u.getPath
                val jarPath = path.substring(5, path.indexOf("!")) // remove "file:" and get path before "!"
                val jar     = new java.util.jar.JarFile(java.net.URLDecoder.decode(jarPath, "UTF-8"))
                val entries = jar.entries()
                val files   = scala.collection.mutable.ListBuffer[String]()
                while (entries.hasMoreElements) {
                  val entry = entries.nextElement()
                  val name  = entry.getName
                  // entry name usually doesn't start with /, but resourcePath does.
                  // data/mei/xxx.mei
                  if (name.startsWith("data/mei/") && name.endsWith(".mei")) {
                    val filename = name.substring("data/mei/".length)
                    if (filename.nonEmpty && !filename.contains("/")) { // Ensure it's directly in the dir
                      files += filename
                    }
                  }
                }
                jar.close()

                val scoreEntries = files.toList.sorted.map { name =>
                  val id = name.stripSuffix(".mei")
                  ScoreEntry(s"/score/$id", s"challan-$id")
                }
                Response.json(scoreEntries.toJson)
              }
              .catchAll(e => ZIO.succeed(Response.internalServerError(s"Error reading jar: ${e.getMessage}")))
          case _ =>
            ZIO.succeed(Response.internalServerError("Resource directory not found"))
        }
      },
      Method.GET / "score" / string("id") -> handler { (id: String, _: Request) =>
        val resourcePath = s"/data/mei/$id.mei"
        Option(getClass.getResource(resourcePath)) match {
          case Some(url) =>
            ZIO
              .attemptBlocking(scala.io.Source.fromURL(url)(using scala.io.Codec.UTF8).mkString)
              .map { content =>
                Response.text(content).addHeader(Header.ContentType(MediaType.application.xml))
              }
              .catchAll(e => ZIO.succeed(Response.internalServerError(e.getMessage)))
          case None =>
            ZIO.succeed(Response.notFound)
        }
      },
    ) @@ Middleware.requestLogging()

  def run =
    (for {
      _ <- ZIO.logInfo("Server started at http://127.0.0.1:8080")
      _ <- Server.serve(routes)
    } yield ()).provide(
      Server.defaultWith(_.port(8080).enableRequestStreaming),
    )
}
