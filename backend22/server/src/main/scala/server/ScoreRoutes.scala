package server

import zio.*
import zio.http.*
import zio.json.*

object ScoreRoutes {
  val routes = Routes(
    // GET /score is deprecated.
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
    // GET /score/:id is deprecated.
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
  )
}
