package ncreep

import zio.json.{DeriveJsonDecoder, JsonDecoder}

/** A bunch of case classes and JSON decoders to model a response from the Wikipedia extracts API:
  * https://www.mediawiki.org/w/api.php?action=help&modules=query%2Bextracts
  * https://www.mediawiki.org/wiki/API:Query
  */
object WikipediaModel {
  final case class ExtractsResponse(query: Query)
  final case class Query(pages: Pages, normalized: Option[List[Normalized]])
  final case class Pages(values: Map[String, Page])
  /** Information about how a title that was queried was normalized in the response. */
  final case class Normalized(from: String, to: String)
  final case class Page(title: String, extract: Option[String])

  object Query {
    implicit val decoder: JsonDecoder[Query] = DeriveJsonDecoder.gen[Query]
  }

  object Page {
    implicit val decoder: JsonDecoder[Page] = DeriveJsonDecoder.gen[Page]
  }

  object Normalized {
    implicit val decoder: JsonDecoder[Normalized] = DeriveJsonDecoder.gen[Normalized]
  }

  object Pages {
    implicit val decoder: JsonDecoder[Pages] = JsonDecoder[Map[String, Page]].map(Pages.apply)
  }

  object ExtractsResponse {
    implicit val decoder: JsonDecoder[ExtractsResponse] = DeriveJsonDecoder.gen[ExtractsResponse]
  }
}
