package ncreep

import akka.NotUsed
import akka.stream.alpakka.csv.scaladsl.CsvParsing
import akka.stream.scaladsl.{Compression, Flow, Sink, Source}
import akka.stream.{ActorAttributes, Supervision}
import akka.util.ByteString
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.control.NonFatal

trait WikiRabbitHoleExplorerSolution extends WikiRabbitHoleExplorer {
  override def clickStreamData: Source[ByteString, NotUsed] =
    fileDownloader.streamFileFromUrl(clickstreamDataUrl)
      .via(Compression.gunzip())

  override def extractRowData: Flow[ByteString, List[String], NotUsed] =
    CsvParsing.lineScanner(delimiter = CsvParsing.Tab, quoteChar = -1)
      .map(_.map(_.utf8String))

  override def parseLink(row: List[String]): Option[Link] =
  // not a nice way to parse as it suppresses all error information, but good enough for us
    row match {
      case List(source, target, "link", rawTimes) =>
        rawTimes.toIntOption.map { times =>
          Link(source, target, times)
        }
      case _ => None
    }

  override def parseLinks: Flow[List[String], Link, NotUsed] =
    Flow[List[String]]
      .mapConcat(parseLink)

  override def buildLinksMap: Sink[Link, Future[LinksMap]] =
    Sink.fold(LinksMap.empty)(_.addLink(_))

  override def fetchAndBuildLinksMap(): Future[LinksMap] =
    clickStreamData
      .via(extractRowData)
      .via(parseLinks)
      // For seeing progress when running
      //.wireTap(l => println(l))
      // For running quickly when playing around
      // .take(3000000)
      .runWith(buildLinksMap)

  /* ------------------------------------------------ */

  override def allChainsUnder8: Flow[LinkChain, LinkChain, NotUsed] =
    Flow[LinkChain]
      .takeWhile(_.size < 8)

  override def allChainsOfSize7: Flow[LinkChain, LinkChain, NotUsed] =
    Flow[LinkChain]
      .dropWhile(_.size < 7)
      .takeWhile(_.size < 8)

  override def findTargetThatContains(target: String): Flow[LinkChain, LinkChain, NotUsed] =
    Flow[LinkChain]
      .filter(_.last.value.toLowerCase.contains(target.toLowerCase))
      .take(3)

  override def findLoops: Flow[LinkChain, LinkChain, NotUsed] =
    Flow[LinkChain]
      .filter(_.hasFullLoop)
      .take(5)

  override def findMaxClickTotal: Flow[LinkChain, LinkChain, NotUsed] =
    allChainsOfSize7
      .reduce { (previousMax, curr) =>
        if (curr.totalClickTimes > previousMax.totalClickTimes) curr
        else previousMax
      }

  /* ------------------------------------------------ */

  override def crawlStream(linksMap: LinksMap)
                          (from: String): Source[LinkChain, NotUsed] =
    Source
      .unfold(LinkCrawlState.initFrom(from))(unfolder(linksMap))
      .mapConcat(identity)
      .wireTap(linkChain => println(linkChain)) // so that we can see progress while the app is running

  override def unfolder(linksMap: LinksMap)
                       (state: LinkCrawlState): Option[(LinkCrawlState, List[LinkChain])] =
    state.toVisit match {
      case toVisitNow :: toVisitLater =>
        Some {
          val visited = state.visited

          val newLinkChains = getNewLinkChainsToVisit(linksMap)(toVisitNow)

          val previouslyUnvisited = getPreviouslyUnvisited(visited)(newLinkChains)
          val titlesToBeVisited = getTitlesToBeVisited(previouslyUnvisited)

          val newState = buildNewState(
            toVisitLater = toVisitLater, visited = visited,
            previouslyUnvisited = previouslyUnvisited, titlesToBeVisited = titlesToBeVisited)

          (newState, newLinkChains)
        }
      case Nil => None
    }

  override def getNewLinkChainsToVisit(linksMap: LinksMap)
                                      (toVisitNow: LinkChain): List[LinkChain] =
    linksMap
      .getTopTargets(toVisitNow.last, top = 3)
      .map(toVisitNow.addNextTarget)

  override def getPreviouslyUnvisited(visited: Set[WikiTitle])
                                     (newLinkChains: List[LinkChain]): List[LinkChain] =
    newLinkChains.filterNot(link => visited.contains(link.last))

  override def getTitlesToBeVisited(unvisited: List[LinkChain]): List[WikiTitle] =
    unvisited.map(_.last)

  override def buildNewState(toVisitLater: List[LinkChain], visited: Set[WikiTitle],
                             previouslyUnvisited: List[LinkChain], titlesToBeVisited: List[WikiTitle]): LinkCrawlState =
    LinkCrawlState(
      toVisit = toVisitLater ++ previouslyUnvisited,
      visited = visited ++ titlesToBeVisited)

  /* ------------------------------------------------ */

  override def chainToGraphPieces: Flow[LinkChain, Graph, NotUsed] =
    Flow[LinkChain].map(_.asGraphPiece)

  override def graphSink: Sink[Graph, Future[Graph]] =
    Sink.fold(Graph.empty)(_ joinGraph _)

  /* ------------------------------------------------ */

  override def runCrawl(linksMap: LinksMap)
                       (from: String,
                        selector: Flow[LinkChain, LinkChain, NotUsed]): Future[Graph] = {
    crawlStream(linksMap)(from)
      .via(selector)
      .via(chainToGraphPieces)
      .runWith(graphSink)
  }

  /* ------------------------------------------------ */

  override def toLinkParts: Flow[LinkChain, LinkChainPart, NotUsed] =
    Flow[LinkChain]
      .map(_.toParts)
      .mapConcat(identity)

  override def addSummary: Flow[LinkChainPart, LinkChainPart.WithSummary, NotUsed] =
    Flow[LinkChainPart]
      .mapAsync(100) { linkChainPart =>
        summaryFetcher
          .fetchSummary(linkChainPart.source)
          .map { summary =>
            linkChainPart.addSummary(summary)
          }
      }

  override def addSummaryBatched: Flow[LinkChainPart, LinkChainPart.WithSummary, NotUsed] =
    Flow[LinkChainPart]
      .grouped(10)
      .mapAsync(100) { linkChainParts =>
        val titles = linkChainParts.map(_.source)

        summaryFetcher
          .fetchSummariesBatch(titles)
          .map { summaries =>
            linkChainParts.zip(summaries).map { case (lcp, summary) => lcp.addSummary(summary) }
          }
      }.mapConcat(identity)

  override def addSummaryThrottled: Flow[LinkChainPart, LinkChainPart.WithSummary, NotUsed] =
    Flow[LinkChainPart]
      .throttle(elements = 1000, per = 1.second)
      .via(addSummaryBatched)

  override def addSummaryWithErrorHandling: Flow[LinkChainPart, LinkChainPart.WithSummary, NotUsed] = {
    val decider: Supervision.Decider = {
      case NonFatal(_) => Supervision.Resume
      case _ => Supervision.Stop
    }

    addSummaryThrottled
      .withAttributes(ActorAttributes.supervisionStrategy(decider))
  }

  override def chainPartsToGraphPieces: Flow[LinkChainPart.WithSummary, Graph, NotUsed] =
    Flow[LinkChainPart.WithSummary].map(_.asGraphPiece)

  override def chainToGraphWithSummaries: Flow[LinkChain, Graph, NotUsed] =
    toLinkParts
      .via(addSummaryWithErrorHandling)
      .via(chainPartsToGraphPieces)

  /* ------------------------------------------------ */

  override def runCrawlWithSummaries(linksMap: LinksMap)
                                    (from: String,
                                     selector: Flow[LinkChain, LinkChain, NotUsed]): Future[Graph] =
    crawlStream(linksMap)(from)
      .via(selector)
      .via(chainToGraphWithSummaries)
      .runWith(graphSink)

}
