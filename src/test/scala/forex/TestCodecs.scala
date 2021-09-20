package forex

import forex.http.rates.server.models.out.{ GetApiResponse, ParseCurrencyError }
import forex.http.rates.server.models.JsonConfig._
import forex.domain.Utils._
import io.circe.Decoder
import io.circe.generic.extras.semiauto.deriveConfiguredDecoder

object TestCodecs {
  implicit val unknownCurrencyDecoder: Decoder[ParseCurrencyError] = deriveConfiguredDecoder[ParseCurrencyError]
  implicit val responseDecoder: Decoder[GetApiResponse]            = deriveConfiguredDecoder[GetApiResponse]
}
