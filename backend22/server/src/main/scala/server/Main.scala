package server

import zio.*
import zio.http.*
import zio.json.*
import zio.logging.backend.SLF4J

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

  val routes = (
    PerformRoutes.routes ++
      ScoreRoutes.routes ++
      CounterpointRoutes.routes ++
      ResourcesRoutes.routes
  ) @@ Middleware.requestLogging()

  def run =
    (for {
      _ <- ZIO.logInfo("Server started at http://127.0.0.1:8080")
      _ <- Server.serve(routes)
    } yield ()).provide(
      Server.defaultWith(_.port(8080).enableRequestStreaming),
    )
}
