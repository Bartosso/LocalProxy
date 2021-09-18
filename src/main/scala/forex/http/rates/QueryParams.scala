package forex.http.rates

import forex.domain.Currency
import org.http4s.{ ParseResult, QueryParamDecoder }
import org.http4s.dsl.impl.ValidatingQueryParamDecoderMatcher

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
