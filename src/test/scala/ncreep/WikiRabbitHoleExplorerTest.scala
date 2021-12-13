package ncreep

import java.util.concurrent.atomic.AtomicReference
import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, Source}
import akka.stream.testkit.scaladsl.TestSink
import ncreep.LinkChain.{build => lc}
import ncreep.TestData._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

/** These tests use the akka-streams testkit to set expectations on streams:
  * https://doc.akka.io/docs/akka/current/stream/stream-testkit.html
  */
class WikiRabbitHoleExplorerTest extends AsyncWordSpec with Matchers {
  private implicit val actorSystem: ActorSystem = ActorSystem("WikiRabbitHoleExplorerTest")
  // we are overriding the execution context from AsyncWordSpec as it only has a single thread
  // and may deadlock in some tests
  private implicit val testExecutionContext: ExecutionContext = actorSystem.dispatcher

  private val explorer = new TesterWikiRabbitHoleExplorer(
    new TestFileDownloader,
    new TestWikipediaSummaryFetcher)

  "Downloading the clicksrteam data" should {
    "download and decompress the stream" in {
      explorer
        .clickStreamData
        .map(_.utf8String)
        .runWith(TestSink.probe)
        .request(1)
        .expectNext(clickstreamRawData)
        .request(1).expectComplete()

      succeed // to keep the compiler happy, otherwise we cant use 'AsyncWordSpec'
    }

    "parse binary as CSV" in {
      explorer
        .clickStreamData
        .via(explorer.extractRowData)
        .runWith(TestSink.probe)
        .request(21)
        .expectNext(
          List("other-search", "Camp_Tawonga", "external", "183"),
          List("Groveland,_California", "Camp_Tawonga", "link", "14"),
          List("other-empty", "Camp_Tawonga", "external", "24"),
          List("Mount_Unzen", "1792_Unzen_earthquake_and_tsunami", "link", "280"),
          List("Tokugawa_Ienari", "1792_Unzen_earthquake_and_tsunami", "link", "14"),
          List("List_of_disasters_in_Japan_by_death_toll", "1792_Unzen_earthquake_and_tsunami", "link", "31"),
          List("other-internal", "1792_Unzen_earthquake_and_tsunami", "external", "55"),
          List("List_of_volcanic_eruptions_by_death_toll", "1792_Unzen_earthquake_and_tsunami", "link", "53"),
          List("other-search", "1792_Unzen_earthquake_and_tsunami", "external", "696"),
          List("1771_Great_Yaeyama_Tsunami", "1792_Unzen_earthquake_and_tsunami", "link", "11"),
          List("2018_Sunda_Strait_tsunami", "1792_Unzen_earthquake_and_tsunami", "link", "20"),
          List("List_of_earthquakes_in_Japan", "1792_Unzen_earthquake_and_tsunami", "link", "39"),
          List("Cumbre_Vieja_tsunami_hazard", "1792_Unzen_earthquake_and_tsunami", "link", "22"),
          List("Megatsunami", "1792_Unzen_earthquake_and_tsunami", "link", "98"),
          List("List_of_natural_disasters_by_death_toll", "1792_Unzen_earthquake_and_tsunami", "link", "40"),
          List("""something_"with_quotes"""", """"to_other_quotes""", "link", "930"),
          List("other-empty", "1792_Unzen_earthquake_and_tsunami", "external", "143"),
          List("other-search", "West_Bradford,_Lancashire", "external", "69"),
          List("other-empty", "West_Bradford,_Lancashire", "external", "18"),
          List("List_of_colleges_in_Mumbai", "Shri_M.D._Shah_Mahila_College_of_Arts_and_Commerce", "link", "25"),
          List("other-search", "Shri_M.D._Shah_Mahila_College_of_Arts_and_Commerce", "external", "320"))
        .request(1).expectComplete()

      succeed
    }

    "parse each CSV row into a link" in {
      explorer.parseLink(List("Mount_Unzen", "1792_Unzen_earthquake_and_tsunami", "link", "280")) shouldBe
        Some(Link("Mount_Unzen", "1792_Unzen_earthquake_and_tsunami", 280))

      explorer.parseLink(List("other-search", "Camp_Tawonga", "external", "183")) shouldBe None
      explorer.parseLink(List("other-empty", "Camp_Tawonga", "external", "24")) shouldBe None
      explorer.parseLink(List("other-internal", "1792_Unzen_earthquake_and_tsunami", "external", "55")) shouldBe None
      explorer.parseLink(List("some", "bad", "link", "not a number")) shouldBe None
    }

    "parse CSV as a flow" in {
      val csvLines = Source {
        List(
          List("Mount_Unzen", "1792_Unzen_earthquake_and_tsunami", "link", "280"),
          List("other-search", "Camp_Tawonga", "external", "183"),
          List("List_of_earthquakes_in_Japan", "1792_Unzen_earthquake_and_tsunami", "link", "39"),
          List("other-empty", "Camp_Tawonga", "external", "24"))
      }

      csvLines
        .via(explorer.parseLinks)
        .runWith(TestSink.probe)
        .request(2)
        .expectNext(
          Link("Mount_Unzen", "1792_Unzen_earthquake_and_tsunami", 280),
          Link("List_of_earthquakes_in_Japan", "1792_Unzen_earthquake_and_tsunami", 39))
        .request(1).expectComplete()

      succeed
    }

    "build a link-map from links" in {
      val links = Source(
        List(link1, link2, link3, link4))

      val eventualLinksMap =
        links.runWith(explorer.buildLinksMap)

      // this is a good candidate for property-based testing...
      val expected =
        LinksMap.empty
          .addLink(link1)
          .addLink(link2)
          .addLink(link3)
          .addLink(link4)

      eventualLinksMap.map { linksMap =>
        linksMap shouldBe expected
      }
    }

    "fetch and build a links map" in {
      val eventualLinksMap = explorer.fetchAndBuildLinksMap()

      val expected = LinksMap.empty
        .addLink(link1)
        .addLink(link2)
        .addLink(link3)
        .addLink(link4)
        .addLink(link5)
        .addLink(link6)
        .addLink(link7)
        .addLink(link8)
        .addLink(link9)
        .addLink(link10)
        .addLink(link11)
        .addLink(link12)
        .addLink(link13)

      eventualLinksMap.map { linksMap =>
        linksMap shouldBe expected
      }
    }
  }

  "Exploring link chains" should {
    def testExplore(explore: Flow[LinkChain, LinkChain, NotUsed]) =
      Source(linkChains)
        .via(explore)
        .runWith(TestSink.probe)

    "find all chains below 8" in {
      testExplore(explorer.allChainsUnder8)
        .request(34)
        .expectNext(
          chain1,
          chain21, chain22,
          chain31, chain32, chainLoop31,
          chain41, chain42, chain43, chain44, chainLoop41,
          chain51, chain52, chain53, chain54, chainLoop51,
          chain61, chain62, chain63, chain64, chain65, chain66, chain67, chain68, chainLoop61,
          chain71, chain72, chain73, chain74, chain75, chain76, chain77, chain78, chainLoop71)
        .request(1).expectComplete()

      succeed
    }

    "find all chains of size 7" in {
      testExplore(explorer.allChainsOfSize7)
        .request(9)
        .expectNext(chain71, chain72, chain73, chain74, chain75, chain76, chain77, chain78, chainLoop71)
        .request(1).expectComplete()

      succeed
    }

    "find the first 3 instances of a target in the chains" in {
      testExplore(explorer.findTargetThatContains("H")) // making sure that lower-casing is applied
        .request(3)
        .expectNext(chain81, chain82, chain83)
        .request(1).expectComplete()

      succeed
    }

    "find 5 loops" in {
      testExplore(explorer.findLoops)
        .request(5)
        .expectNext(chainLoop31, chainLoop41, chainLoop51, chainLoop61, chainLoop71)
        .request(1).expectComplete()

      succeed
    }

    "find the max total clicks in chains of size 7" in {
      testExplore(explorer.findMaxClickTotal)
        .request(1)
        .expectNext(chainLoop71)
        .request(1).expectComplete()

      succeed
    }
  }

  "The link crawler" should {
    "fetch the new link chains to visit" in {
      val linksMap = LinksMap.empty
        .addLink(Link(source = "d", target = "e1", times = 10))
        .addLink(Link(source = "d", target = "e2", times = 20))
        .addLink(Link(source = "d", target = "e3", times = 30))
        .addLink(Link(source = "d", target = "e4", times = 40))
        .addLink(Link(source = "d", target = "e5", times = 50))

      val chain = lc("a", 1 -> "b", 2 -> "c", 3 -> "d")
      val newLinks = explorer.getNewLinkChainsToVisit(linksMap)(chain)

      val expected = List(
        lc("a", 1 -> "b", 2 -> "c", 3 -> "d", 50 -> "e5"),
        lc("a", 1 -> "b", 2 -> "c", 3 -> "d", 40 -> "e4"),
        lc("a", 1 -> "b", 2 -> "c", 3 -> "d", 30 -> "e3"),
      )

      newLinks shouldBe expected
    }

    "get the previously unvisited links" in {
      val chain1 = lc("a", 1 -> "b", 2 -> "c", 3 -> "d")
      val chain2 = lc("a", 1 -> "b", 2 -> "c", 3 -> "dd")
      val chain3 = lc("a", 1 -> "b", 2 -> "cc")
      val chain4 = lc("aa")
      val chain5 = lc("a", 1 -> "b", 2 -> "c", 3 -> "d", 4 -> "e")

      val newLinkChains = List(chain1, chain2, chain3, chain4, chain5)
      val visited = Set("aa", "bb", "cc", "dd").map(WikiTitle)

      val unvisited = explorer.getPreviouslyUnvisited(visited)(newLinkChains)

      unvisited shouldBe List(chain1, chain5)
    }

    "get the titles to be visited" in {
      val unvisited = List(
        lc("a", 1 -> "b", 2 -> "c", 3 -> "d"),
        lc("a", 1 -> "b", 2 -> "c", 3 -> "d", 4 -> "e"),
        lc("a", 1 -> "b", 2 -> "f"),
      )

      val newlyVisited = explorer.getTitlesToBeVisited(unvisited)

      newlyVisited shouldBe List("d", "e", "f").map(WikiTitle)
    }

    "update the crawl state correctly" in {
      val chain1 = lc("a", 1 -> "b", 2 -> "c", 3 -> "d")
      val chain2 = lc("a", 1 -> "b", 2 -> "c", 3 -> "d", 4 -> "e")
      val chain3 = lc("a", 1 -> "b", 2 -> "g", 3 -> "h")
      val chain4 = lc("a", 1 -> "b", 2 -> "g", 4 -> "i", 40 -> "k")
      val chain5 = lc("a", 1 -> "b")

      val expected = LinkCrawlState(
        List(chain1, chain2, chain3, chain4, chain5),
        Set("a", "b", "k").map(WikiTitle))

      val newState = explorer.buildNewState(
        toVisitLater = List(chain1, chain2, chain3),
        visited = Set("a", "b").map(WikiTitle),
        previouslyUnvisited = List(chain4, chain5),
        titlesToBeVisited = List("k", "b").map(WikiTitle))

      newState shouldBe expected
    }

    "unfolding the state" should {
      "handle an empty state" in {
        val empty = LinkCrawlState(toVisit = List.empty, visited = Set.empty)

        explorer.unfolder(linksMap)(state = empty) shouldBe None
      }

      "unfold from the first step of the crawl" in {
        val firstStep = LinkCrawlState.initFrom(title = "a")

        val expectedChains = List(
          lc("a", 14 -> "d"),
          lc("a", 10 -> "b"),
          lc("a", 9 -> "e"))

        val expectedVisited = Set("b", "d", "e").map(WikiTitle)

        val newState = LinkCrawlState(toVisit = expectedChains, visited = expectedVisited)

        explorer.unfolder(linksMap)(firstStep) shouldBe Some((newState, expectedChains))
      }

      "produce the top 3 links for the next chain to be visited" in {
        val state1 = LinkCrawlState(
          toVisit = List(
            lc("a", 1 -> "h"),
            lc("a", 10 -> "b")),
          visited = Set("b", "h").map(WikiTitle))

        val state2 = LinkCrawlState(
          toVisit = List(
            lc("a", 10 -> "b"),
            lc("a", 1 -> "h", 6 -> "i4"),
            lc("a", 1 -> "h", 5 -> "i3"),
            lc("a", 1 -> "h", 4 -> "i2")),
          visited = Set("b", "h", "i2", "i3", "i4").map(WikiTitle))

        val output = List(
          lc("a", 1 -> "h", 6 -> "i4"),
          lc("a", 1 -> "h", 5 -> "i3"),
          lc("a", 1 -> "h", 4 -> "i2"))

        explorer.unfolder(linksMap)(state1) shouldBe Some((state2, output))
      }

      "produce nothing when the next chain has no further links" in {
        val state1 = LinkCrawlState(
          toVisit = List(lc("a", 14 -> "d")),
          visited = Set.empty)

        val state2 = LinkCrawlState(
          toVisit = List.empty,
          visited = Set.empty)

        val output = List.empty

        explorer.unfolder(linksMap)(state1) shouldBe Some((state2, output))
      }

      "not add new chains when the next step was already visited" in {
        val state1 = LinkCrawlState(
          toVisit = List(lc("a", 2 -> "c")), // 'c' points back to 'd'
          visited = Set("c", "d").map(WikiTitle))

        val state2 = LinkCrawlState(
          toVisit = List.empty,
          visited = Set("c", "d").map(WikiTitle))

        val output = List(lc("a", 2 -> "c", 11 -> "d"))

        explorer.unfolder(linksMap)(state1) shouldBe Some((state2, output))
      }

      "work when unfolding multiple steps" in {
        val state1 = LinkCrawlState(
          toVisit = List(
            lc("a", 14 -> "d"),
            lc("a", 10 -> "b"),
            lc("a", 9 -> "e")),
          visited = Set("b", "d", "e").map(WikiTitle))

        val state2 = LinkCrawlState(
          toVisit = List(
            lc("a", 10 -> "b"),
            lc("a", 9 -> "e")),
          visited = Set("b", "d", "e").map(WikiTitle))

        // 'd' doesn't have further links
        val output1 = List.empty

        explorer.unfolder(linksMap)(state1) shouldBe Some((state2, output1))

        val state3 = LinkCrawlState(
          toVisit = List(
            lc("a", 9 -> "e"),
            lc("a", 10 -> "b", 15 -> "f2"),
            lc("a", 10 -> "b", 5 -> "f1")),
          visited = Set("b", "d", "e", "f1", "f2").map(WikiTitle))

        val output2 = List(
          lc("a", 10 -> "b", 15 -> "f2"),
          lc("a", 10 -> "b", 5 -> "f1"),
          lc("a", 10 -> "b", 4 -> "d"))

        explorer.unfolder(linksMap)(state2) shouldBe Some((state3, output2))

        val state4 = LinkCrawlState(
          toVisit = List(
            lc("a", 10 -> "b", 15 -> "f2"),
            lc("a", 10 -> "b", 5 -> "f1"),
            lc("a", 9 -> "e", 19 -> "g1"),
            lc("a", 9 -> "e", 3 -> "g3")),
          visited = Set("b", "d", "e", "f1", "f2", "g1", "g3").map(WikiTitle))

        val output3 = List(
          lc("a", 9 -> "e", 19 -> "g1"),
          lc("a", 9 -> "e", 5 -> "f2"),
          lc("a", 9 -> "e", 3 -> "g3"))

        explorer.unfolder(linksMap)(state3) shouldBe Some((state4, output3))
      }

      "stop when there are no more links to visit" in {
        val state = LinkCrawlState(toVisit = List.empty, visited = Set("b", "d", "e").map(WikiTitle))

        explorer.unfolder(linksMap)(state) shouldBe None
      }
    }

    "crawling the links-map" should {
      "produce a breadth first traversal of top links starting from an existing title" in {
        explorer.crawlStream(linksMap)("a")
          .runWith(TestSink.probe)
          .request(9)
          .expectNext(
            lc("a", 14 -> "d"),
            lc("a", 10 -> "b"),
            lc("a", 9 -> "e"),

            lc("a", 10 -> "b", 15 -> "f2"),
            lc("a", 10 -> "b", 5 -> "f1"),
            lc("a", 10 -> "b", 4 -> "d"),

            lc("a", 9 -> "e", 19 -> "g1"),
            lc("a", 9 -> "e", 5 -> "f2"),
            lc("a", 9 -> "e", 3 -> "g3"))
          .request(1).expectComplete()

        succeed
      }

      "produce an empty stream when starting from a missing title" in {
        explorer.crawlStream(linksMap)("missing-title")
          .runWith(TestSink.probe)
          .expectSubscriptionAndComplete()

        succeed
      }
    }
  }

  "Converting link chains to graph pieces" should {
    "work per chain" in {
      val expectedGraph = Graph(
        Set(
          Node(id = "a", label = "a", info = "", root = true),
          Node(id = "b", label = "b", info = "", root = false),
          Node(id = "c", label = "c", info = "", root = false),
          Node(id = "d", label = "d", info = "", root = false)),
        Set(
          Edge(source = "a", target = "b", weight = 30),
          Edge(source = "b", target = "c", weight = 4),
          Edge(source = "c", target = "d", weight = 50)))

      Source(List(lc("a", 30 -> "b", 4 -> "c", 50 -> "d")))
        .via(explorer.chainToGraphPieces)
        .runWith(TestSink.probe)
        .request(1)
        .expectNext(expectedGraph)
        .request(1).expectComplete()

      succeed
    }

    "aggregate graphs into a sink" in {
      val expectedGraph = Graph(
        Set(
          Node(id = "a", label = "a", info = "", root = true),
          Node(id = "b", label = "b", info = "", root = false),
          Node(id = "c", label = "c", info = "", root = false),
          Node(id = "d", label = "d", info = "", root = false),
          Node(id = "e", label = "e", info = "", root = false)),
        Set(
          Edge(source = "a", target = "b", weight = 30),
          Edge(source = "b", target = "c", weight = 4),
          Edge(source = "c", target = "d", weight = 50),
          Edge(source = "a", target = "d", weight = 13),
          Edge(source = "d", target = "a", weight = 96),
          Edge(source = "a", target = "c", weight = 19),
          Edge(source = "c", target = "e", weight = 89),
          Edge(source = "e", target = "d", weight = 761)))

      val eventualGraph = Source(List(
        lc("a", 30 -> "b", 4 -> "c", 50 -> "d"),
        lc("a", 13 -> "d", 96 -> "a"),
        lc("a", 19 -> "c", 89 -> "e", 761 -> "d")))
        .via(explorer.chainToGraphPieces)
        .runWith(explorer.graphSink)

      eventualGraph.map { graph =>
        graph shouldBe expectedGraph
      }
    }
  }

  "Running a full crawl" should {
    "generate the crawl, explore it, and convert into a graph" in {
      val containsG = Flow[LinkChain].filter(_.last.value.contains("g"))

      val eventualGraph = explorer.runCrawl(linksMap)(from = "a", selector = containsG)

      val expectedGraph = Graph(
        Set(
          Node(id = "a", label = "a", info = "", root = true),
          Node(id = "e", label = "e", info = "", root = false),
          Node(id = "g1", label = "g1", info = "", root = false),
          Node(id = "g3", label = "g3", info = "", root = false)),
        Set(
          Edge(source = "a", target = "e", weight = 9),
          Edge(source = "e", target = "g1", weight = 19),
          Edge(source = "e", target = "g3", weight = 3)))

      eventualGraph.map { graph =>
        graph shouldBe expectedGraph
      }
    }
  }

  "Adding summaries to link chains" should {
    val p1 = LinkChainPart(source = "a", target = "b", times = 30, root = true)
    val p2 = LinkChainPart(source = "b", target = "c", times = 4, root = false)
    val p3 = LinkChainPart(source = "c", target = "d", times = 50, root = false)
    val p4 = LinkChainPart(source = "d", root = false)

    val p1WithSummary = p1.addSummary(WikiSummary("a-summary"))
    val p2WithSummary = p2.addSummary(WikiSummary("b-summary"))
    val p3WithSummary = p3.addSummary(WikiSummary("c-summary"))
    val p4WithSummary = p4.addSummary(WikiSummary("d-summary"))

    val parts = List(p1, p2, p3, p4)
    val partsWithSummaries = List(p1WithSummary, p2WithSummary, p3WithSummary, p4WithSummary)

    "break chains into parts" in {
      Source(List(lc("a", 30 -> "b", 4 -> "c", 50 -> "d")))
        .via(explorer.toLinkParts)
        .runWith(TestSink.probe)
        .request(4)
        .expectNextN(parts)
        .request(1).expectComplete()

      succeed
    }

    "add the summary per part" in {
      Source(parts)
        .via(explorer.addSummary)
        .runWith(TestSink.probe)
        .request(4)
        .expectNextN(partsWithSummaries)
        .request(1).expectComplete()

      succeed
    }

    "use batching" in {
      val summaryFetcher = new TestWikipediaSummaryFetcher

      val explorer = new TesterWikiRabbitHoleExplorer(
        new TestFileDownloader,
        summaryFetcher)

      Source(parts)
        .via(explorer.addSummaryBatched)
        .runWith(TestSink.probe)
        .request(4)
        .expectNextN(partsWithSummaries)
        .request(1).expectComplete()

      // making sure we made a single batched call
      summaryFetcher.getNumOfCalls shouldBe 1
    }

    "throttle the requests" in {
      import akka.stream.contrib.Implicits._

      // we're creating a number of messages large enough to trigger the throttle
      val throttleRateSize = 1 to 1000
      val messages1 = throttleRateSize.map(_ => p1)
      val messages2 = throttleRateSize.map(_ => p2)

      val results1 = throttleRateSize.map(_ => p1WithSummary)
      val results2 = throttleRateSize.map(_ => p2WithSummary)

      val timeToProcess = new AtomicReference(0.seconds)

      Source(messages1 ++ messages2)
        .via(explorer.addSummaryThrottled)
        .timed(identity, timeToProcess.set)
        .runWith(TestSink.probe)
        .request(throttleRateSize.size * 2)
        .expectNextN(results1 ++ results2)
        .request(1).expectComplete()

      timeToProcess.get should be > 1.second
    }

    "skip on exceptions" in {
      val explorer = new TesterWikiRabbitHoleExplorer(
        new TestFileDownloader,
        new TestWikipediaSummaryFetcher(throwOn = Set("b", "d")))

      val batchSize = 1 to 10
      // since exceptions are going to kill whole batches, we create enough messages to have more
      // than one batch
      val batch1 = batchSize.map(_ => p1)
      val batch2 = batchSize.map(_ => p2) // going to fall with an exception
      val batch3 = batchSize.map(_ => p3)
      val batch4 = batchSize.map(_ => p4) // same as above

      val result1 = batchSize.map(_ => p1WithSummary)
      val result2 = batchSize.map(_ => p3WithSummary)

      Source(batch1 ++ batch2 ++ batch3 ++ batch4)
        .via(explorer.addSummaryWithErrorHandling)
        .runWith(TestSink.probe)
        .request(batchSize.size)
        .request(batchSize.size)
        .expectNextN(result1)
        .expectNextN(result2)
        .request(1).expectComplete()

      succeed
    }

    val graph1 = Graph(
      Set(Node(id = "a", label = "a", info = "a-summary", root = true)),
      Set(Edge(source = "a", target = "b", weight = 30)))
    val graph2 = Graph(
      Set(Node(id = "b", label = "b", info = "b-summary", root = false)),
      Set(Edge(source = "b", target = "c", weight = 4)))
    val graph3 = Graph(
      Set(Node(id = "c", label = "c", info = "c-summary", root = false)),
      Set(Edge(source = "c", target = "d", weight = 50)))
    val graph4 = Graph(
      Set(Node(id = "d", label = "d", info = "d-summary", root = false)),
      Set.empty)

    "convert link chain pieces with summaries into graph pieces" in {
      Source(List(p1WithSummary, p2WithSummary, p3WithSummary, p4WithSummary))
        .via(explorer.chainPartsToGraphPieces)
        .runWith(TestSink.probe)
        .request(4)
        .expectNext(graph1, graph2, graph3, graph4)
        .request(1).expectComplete()

      succeed
    }

    "convert link chains into graphs with summaries" in {
      val graph1 = Graph(
        Set(Node(id = "a", label = "a", info = "a-summary", root = true)),
        Set(Edge(source = "a", target = "b", weight = 30)))
      val graph2 = Graph(
        Set(Node(id = "b", label = "b", info = "b-summary", root = false)),
        Set(Edge(source = "b", target = "c", weight = 4)))
      val graph3 = Graph(
        Set(Node(id = "c", label = "c", info = "c-summary", root = false)),
        Set(Edge(source = "c", target = "d", weight = 50)))
      val graph4 = Graph(
        Set(Node(id = "d", label = "d", info = "d-summary", root = false)),
        Set.empty)

      Source(List(lc("a", 30 -> "b", 4 -> "c", 50 -> "d")))
        .via(explorer.chainToGraphWithSummaries)
        .runWith(TestSink.probe)
        .request(4)
        .expectNext(graph1, graph2, graph3, graph4)
        .request(1).expectComplete()

      succeed
    }
  }

  "Crawling the links-map with summaries" should {
    "crawl the links and produce a graph with summaries" in {
      val containsG = Flow[LinkChain].filter(_.last.value.contains("g"))

      val eventualGraph = explorer.runCrawlWithSummaries(linksMap)(from = "a", selector = containsG)

      val expectedGraph = Graph(
        Set(
          Node(id = "a", label = "a", info = "a-summary", root = true),
          Node(id = "e", label = "e", info = "e-summary", root = false),
          Node(id = "g1", label = "g1", info = "g1-summary", root = false),
          Node(id = "g3", label = "g3", info = "g3-summary", root = false)),
        Set(
          Edge(source = "a", target = "e", weight = 9),
          Edge(source = "e", target = "g1", weight = 19),
          Edge(source = "e", target = "g3", weight = 3)))

      eventualGraph.map { graph =>
        graph shouldBe expectedGraph
      }
    }
  }
}


