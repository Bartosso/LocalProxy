package forex.services.rates.interpreters

import cats.Functor
import cats.syntax.functor._
import cats.data.NonEmptyList
import forex.domain.{ Price, Rate }
import forex.domain.Rate.Pair
import forex.http.rates.Protocol.Out.{ GetCurrenciesRequest, GetCurrencyValuePair }
import forex.http.rates.clients.OneFrameHttpClient
import forex.services.rates.errors.Error.OneFrameLookupFailed
import forex.services.rates.{ errors, Algebra }

class OneFrameImpl[F[_]: Functor](cli: OneFrameHttpClient[F]) extends Algebra[F] {
  override def get(pair: Rate.Pair): F[Either[errors.Error, Rate]] =
    cli
      .getCurrenciesRates(
        GetCurrenciesRequest(NonEmptyList(GetCurrencyValuePair(from = pair.from, to = pair.to), Nil))
      )
      .map(_.map { response =>
        val headValue = response.head
        val pair      = Pair(headValue.from, headValue.to)
        val price     = Price(headValue.price)
        Rate(pair, price, headValue.timeStamp)
      })
      .map(_.left.map(er => OneFrameLookupFailed(er.toString)))
}
