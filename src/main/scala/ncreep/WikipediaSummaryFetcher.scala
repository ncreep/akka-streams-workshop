package ncreep

import ncreep.WikipediaModel.ExtractsResponse
import sttp.capabilities.akka.AkkaStreams
import sttp.client3._
import sttp.client3.ziojson.asJson
import sttp.model.Uri
import scala.concurrent.{ExecutionContext, Future}

/** Uses the Wikipedia API to fetch summaries for Wikipedia titles. */
trait WikipediaSummaryFetcher {
  /** Fetches the summary of the given title. */
  def fetchSummary(title: WikiTitle): Future[WikiSummary]

  /** Fetches the summaries for the given titles. The returned sequence is ordered
    * to match input sequence.
    */
  def fetchSummariesBatch(titles: Seq[WikiTitle]): Future[Seq[WikiSummary]]
}

object WikipediaSummaryFetcher {
  class Default(backend: SttpBackend[Future, AkkaStreams])
               (implicit ec: ExecutionContext) extends WikipediaSummaryFetcher {

    def fetchSummary(title: WikiTitle): Future[WikiSummary] =
      fetchRawSummaries(List(title)).map { summaries =>
        summaries.getOrElse(title, WikiSummary.empty)
      }

    def fetchSummariesBatch(titles: Seq[WikiTitle]): Future[Seq[WikiSummary]] =
      fetchRawSummaries(titles).map { summaries =>
        titles.map(title => summaries.getOrElse(title, WikiSummary.empty))
      }

    private def doRequest(url: Uri) = {
      basicRequest
        .get(url)
        .response(asJson[ExtractsResponse])
        .send(backend)
        .flatMap { resp =>
          Future.fromTry(resp.body.toTry)
        }
    }

    private def makeURL(titles: Seq[WikiTitle]) = {
      val query = Map(
        "format" -> "json",
        "action" -> "query",
        "prop" -> "extracts",
        "exintro" -> "true",
        "exsentences" -> "5",
        "titles" -> titles.map(_.value).mkString("|"))

      uri"https://en.wikipedia.org/w/api.php?$query"
    }

    private def fetchRawSummaries(titles: Seq[WikiTitle]): Future[Map[WikiTitle, WikiSummary]] =
    // this is not actually robust, if the number of titles will get too large this will probably
    // require doing paging over the response, left as an exercise to the reader
      doRequest(makeURL(titles))
        .map { response =>
          // since the response contains normalized titles, we have to do some juggling to
          // retrieve back the titles that we queried with
          val normalizedMap = response.query.normalized
            .getOrElse(List.empty)
            .map(n => n.to -> n.from).toMap

          response.query.pages.values.values
            .map { page =>
              val title = page.title
              val unnormalizedTitle = normalizedMap.getOrElse(title, title)

              val extract = page.extract

              val summary = extract.map(WikiSummary.apply).getOrElse(WikiSummary.empty)

              // since the query was performed with unnormalized titles, we return the result
              // with unnormalized titles as well
              WikiTitle(unnormalizedTitle) -> summary
            }.toMap
        }
  }

}
