package forex.http.rates.clients

import cats.data.NonEmptyList
import cats.effect.Sync
import cats.syntax.either._
import cats.syntax.functor._
import forex.config.ClientConfig
import forex.http.rates.Protocol.In.GetCurrenciesValue
import forex.http.rates.Protocol.Out.GetCurrenciesRequest
import forex.http.rates.clients.errors.OneFrameRestClientErrors
import org.http4s.{ Header, Headers, Method, Request, Uri }
import org.http4s.client.Client

sealed trait OneFrameHttpClient[F[_]] {
  def getCurrenciesRates(in: GetCurrenciesRequest): F[OneFrameRestClientErrors Either NonEmptyList[GetCurrenciesValue]]
}

class OneFrameBlazeClientImpl[F[_]: Sync](config: ClientConfig, client: Client[F]) extends OneFrameHttpClient[F] {
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

object OneFrameHttpClient {
  def apply[F[_]: Sync](config: ClientConfig, client: Client[F]): OneFrameBlazeClientImpl[F] =
    new OneFrameBlazeClientImpl(config, client)
}
