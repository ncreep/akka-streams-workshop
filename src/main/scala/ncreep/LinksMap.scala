package ncreep

import scala.collection.immutable.SortedSet

/** A map of links between Wikipedia titles. Allows fetching the
  * top clicked targets for an entry.
  *
  * Use [[LinksMap.empty]] to construct an empty [[LinksMap]] instance.
  */
trait LinksMap {
  /** Adding a single link into the map. */
  def addLink(link: Link): LinksMap

  /** Returns the top clicked links for the given title.
    *
    * @param top The number of link targets to return.
    */
  def getTopTargets(title: WikiTitle, top: Int): List[LinkTarget]
}

object LinksMap {
  val empty: LinksMap = Default(Map.empty)

  final case class Default(values: Map[WikiTitle, SortedSet[LinkTarget]]) extends LinksMap {
    /** Since we are interested in fetching the data in reverse order of the number of
      * clicks we create this reversed order here.
      */
    private implicit val linkTargetReverseOrdering: Ordering[LinkTarget] = {
      Ordering
        .by((_: LinkTarget).times.value).reverse
        .orElseBy(_.value.value)
    }

    def addLink(link: Link): LinksMap = {
      val oldLinks = values.getOrElse(link.source, SortedSet.empty)
      val newLinks = oldLinks + link.target

      Default(values.updated(link.source, newLinks))
    }

    def getTopTargets(title: WikiTitle, top: Int): List[LinkTarget] =
      values
        .getOrElse(title, SortedSet.empty)
        .take(top)
        .toList
  }
}
