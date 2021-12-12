package ncreep

/** A chain of links between titles in Wikipedia.
  * Each link in the chain includes the number of times that link was clicked
  * from its source.
  *
  * For example, if we are browsing from the title "Avocado", a chain can look
  * something like this:
  * {{{
  * Avocado -193-> Climacteric_(botany) -23-> Ethylene -196-> Acetylene -262-> Alkyne
  * }}}
  */
trait LinkChain {
  def size: Int

  /** The latest entry in this chain. */
  def last: WikiTitle

  /** Adding a new target to the end of this chain. */
  def addNextTarget(target: LinkTarget): LinkChain

  /** The total number of clicks contained in this chain. */
  def totalClickTimes: Int

  /** Returns 'true' if this chain contain a full loop. I.e., if the chain
    * starts and ends with the same title.
    */
  def hasFullLoop: Boolean

  /** Breaks up the chain into its constituent parts. */
  def toParts: List[LinkChainPart]

  /** Converts this chain into a single graph part. */
  def asGraphPiece: Graph
}

object LinkChain {
  /** Creates a new chain that start from the given Wikipedia title. */
  def startFrom(title: String): LinkChain = Default(WikiTitle(title), targets = List.empty)

  /** Provides simplified syntax for building up link chains. Useful for testing.
    * The usage of this function is meant to mimic the string representation of a link chain:
    * {{{
    *   build("a", 8 -> "b", 20 -> "c", 13 -> "d")
    * }}}
    */
  def build(startTitle: String, targets: (Int, String)*): LinkChain = {
    val typedTargets = targets.map { case (times, title) =>
      LinkTarget(WikiTitle(title), ClickTimes(times))
    }

    typedTargets.foldLeft(startFrom(startTitle))(_.addNextTarget(_))
  }

  /** The default implementation of a [[LinkChain]]. */
  final case class Default(start: WikiTitle, targets: List[LinkTarget]) extends LinkChain {
    val size: Int = targets.size + 1 // + 1 for the start node

    def last: WikiTitle = targets match {
      case head :: _ => head.value
      case Nil => start
    }

    def totalClickTimes: Int = targets.map(_.times.value).sum

    def addNextTarget(target: LinkTarget): LinkChain =
      this.copy(targets = target :: targets)

    def hasFullLoop: Boolean =
      targets.headOption.exists(_.value == start)

    def toParts: List[LinkChainPart] = {
      val targetsInOrder = targets.reverse

      val sources = start :: targetsInOrder.map(_.value)
      val allTargets = targetsInOrder.map(Some.apply) :+ None

      sources.zip(allTargets).map { case (source, target) =>
        LinkChainPart(source = source, target = target, root = source == start)
      }
    }

    def asGraphPiece: Graph = toParts
      .map(_.addSummary(WikiSummary.empty))
      .foldLeft(Graph.empty)(_ joinGraph _.asGraphPiece)

    override def toString: String = {
      toParts.map { st =>
        val arrow = st
          .target
          .map(t => s" -${t.times.value}-> ")
          .getOrElse("")

        s"${st.source.value}$arrow"
      }.mkString
    }
  }
}
