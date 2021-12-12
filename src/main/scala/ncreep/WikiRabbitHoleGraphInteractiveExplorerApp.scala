package ncreep

import scala.concurrent.Future

/** Now we can create the full Wiki rabbit hole explorer application.
  * Use [[WikiRabbitHoleExplorer.fetchAndBuildLinksMap]] along with
  * [[WikiRabbitHoleGraphInteractiveExplorer.runInteractive]] to create a single app
  * that loads all the Wikipedia clickstream and then listens to the user commands.
  *
  * Note, that it will take a bit of time for the [[LinksMap]] to be built, wait till you see
  * the initial message of the interactive protocol before you start providing commands.
  */
object WikiRabbitHoleGraphInteractiveExplorerApp extends AppWithDeps with
                                                         WikiRabbitHoleGraphInteractiveExplorer {
  def run(): Future[Unit] = ???
}
