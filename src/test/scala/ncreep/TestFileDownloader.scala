package ncreep

import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import akka.NotUsed
import akka.stream.scaladsl.{Compression, Source, StreamConverters}
import akka.util.ByteString
import sttp.model.Uri

class TestFileDownloader extends FileDownloader {
  def streamFileFromUrl(url: Uri, localDirectory: Path): Source[ByteString, NotUsed] = {
    val inputStream = new ByteArrayInputStream(TestData.clickstreamRawData.getBytes(StandardCharsets.UTF_8))

    StreamConverters
      .fromInputStream(() => inputStream)
      .via(Compression.gzip)
      .mapMaterializedValue(_ => NotUsed)
  }
}
