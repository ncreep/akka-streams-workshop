package ncreep

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class LinksMapTest extends AnyWordSpec with Matchers {
  "The links map" should {
    "produce the top targets for added links" in {
      val linksMap = LinksMap.empty
        .addLink(Link("a", "b", 10))
        .addLink(Link("a", "c", 20))
        .addLink(Link("a", "d", 15))
        .addLink(Link("a", "e", 30))
        .addLink(Link("a", "f", 5))
        .addLink(Link("b", "c", 5))
        .addLink(Link("b", "d", 10))
        .addLink(Link("a", "g", 40))

      linksMap.getTopTargets(WikiTitle("a"), top = 3) shouldBe List(
        LinkTarget("g", 40),
        LinkTarget("e", 30),
        LinkTarget("c", 20))

      linksMap.getTopTargets(WikiTitle("b"), top = 3) shouldBe List(
        LinkTarget("d", 10),
        LinkTarget("c", 5))
    }
  }
}
