package ncreep

import java.net.URI
import akka.stream.scaladsl.{Flow, Source}
import akka.util.ByteString
import akka.{Done, NotUsed}
import ncreep.WikipediaAppCommand.{InvalidCommand, PerformCommand, Quit}
import ncreep.WikipediaAppResponse.{PerformedCommand, Quitting}
import ncreep.WikipediaCommand.{Browse, Find, Loop}
import scala.concurrent.Future

trait WikiRabbitHoleGraphInteractiveExplorerSolution extends WikiRabbitHoleGraphInteractiveExplorer with
                                                             WikiRabbitHoleExplorerSolution {
  override def cleanupInput: Flow[ByteString, String, NotUsed] =
    Flow[ByteString]
      .map(_.utf8String.trim)

  override def parseCommand(input: String): WikipediaAppCommand = {
    val parts = input.split(" ").toList

    parts match {
      case List("q") => Quit
      case List("browse", from) => PerformCommand(Browse(from))
      case List("loop", from) => PerformCommand(Loop(from))
      case List("find", from, to) => PerformCommand(Find(from = from, target = to))
      case _ => InvalidCommand
    }
  }

  override def parseCommands: Flow[String, WikipediaAppCommand, NotUsed] =
    Flow[String].map(parseCommand)

  override def performCommand(linksMap: LinksMap)
                             (command: WikipediaCommand): Future[URI] = {
    val (from, selector) = command match {
      case Browse(from) => (from, allChainsUnder8)
      case Loop(from) => (from, findLoops)
      case Find(from, target) => (from, findTargetThatContains(target))
    }

    runCrawlWithSummaries(linksMap)(from, selector)
      .map(GraphRenderer.render(_))
  }

  override def performCommands(linksMap: LinksMap): Flow[WikipediaAppCommand, WikipediaAppResponse, NotUsed] =
    Flow[WikipediaAppCommand].mapAsync(1) {
      case Quit => Future.successful(WikipediaAppResponse.Quitting)
      case PerformCommand(command) =>
        performCommand(linksMap)(command)
          .map(resultPath => PerformedCommand(command, resultPath))
      case InvalidCommand =>
        Future.successful(WikipediaAppResponse.ReceivedInvalidCommand)
    }

  override def setQuitCondition: Flow[WikipediaAppResponse, WikipediaAppResponse, NotUsed] =
    Flow[WikipediaAppResponse]
      .takeWhile(_ != Quitting, inclusive = true)

  override def renderResponse: Flow[WikipediaAppResponse, ByteString, NotUsed] =
    Flow[WikipediaAppResponse]
      .map(_.render)
      .map(ByteString.apply)

  override def interactionProtocol(linksMap: LinksMap): Flow[ByteString, ByteString, NotUsed] =
    cleanupInput
      .via(parseCommands)
      .via(performCommands(linksMap))
      .via(setQuitCondition)
      .via(renderResponse)
      .prepend(Source(List(initMessage)))

  override def runInteractive(linksMap: LinksMap): Future[Done] =
    stdinSource
      .via(interactionProtocol(linksMap))
      .runWith(stdoutSink)
}
