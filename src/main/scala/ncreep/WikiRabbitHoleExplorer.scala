package ncreep

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.util.ByteString
import sttp.model.Uri
import scala.concurrent.{ExecutionContext, Future}

/** It's probably clear from the [[AkkaStreamsIntro]] file that I tend to get quite wordy. That's
  * because I like writing. If you find reading these walls of texts tiring, you can just skip the
  * comments, and jump right into the exercises: read the type signatures, check out the tests (code
  * speaks louder than words), and implement them. If you enjoy reading, great, you can hop on to
  * the next paragraph.
  *
  * In this workshop we are going to use Akka Streams to dive into "Wiki rabbit holes":
  * https://en.wikipedia.org/wiki/Wiki_rabbit_hole
  *
  * You might be familiar with this phenomenon when you go to Wikipedia to read up on avocados and
  * find yourself 3 hours later reading about the history of reintroduction of wolves in Yellowstone.
  * Trying to remember how you got there proves impossible as you clicked so many links on the way
  * there. You just fallen into a Wiki rabbit hole.
  *
  * What we'd like to find here are the common rabbit holes that people find themselves in. For this
  * purpose we'll use the Wikipedia clickstream:
  * https://meta.wikimedia.org/wiki/Research:Wikipedia_clickstream
  * https://wikimediafoundation.org/news/2018/01/16/wikipedia-rabbit-hole-clickstream/
  *
  * This is aggregated data that counts the number of times people clicked links in various
  * Wikipedia pages.
  *
  * Using the clickstream data we'll explore Wiki rabbit holes by doing the following:
  * 1. Start from some entry in Wikipedia
  * 2. Find the top 3 links that lead away from that entry
  * 3. Follow the links
  * 4. Repeat steps 1-3 as necessary
  * 5. Render the result in a visual graph
  *
  * By doing this we'll be exploring link chains in Wikipedia and gain some insight into possible
  * rabbit holes.
  *
  * We'll be using streaming techniques to solve various aspects of the logic described above.
  *
  * This is a follow-along file. The exercises for the workshop are stubs in the file. As you read
  * the instructions you'll have to implement the `???` stubs. Once you implemented a definition
  * you can try to execute the tests defined in the test suite for this trait ([[WikiRabbitHoleExplorerTest]]).
  *
  * When running, we will sometimes need to load a large file into memory. Make sure to allocate
  * enough memory in your JVM parameters, e.g., `-Xmx4g`.
  *
  * If you're stuck with any of the exercises you can take a look at the solutions for them on
  * the `solutions` branch. Search for filenames with the word "solution" in them.
  *
  * Here we go then.
  */
trait WikiRabbitHoleExplorer {
  /** To run the streams that we define we'll need an [[ActorSystem]] you can assume that it will
    * be provided for you, so no need to implement this member.
    */
  implicit def actorSystem: ActorSystem

  /** The execution-context will come in handy when we do some [[Future]] manipulation.
    * No need to implement this either, it will be provided later on.
    * (Note, since this is not a workshop about the proper handling of [[Future]]s we're not going
    * to delve into the intricacies of choosing the "right" execution context.)
    */
  implicit def executionContext: ExecutionContext

  /* ------------------------------------------------ */

  /** With the generalities out of the way, we can start with our first task: obtaining the
    * Wikipedia clickstream file that we'll be working on and converting it into an in-memory
    * data-structure that we can easily operate on, a [[LinksMap]].
    *
    * The clickstream data is provided in a large file, one per month. We'll be using the file
    * specified by this URL. There are newer files available by now, feel free to modify the URL
    * accordingly. Though do note that the exercises were tested on this specific file, and it's
    * not guaranteed that they'll work correctly with others.
    */
  def clickstreamDataUrl: Uri = {
    val clickstreamRoot = "https://dumps.wikimedia.org/other/clickstream"
    val clickstreamFile = "2021-06/clickstream-enwiki-2021-06.tsv.gz"

    // not using the interpolator as it does some unwanted escaping
    Uri.unsafeParse(s"$clickstreamRoot/$clickstreamFile")
  }

  /** The [[FileDownloader]] is utility that that we'll be using to download data. It's already
    * implemented and you can assume it will be provided for you.
    *
    * We'll be using [[FileDownloader.streamFileFromUrl]] to obtain a stream of raw bytes from the URL.
    */
  def fileDownloader: FileDownloader

  /** Here's the first task: use the [[FileDownloader]] to obtain a stream of bytes from the file.
    * The catch is that the file is gzipped, but we want a stream of bytes that are no longer zipped.
    *
    * As a general rule, Akka Streams has very good documentation, we'll be relying on it a lot
    * to solve the exercises of the workshop.
    * A good summary page for the different operations that Akka Streams supports is this:
    * https://doc.akka.io/docs/akka/current/stream/operators/index.html
    *
    * Use this page to find a way to decompress a stream.
    *
    * Hint: connecting a [[Flow]] to a [[Source]] produces a new [[Source]].
    *
    * Run the test suite in [[WikiRabbitHoleExplorerTest]] to make sure that you implemented this
    * correctly.
    */
  def clickStreamData: Source[ByteString, NotUsed] = ???

  /** Part of the benefit of using streams is that we can easily break up a large problem into small,
    * testable pieces. That's what we'll do here.
    * Now that we have a [[Source]] for the raw bytes of the clickstream file we want to convert it
    * into something more manageable.
    * If you actually download the file and look at its contents, you'll see that it's a
    * text file in a tab-delimited format. What we want now is a way to convert the raw bytes into
    * a stream of rows. Each row is a [[List]] of [[String]]. To achieve this we'll use a [[Flow]]
    * that takes in bytes and produces the rows.
    *
    * We could implement it ourselves, but Akka's Alpakka project provides many "connectors" that can
    * help working with various sources of data. In this case we'll use the Alpakka CSV connector.
    *
    * Use the documentation to find out how to use the CSV parser:
    * https://doc.akka.io/docs/alpakka/current/data-transformations/csv.html
    *
    * Note, due to some quirks in the file we're downloading, you'll have to set `quoteChar = -1`
    * when defining the CSV parser, otherwise some tests will fail.
    * Hint: you can convert a [[ByteString]] into a [[String]] using [[ByteString.utf8String]].
    * Hint 2: the `map` function will come in handy here both on streams and on lists.
    */
  def extractRowData: Flow[ByteString, List[String], NotUsed] = ???

  /** Now that we have the raw CSV data from the file, we'd like to parse it into values we can
    * conveniently use down the road.
    *
    * The format of the file we're working on looks something like this:
    * {{{
    * other-search	Camp_Tawonga	external	183
    * Groveland,_California	Camp_Tawonga	link	14
    * other-empty	Camp_Tawonga	external	24
    * Mount_Unzen	1792_Unzen_earthquake_and_tsunami	link	280
    * Tokugawa_Ienari	1792_Unzen_earthquake_and_tsunami	link	14
    * List_of_disasters_in_Japan_by_death_toll	1792_Unzen_earthquake_and_tsunami	link	31
    * other-internal	1792_Unzen_earthquake_and_tsunami	external	55
    * }}}
    *
    * Each row has 4 entries:
    * - source
    * - target
    * - type: "link", or "external", or "other"
    * - number of times the link was clicked
    *
    * We'll take each such row and try to convert it into a single [[Link]] value. A [[Link]] contains
    * the source of the click, the target of the click, and the number of times that combination happened.
    *
    * The tricky part is that not all link types are relevant to us. We only care about the "link"
    * type, we can ignore all the rest. The "link" type is the only one that actually corresponds
    * to clicks between entries in Wikipedia.
    *
    * You can see a more detailed description of the format here:
    * https://meta.wikimedia.org/wiki/Research:Wikipedia_clickstream#Format
    *
    * Write a function that converts a single row into a [[Link]] value. If the format doesn't
    * match the expected shape, or the "type" of the entry is not "link" - return [[None]].
    * To better understand how this function should behave, you can take a look at the tests.
    *
    * (Using [[None]] might not be good enough for "real" code, as we discard the reason for the
    * failure, but it's good enough for our purposes.)
    *
    * Hint: pattern-matching on [[List]] can be really handy here.
    */
  def parseLink(row: List[String]): Option[Link] = ???

  /** Now that we have a function that can parse a single row into a [[Link]] we want to integrate
    * it into a stream.
    * Create a [[Flow]] that takes in rows and produces [[Link]]s. If a row fails to parse, it
    * will be silently dropped from the stream.
    *
    * "Following" the types would be a good strategy here (and pretty much in all of the exercises).
    * What would happen if you `map` the [[parseLink]] function over a [[Flow]]? Is the type
    * correct? If not, what's missing?
    *
    * Hint: Take a look at the list of "simple operators" in Akka streams:
    * https://doc.akka.io/docs/akka/current/stream/operators/index.html#simple-operators
    * See whether you can find an operator that will solve this task (don't forget to click
    * the links to see some usage examples).
    */
  def parseLinks: Flow[List[String], Link, NotUsed] = ???

  /** Now that we have all the bits and pieces that will help us extract the data we need from the
    * file. We can move on to constructing an in-memory data-structure with the contents of the file.
    *
    * The data-structure we'll be building is a [[LinksMap]]. It has a very minimalistic API:
    * - You can create an empty [[LinksMap]] with [[LinksMap.empty]]
    * - You can add new [[Link]]s with [[LinksMap.addLink]]
    * - (For later) you can query a [[LinksMap]] with [[LinksMap.getTopTargets]] to find out
    * the top targets from a given [[WikiTitle]].
    *
    * We'll create a [[Sink]] that takes in the [[Link]]s we managed to produce in the previous
    * step and build a single [[LinksMap]] instance with all the [[Link]]s in it.
    *
    * Hint: take a look at the list of [[Sink]] operators here and check whether anything matches
    * our needs:
    * https://doc.akka.io/docs/akka/current/stream/operators/index.html#sink-operators
    *
    * Note, since everything we are doing happens in a streaming fashion, up until now we never
    * held the full file in-memory. Now we are leaving the streaming realm and actually accumulate
    * data in-memory. Unfortunately, at this point we have no choice.
    */
  def buildLinksMap: Sink[Link, Future[LinksMap]] = ???

  /** Now we have all the components to download a file, parse it, and a build a single [[LinksMap]]
    * from it.
    *
    * Use all the functions that you implemented so far to create a single flow starting from
    * downloading the file and ending with a fully built [[LinksMap]].
    * You can follow the same structure as you've seen in the [[AkkaStreamsIntro]] file.
    *
    * Don't forget to run the test suite to make sure that you implemented everything correctly up
    * until now.
    *
    * Hint: for debugging it might be quicker to avoid building the full map, but just take
    * some large chunk of the data. You can use the `take` function to do that, e.g.,:
    * {{{
    * .take(3000000)
    * }}}
    * Though the first time you run the function on real data, you should avoid `take` so that
    * the [[FileDownloader]] has a chance to download the full file and cache it.
    * You can also use `wireTap` with a `println` to output intermediate results to see that
    * everything is actually flowing.
    */
  def fetchAndBuildLinksMap(): Future[LinksMap] = ???

  /* You can now move on to the [[Playground]] file and try running your function
   * on real data. The [[Playground]] is equipped with all the real dependencies you need, so you can
   * just invoke [[fetchAndBuildLinksMap]] and see how it works.
   * Since the clickstream file is quite large during the first run it might take a while to
   * download it. You'll have to wait it out once, but since the file is cached locally, you won't
   * have to wait much in future invocations.
   *
   * Bonus: since we are using streaming, most of the code you wrote doesn't really care about the
   * actual source of the data. Suppose you want to read multiple clickstream files at once. How
   * will you do that? How much of the code you wrote will have to change?
   */

  /* ------------------------------------------------ */

  /** Well done! You've completed the first part of the workshop. Now that we have a way to obtain
    * all the click data that we need, we'll actually need to do something with it.
    *
    * So how will we explore all this click data?
    * Suppose we ask ourselves about Wiki rabbit holes that start from the term "Avocado".
    * We can start by querying the [[LinksMap]] for the term "Avocado" and get the top 3 links
    * that go from "Avocado" to somewhere else:
    * - Lauraceae
    * - Hass avocado
    * - Berry (botany)
    *
    * These are the top three links that people are most likely to click when browsing the "Avocado"
    * page. And these are our first steps down the rabbit hole.
    * We can go another step and see the top links from "Lauraceae":
    * - Laurus Nobilis
    * - Sassafras
    * - Cassytha
    * And the same from the other two links. This is our second step.
    * We can go further and further down, visiting more and more entries in Wikipedia. We are
    * essentially crawling a tree-like graph of links starting from the entry "Avocado" (not really
    * a tree as we can have loops in it). This happens to be a breadth-first traversal of the graph.
    *
    * Now, each path on that tree is a possible Wiki rabbit hole. For example, this chain of links:
    * {{{
    *   Avocado -705-> Lauraceae -247-> Laurus Nobilis -689-> Bay leaf -925-> Cinnamomum tamala
    * }}}
    * The numbers in-between the entries represent the number of times this link was clicked from
    * the previous entry. So "Lauraceae" was clicked 705 times from the "Avocado" page.
    *
    * In the code we are going to represent these chains with the [[LinkChain]] data structure.
    * Our goal would be to generate these [[LinkChain]]s as we crawl the Wikipedia graph in a
    * breadth-first manner and operate on them.
    *
    * Since the actual generation is going to be tricky, we'll start with a few exercises to get
    * a better feel for what we can do with [[LinkChain]]s.
    */

  /** Suppose you have a stream of [[LinkChain]]s, as they are produced from a breadth first traversal
    * of the links tree, so something like this:
    * {{{
    *   a
    *   a -> b1
    *   a -> b2
    *   a -> b1 -> c1
    *   a -> b1 -> c2
    *   a -> b2 -> d1
    *   a -> b2 -> d2
    *   ...
    * }}}
    *
    * As we go deeper, the chains become longer.
    * Create a [[Flow]] that only produces the chains whose length is < 8.
    * The stream should complete once we reach chains of size == 8.
    *
    * Don't forget to run the tests when implementing this flow.
    *
    * Hint: take a look at the "simple operators" in the docs:
    * https://doc.akka.io/docs/akka/current/stream/operators/index.html#simple-operators
    */
  def allChainsUnder8: Flow[LinkChain, LinkChain, NotUsed] = ???

  /** Now produce chains of exactly size == 7.
    * The stream should complete once we reach chains of size == 8.
    */
  def allChainsOfSize7: Flow[LinkChain, LinkChain, NotUsed] = ???

  /** Find the first 3 chains whose latest entry contains the given [[target]].
    *
    * Use lower-casing on both the [[target]] and the chain itself.
    *
    * This can be used, for example, to find the distance between "Avocado" and "Marie_Curie".
    */
  def findTargetThatContains(target: String): Flow[LinkChain, LinkChain, NotUsed] = ???

  /** A chain has a full loop if it starts and ends with the same Wikipedia title.
    * Use [[LinkChain.hasFullLoop]] to create a [[Flow]] that produces the first 5 loops.
    */
  def findLoops: Flow[LinkChain, LinkChain, NotUsed] = ???

  /** Of all the chains of exactly size 7, find the the chain that has the maximum total number
    * of clicks in it ([[LinkChain.totalClickTimes]]).
    *
    * Hint: you can reuse [[allChainsOfSize7]] here. Think of how you'd implement a function that
    * finds the maximum of a non-empty [[List]] (without using the `max` method).
    *
    * This one is a bit more difficult than the others.
    */
  def findMaxClickTotal: Flow[LinkChain, LinkChain, NotUsed] = ???

  /* ------------------------------------------------ */

  /** Hopefully you've gathered some intuition about [[LinkChain]]s, we can try to actually
    * produce them from a [[LinksMap]].
    *
    * This is probably the most difficult part of this workshop, don't worry if you're having trouble
    * following along. You can always take a look at the `solutions` branch of this repo if you
    * get stuck.
    */

  /** We are going to build the solution in a top-down manner.
    *
    * The main workhorse for this implementation is going to be `unfold`:
    * https://doc.akka.io/docs/akka/current/stream/operators/Source/unfold.html
    * Take your time familiarizing yourself with the documentation. Once you feel you understand how
    * `unfold` works you can try to implement [[crawlStream]].
    *
    * [[crawlStream]] takes an existing [[LinksMap]] and a Wikipedia title to start from. It uses
    * [[Source.unfold]] and [[unfolder]] to build stream of [[LinkChain]]s that explore the graph
    * of entries starting from the input title. Creating longer and longer [[LinkChain]]s on every
    * step.
    *
    * Assuming that [[unfolder]] is already implemented, implement [[crawlStream]].
    *
    * Hint: this is a very good case for "following the types". You can also check out the tests
    * to see how this function should work.
    * Hint 2: [[LinkCrawlState.initFrom]] will get you started with the unfolding process.
    * Hint 3: Consider adding a call to `wireTap` with a `println` so that you can observe the
    * progress of the unfolding process.
    */
  def crawlStream(linksMap: LinksMap)
                 (from: String): Source[LinkChain, NotUsed] = ???

  /** This is the core of the unfolding process. Given the current [[LinkCrawlState]] and a [[LinksMap]]
    * you need to produce some new [[LinkChain]]s to explore and update the state with the newly
    * visited [[WikiTitle]]s.
    *
    * This function is covered by lots of tests, take a look at them to try and understand
    * how it should work.
    *
    * Hint: the implementation of the more complex branch relies on the 4 functions below. Try
    * reading their signatures and descriptions and figure out how to use them.
    * Hint 2: the variable names in the pattern-match below are suggestive. At each invocation
    * of [[unfolder]] only `toVisitNow` should be actually visited.
    */
  def unfolder(linksMap: LinksMap)
              (state: LinkCrawlState): Option[(LinkCrawlState, List[LinkChain])] =
    state.toVisit match {
      case toVisitNow :: toVisitLater => ???
      case Nil => ???
    }

  /** Given a [[LinkChain]] find the top 3 targets for the latest entry in the chain
    * and return a new list of [[LinkChain]]s with these new targets added to the end of the given
    * chain.
    *
    * Hint: Use [[LinksMap.getTopTargets]] to fetch the top targets for an entry.
    */
  def getNewLinkChainsToVisit(linksMap: LinksMap)
                             (toVisitNow: LinkChain): List[LinkChain] = ???

  /** Given a list of [[LinkChain]]s return only those [[LinkChain]]s whose latest entries were
    * not yet visited.
    */
  def getPreviouslyUnvisited(visited: Set[WikiTitle])
                            (newLinkChains: List[LinkChain]): List[LinkChain] = ???

  /** Given a list of unvisited [[LinkChain]]s produce a list of the [[WikiTitle]]s that
    * correspond to their latest entries.
    */
  def getTitlesToBeVisited(unvisited: List[LinkChain]): List[WikiTitle] = ???

  /** Construct a new [[LinkCrawlState]] from the given inputs.
    *
    * @param toVisitLater        The [[LinkChain]]s that should be visited in a future step
    * @param visited             The [[WikiTitle]]s that were already visited.
    * @param previouslyUnvisited The newly generated [[LinkChain]]s that need to be visited in
    *                            a future step.
    * @param newVisited          [[WikiTitle]]s that were visited in the current step (and should
    *                            not be visited again)
    */
  def buildNewState(toVisitLater: List[LinkChain], visited: Set[WikiTitle],
                    previouslyUnvisited: List[LinkChain], titlesToBeVisited: List[WikiTitle]): LinkCrawlState = ???

  /* ------------------------------------------------ */

  /** Congrats for making it this far.
    *
    * Now that we are capable of crawling the clickstream data and generating Wiki rabbit holes,
    * it's time to have some fun.
    *
    * Our goal is to create a visual representation of the Wiki rabbit holes. For that we'll
    * create a [[Graph]] that we can visually render with the [[GraphRenderer]] utilities.
    *
    * Make a [[Flow]] that converts [[LinkChain]]s into [[Graph]] pieces.
    *
    * Hint: [[LinkChain.asGraphPiece]] will be useful here.
    */
  def chainToGraphPieces: Flow[LinkChain, Graph, NotUsed] = ???

  /** After converting each [[LinkChain]] in our crawl into a [[Graph]] piece, we want to
    * merge all the pieces into a single big [[Graph]]. Create a [[Sink]] that does that.
    *
    * Hint: the solution is quite similar to [[buildLinksMap]].
    */
  def graphSink: Sink[Graph, Future[Graph]] = ???

  /** And now we are ready to combine all the pieces together.
    *
    * Given an input Wikipedia title, crawl the [[LinksMap]], use the given [[Flow]] to select certain
    * outputs of the crawl, and then combine all the outputs into a single [[Graph]].
    *
    * When you're done head over to [[WikiRabbitHoleGraphApp]].
    */
  def runCrawl(linksMap: LinksMap)
              (from: String,
               selector: Flow[LinkChain, LinkChain, NotUsed]): Future[Graph] = ???

  /* ------------------------------------------------ */

  /** After all this effort, finally seeing all your work in an actual graph was hopefully
    * satisfying.
    * But, to make it extra interesting, it would be nice to have more than just Wikipedia titles
    * in the graph, so that we can quickly find out what the titles are about.
    *
    * For this purpose we'll be using a [[WikipediaSummaryFetcher]] that can query the Wikipedia
    * API and return a summary for a title.
    *
    * Assume that the fetcher will be provided for you when you run the final app.
    */
  def summaryFetcher: WikipediaSummaryFetcher

  /** Before we can use the summary fetcher, we first want to break apart our [[LinkChain]]s
    * into individual titles - [[LinkChainPart]]s. This way we can query each title separately.
    *
    * Create a [[Flow]] that does that.
    *
    * Hint: [[LinkChain.toParts]] will do most of the work for you.
    */
  def toLinkParts: Flow[LinkChain, LinkChainPart, NotUsed] = ???

  /** Now that we have a stream of [[LinkChainPart]]s we can add a summary to each one.
    * Since [[WikipediaSummaryFetcher]] uses [[Future]]s, the "asynchronous operators" will be
    * useful here:
    * https://doc.akka.io/docs/akka/current/stream/operators/index.html#asynchronous-operators
    *
    * These functions let you control how much parallelism to have in the stream. It can be very
    * important in general, but in this case you can just arbitrarily set it to 100 max concurrent
    * calls.
    *
    * Create a [[Flow]] that adds a summary to each incoming [[LinkChainPart]] using
    * [[WikipediaSummaryFetcher.fetchSummary]].
    *
    * Hint: make use of [[LinkChainPart.addSummary]].
    */
  def addSummary: Flow[LinkChainPart, LinkChainPart.WithSummary, NotUsed] = ???

  /** The Wikipedia API supports batching. So far we queried the API for one title each time.
    * Modify the [[addSummary]] [[Flow]] so that it takes advantage of batching using
    * [[WikipediaSummaryFetcher.fetchSummariesBatch]] to send batches of 10 items each time.
    *
    * Hint: you'll need one of the "simple operators" yet again:
    * https://doc.akka.io/docs/akka/current/stream/operators/index.html#simple-operators
    *
    * Bonus: sometimes it can take too long to wait for a full batch to arrive, so it makes
    * sense to wait for a batch for a certain maximum time. How would you implement that?
    */
  def addSummaryBatched: Flow[LinkChainPart, LinkChainPart.WithSummary, NotUsed] = ???

  /** Some APIs implement rate throttling - you can't query them too fast.
    * Assuming that this is the case with the Wikipedia API, we don't want to bombard it with
    * too many requests.
    *
    * Add throttling to your [[addSummaryBatched]] [[Flow]], so that you don't perform more than
    * a 1000 requests per second.
    *
    * Hint: the "simple operators" have a function for this as well...
    */
  def addSummaryThrottled: Flow[LinkChainPart, LinkChainPart.WithSummary, NotUsed] = ???

  /** API calls can sometimes be flaky and throw exceptions. If we don't handle them our stream
    * will crash.
    *
    * Add some basic error-handling to your [[addSummaryThrottled]] [[Flow]] so that it skips failed
    * calls.
    *
    * Hint: the documentation on the topic is very detailed:
    * https://doc.akka.io/docs/akka/current/stream/stream-error.html#supervision-strategies
    */
  def addSummaryWithErrorHandling: Flow[LinkChainPart, LinkChainPart.WithSummary, NotUsed] = ???

  /** Food for thought: notice how all the the `addSummary` function variants have the same
    * type-signature. This means that we could've added all of these features (batching, throttling,
    * error-handling) without our clients knowing anything about it.
    * A [[Flow]] is a very powerful building block.
    *
    * Bonus: since a typical crawl would contain many duplicates entries in its [[LinkChain]]s it
    * will be quite inefficient to query the API for the same title multiple times. Apart from
    * adding caching directly in [[WikipediaSummaryFetcher]], can you think of a way to avoid
    * duplicate calls by implementing deduplication directly in the stream? (Hint: it's a tricky
    * use case for [[Flow.scan]]).
    */

  /** To be able to render a graph with summaries, we need to convert our [[LinkChainPart.WithSummary]]
    * values into graph pieces. Implement a [[Flow]] that does that.
    *
    * Hint: this is very similar to [[chainToGraphPieces]].
    */
  def chainPartsToGraphPieces: Flow[LinkChainPart.WithSummary, Graph, NotUsed] = ???

  /** Combine the previous exercises to create a single [[Flow]] that takes in [[LinkChain]]s
    * and creates [[Graph]] pieces that have summaries.
    */
  def chainToGraphWithSummaries: Flow[LinkChain, Graph, NotUsed] = ???

  /** Now we can rewrite [[runCrawl]] so that it adds summaries to its [[LinkChain]].
    *
    * Copy the implementation of [[runCrawl]] and adapt it to use [[chainToGraphWithSummaries]].
    * After you're done go back to [[WikiRabbitHoleGraphApp]] and rerun it using
    * [[runCrawlWithSummaries]].
    */
  def runCrawlWithSummaries(linksMap: LinksMap)
                           (from: String,
                            selector: Flow[LinkChain, LinkChain, NotUsed]): Future[Graph] = ???

  /** And that's the very end of this workshop, congratulations on getting this far.
    * I hope that you gained some hands on experience with thinking in a stream-y way, and that
    * it will prove useful when solving problems in your own code.
    *
    * If you're feeling more adventurous and want to turn your app into something more interactive
    * you can jump to [[WikiRabbitHoleGraphInteractiveExplorer]] for the last bonus part of the
    * workshop.
    * Otherwise, feel free to extend the code with more functionality, solve the bonus questions
    * sprinkled in the comments, or to delve deeper into some of the implementation details
    * ([[FileDownloader]] is particularly tricky).
    */
}
