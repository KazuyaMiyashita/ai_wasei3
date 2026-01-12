package server

import zio.*
import zio.http.*
import java.io.File
import java.net.URLDecoder

object ResourcesRoutes {
  // Configurable root path
  // Priority:
  // 1. System Property "resources.root"
  // 2. Environment Variable "RESOUCES_ROOT"
  // 3. Default "data"
  lazy val RootPath: String =
    sys.props
      .get("resources.root")
      .orElse(sys.env.get("RESOUCES_ROOT"))
      .getOrElse("data")

  val routes = Routes(
    Method.GET / "resources" -> handler { (_: Request) =>
      handleResources(Nil)
    },
    Method.GET / "resources" / string("p1") -> handler { (p1: String, _: Request) =>
      handleResources(List(p1))
    },
    Method.GET / "resources" / string("p1") / string("p2") -> handler { (p1: String, p2: String, _: Request) =>
      handleResources(List(p1, p2))
    },
    Method.GET / "resources" / string("p1") / string("p2") / string("p3") -> handler {
      (p1: String, p2: String, p3: String, _: Request) =>
        handleResources(List(p1, p2, p3))
    },
    Method.GET / "resources" / string("p1") / string("p2") / string("p3") / string("p4") -> handler {
      (p1: String, p2: String, p3: String, p4: String, _: Request) =>
        handleResources(List(p1, p2, p3, p4))
    },
  ).handleError(e => Response.internalServerError(e.toString))

  private def handleResources(rawSegments: List[String]): ZIO[Any, Nothing, Response] = {
    // Decode segments just in case ZIO HTTP passes them raw
    val segments = rawSegments.map(s => URLDecoder.decode(s, "UTF-8"))

    // Prevent directory traversal
    if (segments.exists(s => s.contains("..") || s.contains("/") || s.contains("\\"))) {
      ZIO.succeed(Response.badRequest("Invalid path segments"))
    } else {
      val relativePath = if (segments.isEmpty) "" else segments.mkString("/", "/", "")

      // Determine if RootPath is absolute filesystem path or resource path
      val isAbsolutePath =
        RootPath.startsWith("/") || (java.lang.System.getProperty("os.name").toLowerCase.contains("win") && RootPath
          .contains(":"))

      if (isAbsolutePath) {
        val fullPath = if (segments.isEmpty) RootPath else s"$RootPath$relativePath"
        (handleFileProtocol(new File(fullPath), relativePath)).catchAll(e =>
          ZIO.succeed(Response.internalServerError(e.getMessage)),
        )
      } else {
        val fullResourcePath = if (segments.isEmpty) s"/$RootPath" else s"/$RootPath$relativePath"
        (Option(getClass.getResource(fullResourcePath)) match {
          case Some(url) =>
            if (url.getProtocol == "file") {
              handleFileProtocol(new File(url.toURI), relativePath)
            } else if (url.getProtocol == "jar") {
              handleJarProtocol(url, fullResourcePath, relativePath)
            } else {
              ZIO.succeed(Response.internalServerError("Unsupported protocol"))
            }
          case None =>
            ZIO.succeed(Response.notFound)
        }).catchAll(e => ZIO.succeed(Response.internalServerError(e.getMessage)))
      }
    }
  }

  private def handleFileProtocol(file: File, requestPath: String): ZIO[Any, Throwable, Response] = {
    ZIO.attempt {
      if (!file.exists()) {
        Response.notFound
      } else if (file.isDirectory) {
        val files = file.listFiles()
        if (files != null) {
          val sb = new StringBuilder
          sb.append(s"""<resource path="$requestPath">""")

          files
            .filterNot(_.getName.startsWith("."))
            .sortBy(_.getName)
            .foreach { f =>
              val name = f.getName
              val path = if (requestPath == "") s"/$name" else s"$requestPath/$name"
              if (f.isDirectory) {
                sb.append(s"""<directory name="$name" path="$path" />""")
              } else {
                val ext = name.split('.').lastOption.getOrElse("unknown")
                sb.append(s"""<file name="$name" path="$path" type="$ext" />""")
              }
            }
          sb.append("</resource>")
          Response.text(sb.toString).addHeader(Header.ContentType(MediaType.application.xml))
        } else {
          Response.internalServerError("Failed to list files")
        }
      } else {
        // Return file content
        val content   = scala.io.Source.fromFile(file)(using scala.io.Codec.UTF8).mkString
        val mediaType = determineMediaType(file.getName)
        Response.text(content).addHeader(Header.ContentType(mediaType))
      }
    }
  }

  private def handleJarProtocol(
      url: java.net.URL,
      resourcePath: String,
      requestPath: String,
  ): ZIO[Any, Throwable, Response] = {
    ZIO.attempt {
      val path    = url.getPath
      val jarPath = path.substring(5, path.indexOf("!"))
      val jar     = new java.util.jar.JarFile(URLDecoder.decode(jarPath, "UTF-8"))

      val entryPath = if (resourcePath.startsWith("/")) resourcePath.substring(1) else resourcePath

      val entry = jar.getJarEntry(entryPath)
      if (entry != null && !entry.isDirectory) {
        val stream  = jar.getInputStream(entry)
        val content = scala.io.Source.fromInputStream(stream)(using scala.io.Codec.UTF8).mkString
        jar.close()
        val mediaType = determineMediaType(entryPath)
        Response.text(content).addHeader(Header.ContentType(mediaType))
      } else {
        val prefix   = if (entryPath.endsWith("/")) entryPath else s"$entryPath/"
        val entries  = jar.entries()
        val children = scala.collection.mutable.Set[(String, String, Boolean)]()

        while (entries.hasMoreElements) {
          val e    = entries.nextElement()
          val name = e.getName
          if (name.startsWith(prefix) && name != prefix) {
            val relative = name.substring(prefix.length)
            if (!relative.startsWith(".")) {
              val slashIndex = relative.indexOf('/')
              if (slashIndex == -1) {
                // Direct child file
                children += ((relative, s"$requestPath/$relative", false))
              } else if (slashIndex == relative.length - 1) {
                // Direct child directory (ends with slash)
                val dirName = relative.substring(0, relative.length - 1)
                if (!dirName.startsWith(".")) {
                  children += ((dirName, s"$requestPath/$dirName", true))
                }
              } else {
                // Sub-child, implies directory exists
                val dirName = relative.substring(0, slashIndex)
                if (!dirName.startsWith(".")) {
                  children += ((dirName, s"$requestPath/$dirName", true))
                }
              }
            }
          }
        }
        jar.close()

        val sb = new StringBuilder
        sb.append(s"""<resource path="$requestPath">""")
        children.toList.sortBy(_._1).foreach { case (name, path, isDir) =>
          if (isDir) {
            sb.append(s"""<directory name="$name" path="$path" />""")
          } else {
            val ext = name.split('.').lastOption.getOrElse("unknown")
            sb.append(s"""<file name="$name" path="$path" type="$ext" />""")
          }
        }
        sb.append("</resource>")
        Response.text(sb.toString).addHeader(Header.ContentType(MediaType.application.xml))
      }
    }
  }

  private def determineMediaType(filename: String): MediaType = {
    if (filename.endsWith(".xml") || filename.endsWith(".mei")) MediaType.application.xml
    else if (filename.endsWith(".html")) MediaType.text.html
    else MediaType.text.plain
  }
}
