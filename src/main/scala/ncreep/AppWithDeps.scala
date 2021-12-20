package ncreep

import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.http.scaladsl.settings.ConnectionPoolSettings
import akka.stream.scaladsl.{Sink, Source, StreamConverters}
import akka.util.ByteString
import sttp.client3.akkahttp.AkkaHttpBackend
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}

/** A template for the apps to be implemented in the workshop.
  * Provides all the necessary dependencies as well as a hook to run an application that returns
  * a [[Future]].
  *
  * Implement the [[run]] method to have an executable application.
  */
abstract class AppWithDeps {
  implicit val actorSystem: ActorSystem = ActorSystem("WikiRabbitHole")
  // Using the dispatcher from the actor-system is probably not the best way to do things
  // but this is not a Futures workshop...
  implicit val executionContext: ExecutionContext = actorSystem.dispatcher

  private val sttpBackend = {
    val settings = ConnectionPoolSettings
      .default
      // we don't care about managing connections in this workshop...
      .withMaxConnections(1000000)

    AkkaHttpBackend.usingActorSystem(actorSystem, customConnectionPoolSettings = Some(settings))
  }


  private def terminate() = {
    actorSystem.terminate()
    sttpBackend.close()
  }

  val fileDownloader: FileDownloader = new FileDownloader.Default(sttpBackend)
  val summaryFetcher: WikipediaSummaryFetcher = new WikipediaSummaryFetcher.Default(sttpBackend)

  val stdinSource: Source[ByteString, NotUsed] =
    StreamConverters.fromInputStream(() => System.in).mapMaterializedValue(_ => NotUsed)
  val stdoutSink: Sink[ByteString, Future[Done]] =
    StreamConverters.fromOutputStream(() => System.out).mapMaterializedValue(_.map(_ => Done))

  def run(): Future[Unit]

  def main(args: Array[String]): Unit = {
    try {
      Await.result(run(), Duration.Inf)
    } finally {
      val _ = terminate()
    }
  }
}
