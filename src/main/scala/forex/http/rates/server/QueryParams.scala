package forex.http.rates.server

import forex.domain.Currency
import org.http4s.dsl.impl.ValidatingQueryParamDecoderMatcher
import org.http4s.{ ParseResult, QueryParamDecoder }

object QueryParams {

  object Names {
    val FROM = "from"
    val TO   = "to"
  }

  private[http] implicit val currencyQueryParam: QueryParamDecoder[Currency] =
    QueryParamDecoder[String].emap(
      string => ParseResult.fromTryCatchNonFatal("unknown currency")(Currency.withNameInsensitive(string))
    )

  object FromQueryParam extends ValidatingQueryParamDecoderMatcher[Currency](Names.FROM)
  object ToQueryParam extends ValidatingQueryParamDecoderMatcher[Currency](Names.TO)

}
