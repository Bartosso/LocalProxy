package forex.http.rates.client

import cats.data.NonEmptyList
import cats.effect.Sync
import cats.syntax.either._
import cats.syntax.functor._
import forex.config.ClientConfig
import forex.http.rates.client.errors.OneFrameRestClientErrors
import forex.http.rates.Utils._
import forex.http.rates.client.Protocol.In.GetCurrenciesValue
import forex.http.rates.client.Protocol.Out.GetCurrenciesRequest
import org.http4s.{ Header, Headers, Method, Request, Uri }
import org.http4s.client.Client

sealed trait OneFrameHttpClient[F[_]] {
  def getCurrenciesRates(in: GetCurrenciesRequest): F[OneFrameRestClientErrors Either NonEmptyList[GetCurrenciesValue]]
}

class OneFrameHttpClientImpl[F[_]: Sync](config: ClientConfig, client: Client[F]) extends OneFrameHttpClient[F] {
  private val targetUri = Uri.unsafeFromString(s"http://${config.targetHost}:${config.targetPort}/rates")
  private val headers   = Headers.of(Header("token", config.token))

  override def getCurrenciesRates(
      in: GetCurrenciesRequest
  ): F[Either[OneFrameRestClientErrors, NonEmptyList[GetCurrenciesValue]]] = {
    val uri     = targetUri.withQueryParam("pair", in.currencies.toList)
    val request = Request[F](Method.GET, uri, headers = headers)
    client.expect[NonEmptyList[GetCurrenciesValue]](request).map(_.asRight)
  }
}

object OneFrameHttpClientImpl {
  def apply[F[_]: Sync](config: ClientConfig, client: Client[F]): OneFrameHttpClientImpl[F] =
    new OneFrameHttpClientImpl(config, client)
}
