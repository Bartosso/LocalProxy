package forex

import cats.effect.Sync
import forex.http.rates.server.models.out.{ GetApiResponse, ParseCurrencyError }
import forex.http.rates.server.models.JsonConfig._
import forex.domain.Utils._
import io.circe.Decoder
import io.circe.generic.extras.semiauto.deriveConfiguredDecoder
import org.http4s.EntityDecoder
import org.http4s.circe.jsonOf

object TestCodecs {

  implicit def jsonDecoder[A <: Product: Decoder, F[_]: Sync]: EntityDecoder[F, A] = jsonOf[F, A]

  implicit val unknownCurrencyDecoder: Decoder[ParseCurrencyError] = deriveConfiguredDecoder[ParseCurrencyError]
  implicit val responseDecoder: Decoder[GetApiResponse]            = deriveConfiguredDecoder[GetApiResponse]
}
