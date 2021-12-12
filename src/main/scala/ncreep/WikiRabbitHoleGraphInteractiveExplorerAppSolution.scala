package ncreep

import scala.concurrent.Future

object WikiRabbitHoleGraphInteractiveExplorerAppSolution extends AppWithDeps with
                                                                 WikiRabbitHoleGraphInteractiveExplorerSolution {
  def run(): Future[Unit] =
    for {
      linksMap <- fetchAndBuildLinksMap()
      _ <- runInteractive(linksMap)
    } yield ()
}
