package forex.http.rates.client.models.out

import forex.domain.{ CacheKey, Currency }
import org.http4s.{ QueryParamEncoder, QueryParameterValue }

final case class GetCurrencyValuePair(from: Currency, to: Currency) {
  def toCacheKey: CacheKey = CacheKey(from, to)
}

object GetCurrencyValuePair {
  implicit val queryEncoder: QueryParamEncoder[GetCurrencyValuePair] = (value: GetCurrencyValuePair) =>
    QueryParameterValue(s"${value.from.entryName}${value.to.entryName}")
}
