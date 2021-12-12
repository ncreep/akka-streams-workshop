package ncreep

import scala.concurrent.Future

object WikiRabbitHoleGraphAppSolution extends AppWithDeps with
                                              WikiRabbitHoleExplorerSolution {
  def run(): Future[Unit] = for {
    linksMap <- fetchAndBuildLinksMap()
    graph <- runCrawl(linksMap)(from = "Avocado", selector = allChainsUnder8)
  } yield {
    GraphRenderer.renderAndLog(graph)

    ()
  }
}

object WikiRabbitHoleGraphWithSummariesAppSolution extends AppWithDeps with
                                                           WikiRabbitHoleExplorerSolution {
  def run(): Future[Unit] = for {
    linksMap <- fetchAndBuildLinksMap()
    graph <- runCrawlWithSummaries(linksMap)(from = "Avocado", selector = allChainsUnder8)
  } yield {
    GraphRenderer.renderAndLog(graph)

    ()
  }
}
