package ncreep

/** A title in Wikipedia, should be possible to use without any further
  * processing of the raw string.
  */
final case class WikiTitle(value: String)

/** A summary of an entry in Wikipedia. */
final case class WikiSummary(value: String)

/** The number of times a title in Wikipedia was clicked. */
final case class ClickTimes(value: Int)

object WikiSummary {
  val empty: WikiSummary = WikiSummary("")
}
