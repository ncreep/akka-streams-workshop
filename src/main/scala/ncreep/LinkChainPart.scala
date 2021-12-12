package ncreep

/** A constituent part of a [[LinkChain]].
  * This usually represents a single link, though it might be missing a target
  * if we are at the end of the chain.
  *
  * @param root 'true' if this part represents the root of the chain it came from.
  */
final case class LinkChainPart(source: WikiTitle,
                               target: Option[LinkTarget],
                               root: Boolean) {
  def addSummary(summary: WikiSummary): LinkChainPart.WithSummary =
    LinkChainPart.WithSummary(this, summary)
}

object LinkChainPart {
  /** Wraps [[LinkChainPart]] with an additional [[WikiSummary]]. */
  final case class WithSummary(value: LinkChainPart, summary: WikiSummary) {
    /** Converts this link chain part into a single graph part. */
    def asGraphPiece: Graph = {
      import value._

      def toNode(raw: String, summary: WikiSummary, root: Boolean) =
        Node(id = raw, label = raw.replaceAll("_", " "), summary.value, root)

      val nodes = Set(toNode(source.value, summary, root))

      val edge = target.map { t =>
        Edge(source = source.value, target = t.value.value, weight = t.times.value)
      }.toSet

      Graph(nodes, edge)
    }
  }

  def apply(source: String, target: String, times: Int, root: Boolean): LinkChainPart =
    LinkChainPart(
      source = WikiTitle(source),
      target = Some(LinkTarget(target, times)),
      root = root)

  def apply(source: String, root: Boolean): LinkChainPart =
    LinkChainPart(
      source = WikiTitle(source),
      target = None,
      root = root)
}
