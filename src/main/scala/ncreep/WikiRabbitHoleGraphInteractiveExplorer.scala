package ncreep

import java.net.URI
import akka.stream.scaladsl._
import akka.util.ByteString
import akka.{Done, NotUsed}
import ncreep.WikipediaAppCommand._
import ncreep.WikipediaCommand._
import scala.concurrent.Future

/** So far we explored Wiki rabbit holes by hardcoding specific entries/selectors and running them.
  * Now we are going to add some interactivity. The aim of this part is to create an application
  * that listens to user input and based on the input creates different graphs.
  *
  * This part is not covered with tests like the rest of the workshop. So you're on your own now...
  * Though you can always take a look at the `solutions` branch.
  */
trait WikiRabbitHoleGraphInteractiveExplorer extends WikiRabbitHoleExplorer {
  /** One way of thinking about user interaction is as a stream of events where every element of
    * the stream corresponds to some user action. This way, an interactive application can be
    * implemented as a piece of stream-processing logic.
    *
    * For this to work, we need to convert the user events into a stream. Here we will be listening
    * to stdin and streaming the inputs as strings.
    * Each line the user enters will end up as a single [[ByteString]] in this [[Source]].
    *
    * Assume that this [[Source]] is provided for you.
    */
  def stdinSource: Source[ByteString, NotUsed]

  /** We are going to log back info to the user via a [[Sink]] that prints to stdout. Each command
    * that we will run will have an output, which will correspond to a single [[ByteString]] that
    * will go into the [[Sink]].
    *
    * Assume that this [[Sink]] is provided for you.
    */
  def stdoutSink: Sink[ByteString, Future[Done]]

  /** The input from the user is a [[ByteString]] and might contain redundant spaces.
    * Convert the raw inputs into trimmed [[String]]s.
    */
  def cleanupInput: Flow[ByteString, String, NotUsed] = ???

  /** For every input string we need to produce a single [[WikipediaAppCommand]].
    * We will support the following commands:
    * - q
    * - browse
    * - loop
    * - find
    *
    * The browse/loop/find commands take arguments that match the argument in [[WikipediaCommand]].
    * For example we can have the following commands:
    * - browse Avocado
    * - loop Avocado
    * - find Avocado Marie_Curie
    *
    * If the inputs don't match any of the available commands then an [[InvalidCommand]] value
    * should be produced.
    */
  def parseCommand(input: String): WikipediaAppCommand = ???

  /** Apply the [[parseCommand]] to a stream of inputs. */
  def parseCommands: Flow[String, WikipediaAppCommand, NotUsed] = ???

  /** Now that we have a [[WikipediaCommand]]s in hand, we can perform them.
    *
    * Assuming that we already have a [[LinksMap]] we can perform a command by running
    * [[runCrawlWithSummaries]] with the appropriate arguments then rendering the resulting [[Graph]]
    * using [[GraphRenderer.render]]. The result [[URI]] is the location of the rendered graph
    * as returned by [[GraphRenderer]].
    *
    * Each [[WikipediaCommand]] matches a single selector from [[WikiRabbitHoleExplorer]]:
    * - [[Browse]] is [[allChainsUnder8]]
    * - [[Loop]] is [[findLoops]]
    * - [[Find]] is [[findTargetThatContains]]
    *
    * Implement this method by executing the appropriate action for each [[WikipediaCommand]]
    * variant.
    */
  def performCommand(linksMap: LinksMap)
                    (command: WikipediaCommand): Future[URI] = ???

  /** Create a [[Flow]] that can execute all incoming [[WikipediaAppCommand]]s and produces the
    * appropriate [[WikipediaAppResponse]] for it.
    *
    * Note that quitting doesn't actually require anything, as the [[setQuitCondition]] will take
    * care of cutting off the stream and thus triggering the end of the application.
    */
  def performCommands(linksMap: LinksMap): Flow[WikipediaAppCommand, WikipediaAppResponse, NotUsed] = ???

  /** The application is going to be running until the stream we are defining completes.
    * Since we are reading from stdout, this means that as it stands, the application will
    * run indefinitely. We want to let the users quit the application when they are done.
    * When the user hits the letter 'q', this stream should complete.
    * Create a [[Flow]] that will cut off the stream when it encounters the [[Quitting]] response.
    * Note that the [[Quitting]] response should be the last element of the input, but it should be passed
    * on to the next stage (so that we can render it to the user when it arrives).
    *
    * Hint: the [[Flow.takeWhile]] function can be both exclusive and inclusive.
    */
  def setQuitCondition: Flow[WikipediaAppResponse, WikipediaAppResponse, NotUsed] = ???

  /** We want to render the [[WikipediaAppResponse]] that we got back to the user.
    * Use [[WikipediaAppResponse.render]] and convert the result into a single [[ByteString]].
    */
  def renderResponse: Flow[WikipediaAppResponse, ByteString, NotUsed] = ???

  /** This is the message that we are going to show to the user initially. We will need to
    * incorporate it into our interaction stream below.
    */
  def initMessage: ByteString = ByteString("Hi there, please enter a command, type 'q' to quit\n")

  /** Now that we have all the parts for the interaction protocol, we can assemble the parts
    * into a single [[Flow]].
    *
    * Use the previous parts to create a single [[Flow]] that:
    * - Cleans up the user input
    * - Parses the commands
    * - Executes them and produces a response
    * - Sets the quitting condition
    * - Renders the response into a [[ByteString]]
    * - Prepends the [[initMessage]] to the [[Flow]] so that it appears as the first output
    * of this [[Flow]].
    *
    * Food for thought: notice how decoupled this interaction protocol from any specific input
    * methods. This means that it's easier to test, and is more reusable in different contexts.
    */
  def interactionProtocol(linksMap: LinksMap): Flow[ByteString, ByteString, NotUsed] = ???

  /** Now we are ready to create our full interaction stream.
    * Connect the [[interactionProtocol]] to the [[stdinSource]] and run it into the [[stdoutSink]].
    *
    * Once you're done, move on to [[WikiRabbitHoleGraphInteractiveExplorerApp]].
    *
    * Bonus: suppose you wanted to run this application over TCP, how many changes would you
    * need to perform? Would the [[interactionProtocol]] need to change? Take a look at how Akka Streams
    * integrates with TCP communication:
    * https://doc.akka.io/docs/akka/current/stream/stream-io.html#streaming-tcp
    */
  def runInteractive(linksMap: LinksMap): Future[Done] = ???
}



