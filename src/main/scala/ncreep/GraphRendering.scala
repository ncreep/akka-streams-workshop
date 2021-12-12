package ncreep

import java.net.URI
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path, Paths}
import zio.json._

/** Responsible for rendering a graph into an HTML file.
  * The HTML file uses Javascript to render the graph, which might take a while to load.
  */
object GraphRenderer {
  /** Rendering a graph and logging the location of the file it was written to. */
  def renderAndLog(graph: Graph, targetFile: Option[Path] = None): Unit = {
    val file = render(graph, targetFile)

    println("Written graph to file:")
    println(file)
  }

  /** Rendering the graph and returning the file it was rendered to. */
  def render(graph: Graph, targetFile: Option[Path] = None): URI = {
    // yes, this is still a streaming workshop, and no, this is not nice stream-y code...

    // not using `getClass.getClassLoader.getResource` as it's currently broken on
    // sbt, see:
    // https://stackoverflow.com/questions/55493096/listing-files-from-resource-directory-in-sbt-1-2-8
    val input = io.Source.fromInputStream(getClass.getResourceAsStream("/graph-template.html"))
    try {
      val template = input.mkString

      val graphJson = graph.toJson

      val replaced = template.replace("{{graphJson}}", graphJson)

      val file = targetFile.getOrElse(Files.createTempFile("wiki-graph", ".html"))

      Files.write(file, replaced.getBytes(StandardCharsets.UTF_8))

      file.toUri
    }
    finally {
      input.close()
    }
  }
}
