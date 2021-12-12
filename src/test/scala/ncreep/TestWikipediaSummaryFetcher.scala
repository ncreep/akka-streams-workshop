package ncreep

import java.util.concurrent.atomic.AtomicInteger
import scala.concurrent.{ExecutionContext, Future}

class TestWikipediaSummaryFetcher(throwOn: Set[String] = Set.empty)
                                 (implicit executionContext: ExecutionContext) extends WikipediaSummaryFetcher {
  private val callsCounter = new AtomicInteger(0)

  def fetchSummary(title: WikiTitle): Future[WikiSummary] =
    fetchSummariesBatch(List(title))
      // the list can't actually be empty, but just in case...
      // this would be a good use-case for NonEmptyList
      .map(_.headOption.getOrElse(WikiSummary.empty))

  def fetchSummariesBatch(titles: Seq[WikiTitle]): Future[Seq[WikiSummary]] = {
    callsCounter.incrementAndGet()

    Future.traverse(titles) { title =>
      if (throwOn.contains(title.value)) Future.failed(new Exception("failed fetching summary"))
      else Future(WikiSummary(s"${title.value}-summary"))
    }
  }

  def getNumOfCalls: Int = callsCounter.get()
}
