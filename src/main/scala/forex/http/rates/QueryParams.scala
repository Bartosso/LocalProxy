package forex.http.rates

import forex.domain.Currency
import org.http4s.{ ParseResult, QueryParamDecoder }
import org.http4s.dsl.impl.ValidatingQueryParamDecoderMatcher

object QueryParams {

  private[http] implicit val currencyQueryParam: QueryParamDecoder[Currency] =
    QueryParamDecoder[String].emap(
      string => ParseResult.fromTryCatchNonFatal("unknown currency")(Currency.withNameInsensitive(string))
    )

  object FromQueryParam extends ValidatingQueryParamDecoderMatcher[Currency]("from")
  object ToQueryParam extends ValidatingQueryParamDecoderMatcher[Currency]("to")

}
