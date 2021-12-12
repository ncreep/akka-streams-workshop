package ncreep

import java.nio.file.{Files, Path, Paths}
import akka.NotUsed
import akka.stream.scaladsl.{Broadcast, FileIO, GraphDSL, Sink, Source}
import akka.stream.{Materializer, SourceShape}
import akka.util.ByteString
import ncreep.FileDownloader.tempDir
import org.fusesource.jansi.Ansi.ansi
import org.fusesource.jansi.AnsiConsole
import sttp.capabilities.akka.AkkaStreams
import sttp.client3._
import sttp.model.Uri
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

/** Provides the means to stream the contents of a single [[Uri]]. */
trait FileDownloader {
  /** Provides a [[Source]] with the bytes of the file that is located at the given URL.
    *
    * @param localDirectory A directory where the file being streamed may be cached. Defaults to the
    *                       system's temporary directory.
    */
  def streamFileFromUrl(url: Uri,
                        localDirectory: Path = tempDir): Source[ByteString, NotUsed]
}

object FileDownloader {
  private val tempDir: Path = Paths.get(System.getProperty("java.io.tmpdir"))

  /** The default implementation of a [[FileDownloader]]. Note that the implementation caches the
    * data it downloads in the directory provided to it (by default the OS temp directory).
    */
  class Default(backend: SttpBackend[Future, AkkaStreams])
               (implicit ec: ExecutionContext,
                mat: Materializer) extends FileDownloader {
    def streamFileFromUrl(url: Uri,
                          localDirectory: Path = tempDir): Source[ByteString, NotUsed] = {
      val filename = url.path.lastOption.getOrElse(url.toString)

      val downloadTarget = localDirectory.resolve(filename)

      streamURL(url, downloadTarget)
    }

    private def streamURL(url: Uri, downloadTarget: Path): Source[ByteString, NotUsed] = Source.futureSource {
      performRequest(url).map(streamResponse(downloadTarget))
    }.mapMaterializedValue(_ => NotUsed)

    private def streamResponse(downloadTarget: Path)
                              (response: Response[Source[ByteString, Any]]) = {
      val contentLength = response.contentLength
      val body: Source[ByteString, Any] = response.body

      if (isAlreadyCached(contentLength, downloadTarget)) streamFromCache(body, downloadTarget)
      else streamResponseContent(downloadTarget, contentLength, body)
    }

    private def streamResponseContent(downloadTarget: Path, contentLength: Option[Long], body: Source[ByteString, Any]) = {
      println("Downloading file, this may take a while...")
      println(s"Caching file to $downloadTarget")
      println("") // placeholder for the progress text later on

      val content = body.mapMaterializedValue(_ => NotUsed)
      val downloadStream = withProgressOutput(contentLength, content)

      // piping out the output of the download and also saving the result to a file
      // using a non-cancelling 'alsoTo' variant so that we stream the whole result
      // even if the consumers of this source decide to only use part of the stream
      nonCancellingAlsoTo(downloadStream, alsoTo = streamToPath(downloadTarget))
    }

    private def streamFromCache(responseBody: Source[ByteString, Any],
                                downloadTarget: Path) = {
      println(s"Skipping file download, using local version from $downloadTarget")
      Source.lazySource { () =>
        // we have to run the body streams, otherwise Akka will produce warnings and will backpressure the connection
        responseBody.runWith(Sink.cancelled)
        streamFromPath(downloadTarget)
      }
    }

    private def performRequest(url: Uri) =
      basicRequest
        .get(url)
        .response(asStreamAlwaysUnsafe(AkkaStreams))
        .send(backend)

    private def isAlreadyCached(responseContentLength: Option[Long], downloadTarget: Path) =
      Files.exists(downloadTarget) && responseContentLength.contains(Files.size(downloadTarget))

    private def withProgressOutput(contentLength: Option[Long],
                                   content: Source[ByteString, NotUsed]): Source[ByteString, NotUsed] = {
      def toMB(value: Long) = value / (1000 * 1000)

      val total = contentLength.map(l => s"/${toMB(l)}").getOrElse("")

      content.scan((0L, ByteString.empty)) { case ((bytesSoFar, _), curr) =>
        (bytesSoFar + curr.size, curr)
      }.map { case (bytesSoFar, bytes) =>
        val message = ansi()
          .cursorUp(1)
          .eraseLine()
          .a(s"Downloaded ${toMB(bytesSoFar)}$total mb")

        AnsiConsole.out().println(message)

        bytes
      }
    }

    private def streamFromPath(path: Path) =
      FileIO.fromPath(path)
        .mapMaterializedValue(_ => NotUsed)

    private def streamToPath(path: Path) =
      FileIO.toPath(path)
        .mapMaterializedValue { completion =>
          completion.recover {
            case NonFatal(_) =>
              println(s"Deleting incomplete file $path")
              // we don't want to keep leftover files in case of errors
              Files.delete(path)
          }

          NotUsed
        }

    /** Same as 'alsoTo' but without cancelling the diverted sink if the main flow is cancelled. */
    private def nonCancellingAlsoTo[A](source: Source[A, NotUsed],
                                       alsoTo: Sink[A, _]): Source[A, NotUsed] =
      Source.fromGraph {
        GraphDSL.create() { implicit b =>
          import GraphDSL.Implicits._

          val bcast = b.add(Broadcast[A](2, eagerCancel = false))

          source ~> bcast ~> alsoTo

          SourceShape(bcast.out(1))
        }
      }
  }
}
