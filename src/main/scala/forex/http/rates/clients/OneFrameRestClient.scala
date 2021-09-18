package forex.http.rates.clients

import cats.effect.Sync
import cats.syntax.either._
import cats.syntax.functor._
import forex.config.ClientConfig
import forex.http.rates.clients.Protocol.In.GetCurrenciesResponse
import forex.http.rates.clients.Protocol.Out.GetCurrenciesRequest
import forex.http.rates.clients.errors.OneFrameRestClientErrors
import org.http4s.{ Header, Headers, Method, Request, Uri }
import org.http4s.client.Client

sealed trait OneFrameRestClient[F[_]] {
  def getCurrenciesRates(in: GetCurrenciesRequest): F[OneFrameRestClientErrors Either GetCurrenciesResponse]
}

class OneFrameBlazeClientImpl[F[_]: Sync](config: ClientConfig, client: Client[F]) extends OneFrameRestClient[F] {
  private val targetUri = Uri.unsafeFromString(s"http://${config.targetHost}:${config.targetHost}/rates")
  private val headers   = Headers.of(Header("token", config.token))

  override def getCurrenciesRates(
      in: GetCurrenciesRequest
  ): F[Either[OneFrameRestClientErrors, GetCurrenciesResponse]] = {
    val uri     = targetUri.withQueryParam("pair", in.currencies.toList)
    val request = Request[F](Method.GET, uri, headers = headers)
    client.expect[GetCurrenciesResponse](request).map(_.asRight)
  }
}

object OneFrameRestClient {}
