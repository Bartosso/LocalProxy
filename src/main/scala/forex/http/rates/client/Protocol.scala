package forex.http.rates.client

import cats.data.NonEmptyList
import cats.syntax.functor._
import forex.domain.Rate.Pair
import forex.domain.{ CacheKey, Currency, Price, Rate, Timestamp }
import forex.domain.Utils._
import io.circe.Decoder
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto.deriveConfiguredDecoder
import org.http4s.{ QueryParamEncoder, QueryParameterValue }

object Protocol {

  implicit val configuration: Configuration = Configuration.default.withSnakeCaseMemberNames

  object Out {
    final case class GetCurrencyValuePair(from: Currency, to: Currency) {
      def toCacheKey: CacheKey = CacheKey(from, to)
    }
    final case class GetCurrenciesRequest(currencies: NonEmptyList[GetCurrencyValuePair])

    implicit val queryEncoder: QueryParamEncoder[GetCurrencyValuePair] = (value: GetCurrencyValuePair) =>
      QueryParameterValue(s"${value.from.entryName}${value.to.entryName}")
  }

  object In {

    sealed trait OneFrameResponse

    final case class GetCurrencyValue(from: Currency,
                                      to: Currency,
                                      bid: BigDecimal,
                                      ask: BigDecimal,
                                      price: Price,
                                      timeStamp: Timestamp) {
      def toCacheKey: CacheKey = CacheKey(from, to)
      def toRate: Rate = {
        val pair = Pair(from, to)
        Rate(pair, price, timeStamp)
      }
    }

    final case class GetCurrenciesSuccessfulResponse(in: List[GetCurrencyValue]) extends OneFrameResponse

    final case class ErrorJsonResponse(error: String) extends OneFrameResponse

    implicit val getCurrenciesValueDecoder: Decoder[GetCurrencyValue] =
      deriveConfiguredDecoder[GetCurrencyValue]

    implicit val errorResponseDecoder: Decoder[ErrorJsonResponse] =
      deriveConfiguredDecoder[ErrorJsonResponse]

    implicit val getCurrenciesSuccessfulResponse: Decoder[GetCurrenciesSuccessfulResponse] =
      Decoder[List[GetCurrencyValue]].map(GetCurrenciesSuccessfulResponse)

    implicit val oneFrameResponseDecoder: Decoder[OneFrameResponse] =
      getCurrenciesSuccessfulResponse.widen or errorResponseDecoder.widen

  }
}
