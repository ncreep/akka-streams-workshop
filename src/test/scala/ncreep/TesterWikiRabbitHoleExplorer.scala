package ncreep

import akka.actor.ActorSystem
import scala.concurrent.ExecutionContext

class TesterWikiRabbitHoleExplorer(val fileDownloader: FileDownloader,
                                   val summaryFetcher: WikipediaSummaryFetcher)
                                  (implicit val actorSystem: ActorSystem,
                                   val executionContext: ExecutionContext) extends WikiRabbitHoleExplorer

