package ncreep

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, Sink, Source}
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

/** This is the basic structure of a streaming application that we'll be using in this workshop.
  * There are many structures like it, but this one is ours.
  */
object AkkaStreamsIntro extends App {
  /** To be able to run an Akka Streams application we need an actor-system in scope
    * In the rest of the code for this workshop the actor-systems are going to be provided for you
    * and you won't have to deal with them.
    */
  implicit val actorSystem: ActorSystem = ActorSystem("BasicExample")

  /** The basic structure of streaming that we are going to be will have:
    * - A [[Source]] - to produce data
    * - Zero or more [[Flow]]s - to transform data
    * - A [[Sink]] - to consume data
    *
    * We join all these components into a single runnable streaming workflow.
    */

  /** This is a [[Source]] that will produce [[Int]]s when it will be run
    * The second type-parameter, known as the "materialized value", will be mostly ignored during this
    * workshop, hence it's set to [[NotUsed]].
    */
  val source: Source[Int, NotUsed] =
  // Here we are producing a fixed list of numbers, but in general a source can produce data from anywhere
  // E.g., files, URLs and the like
    Source(94 to 119)
      // we can apply collection-like operations on [[Source]]s
      .map(_ + 1)
      .filter(_ % 2 == 0)
      // but not all operations have collection equivalents
      .throttle(elements = 5, per = 1.second)

  /** This is a [[Flow]], it takes in [[Int]]s and produces [[Char]]s. Again, we ignore the last
    * type-parameter (the materialized value).
    * [[Flow]]s let us create values that represent various transformations over streams. Later these
    * can be applied to [[Source]]s or other [[Flow]]s to transform their output.
    */
  val flow1: Flow[Int, Char, NotUsed] =
    Flow[Int]
      // [[Flow]]s share many operations with [[Source]], so most things you can do on [[Source]]
      // you can do on [[Flow]] as well.
      .map(_ + 2)
      .map(_.toChar)
      // This lets us run an arbitrary action for each item in the stream without affecting the main
      // flow, can be useful for debugging
      .wireTap(c => println(c))
      .take(50)

  /** We can have multiple [[Flow]]s in a single streaming workflow. So here's another one.
    * This one takes in [[Char]] and produces [[String]].
    */
  val flow2: Flow[Char, String, NotUsed] =
    Flow[Char]
      .map(_.toUpper)
      .grouped(3)
      .map(_.mkString)

  /** This is a [[Sink]] that consumes [[String]]s, the materialized value here is a [[Future]] with
    * a list of strings in it.
    * Here we do care about the materialized value as this is what we get "out" of the whole stream.
    * In this case, the sink will aggregate all the elements that it receives into a single list.
    */
  val sink: Sink[String, Future[Seq[String]]] = Sink.seq[String]

  /** We are now ready to build our streaming workflow.
    * We do this by chaining all the components we have so far.
    */
  val eventualResult: Future[Seq[String]] =
    source
      // [[via]] is used to pipe the output of a [[Source]] into a [[Flow]]
      .via(flow1)
      .via(flow2)
      // we can now "run" the stream by connecting it to a [[Sink]] (this is not the only way to run
      // a stream, but it's the only one we'll use here)
      // After running, we are left with the materialized value of the [[Sink]]
      .runWith(sink)

  /** Since the result is a [[Future]] we [[Await]] before we can print it. Please avoid awaiting
    * in real code.
    */
  val result = Await.result(eventualResult, Duration.Inf)

  println(result) // Vector(BDF, HJL, NPR, TVX, Z)

  actorSystem.terminate()

  /** This was a very basic introduction to Akka Streams. For a more in-depth introduction, please
    * consult the docs:
    * https://doc.akka.io/docs/akka/current/stream/stream-introduction.html
    *
    * Well done, you're now ready to tackle Akka Streams on your own. Please proceed to
    * [[WikiRabbitHoleExplorer]].
    */
}
