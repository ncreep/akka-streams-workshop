package ncreep

import scala.concurrent.Future

/** We are now finally ready to produce a bona fide visual thing.
  *
  * Use the [[WikiRabbitHoleExplorer]] to create an app that:
  * - downloads a clickstream file and builds a [[LinksMap]] ([[WikiRabbitHoleExplorer.fetchAndBuildLinksMap]])
  * - Builds a [[Graph]] that explores some Wiki rabbit holes ([[WikiRabbitHoleExplorer.runCrawl]]).
  * You can use the entry "Avocado" as a starting point.
  * - Render the [[Graph]] with [[GraphRenderer.renderAndLog]]
  *
  * Important: you have to use the exact Wikipedia title as it appears in the clickstream file for
  * things to work. That includes the correct case and using '_' instead of spaces.
  *
  * When you're done an HTML file name will be printed to the console, follow the link to view a
  * graph of the rabbit holes that you created.
  * The graph is interactive. You can zoom in on various parts, and hover the edges (and later
  * on on the nodes). The red dot marks the Wikipedia entry you started with. The edge thickness
  * represents the number of clicks between the entries. The more popular the link, the thicker
  * the edge.
  *
  * Warning: the graphs can get pretty heavy and take some time to load in the browser, if you
  * wait long enough they will eventually load (fingers crossed).
  *
  * Hint: for the `selector` parameter in the crawl you can use any of the flows you created in
  * the second part of the workshop (like [[WikiRabbitHoleExplorer.allChainsUnder8]]). Play around
  * with different selectors, see how the graphs change.
  */
object WikiRabbitHoleGraphApp extends AppWithDeps with
                                      WikiRabbitHoleExplorer {
  def run(): Future[Unit] = ???
}
