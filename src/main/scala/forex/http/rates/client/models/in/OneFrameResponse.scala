package forex.http.rates.client.models.in

import cats.syntax.functor._
import forex.domain.Utils._
import forex.http.rates.client.models.JsonConfig._
import io.circe.Decoder
import io.circe.generic.extras.semiauto.deriveConfiguredDecoder

sealed trait OneFrameResponse

final case class GetCurrenciesSuccessfulResponse(in: List[GetCurrencyValue]) extends OneFrameResponse

final case class ErrorJsonResponse(error: String) extends OneFrameResponse

object OneFrameResponse {
  implicit val errorResponseDecoder: Decoder[ErrorJsonResponse] =
    deriveConfiguredDecoder[ErrorJsonResponse]

  implicit val getCurrenciesSuccessfulResponse: Decoder[GetCurrenciesSuccessfulResponse] =
    Decoder[List[GetCurrencyValue]].map(GetCurrenciesSuccessfulResponse)

  implicit val oneFrameResponseDecoder: Decoder[OneFrameResponse] =
    getCurrenciesSuccessfulResponse.widen or errorResponseDecoder.widen
}
