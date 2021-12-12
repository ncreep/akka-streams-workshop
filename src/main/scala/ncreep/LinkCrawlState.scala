package ncreep

/** The state of a link crawling procedure.
  *
  * @param toVisit The links that still need to be crawled.
  * @param visited The titles that were already visited.
  */
final case class LinkCrawlState(toVisit: List[LinkChain], visited: Set[WikiTitle])

object LinkCrawlState {
  /** Creates a new [[LinkCrawlState]] that starts from the given Wikipediat title. */
  def initFrom(title: String): LinkCrawlState =
    LinkCrawlState(
      toVisit = List(LinkChain.startFrom(title)),
      visited = Set.empty)
}
