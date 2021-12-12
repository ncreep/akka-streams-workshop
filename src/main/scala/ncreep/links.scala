package ncreep

/** A link between two Wikipedia titles: from [[source]] to [[target]].
  * The [[target]] includes the number of times that the target was clicked
  * from the current [[source]].
  */
final case class Link(source: WikiTitle, target: LinkTarget)

object Link {
  def apply(source: String, target: String, times: Int): Link =
    Link(WikiTitle(source), LinkTarget(WikiTitle(target), ClickTimes(times)))
}

/** The target of a link in Wikipedia.
  * That is, a title that is being clicked from another title.
  *
  * @param times The number of times the title was clicked.
  */
final case class LinkTarget(value: WikiTitle, times: ClickTimes)

object LinkTarget {
  def apply(value: String, times: Int): LinkTarget =
    LinkTarget(WikiTitle(value), ClickTimes(times))
}
