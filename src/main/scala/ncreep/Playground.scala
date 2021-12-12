package ncreep

import scala.concurrent.Future

/** A "playground" where you can play around with the various functions you're working on.
  * This object extends [[AppWithDeps]] so you just need to implement [[Playground.run]] to make
  * it executable. All the real dependencies (like [[FileDownloader]]) are provided via
  * [[AppWithDeps]].
  */
object Playground extends AppWithDeps with WikiRabbitHoleExplorer {
  def run(): Future[Unit] = ???
}
