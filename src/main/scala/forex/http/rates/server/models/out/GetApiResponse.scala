package forex.http.rates.server.models.out

import forex.domain.{ Currency, Price, Timestamp }
import forex.domain.Utils._
import forex.http.rates.server.models.JsonConfig._
import io.circe.Encoder
import io.circe.generic.extras.semiauto.deriveConfiguredEncoder

final case class GetApiResponse(
    from: Currency,
    to: Currency,
    price: Price,
    timestamp: Timestamp
)

object GetApiResponse {
  implicit val responseEncoder: Encoder[GetApiResponse] = deriveConfiguredEncoder[GetApiResponse]
}
