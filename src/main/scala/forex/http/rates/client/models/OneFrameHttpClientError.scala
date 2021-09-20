package forex.http.rates.client.models

import derevo.derive
import tofu.logging.derivation.loggable

@derive(loggable)
sealed trait OneFrameHttpClientError
object OneFrameHttpClientError {
  case object NotFound extends OneFrameHttpClientError

  case object EmptyResponse extends OneFrameHttpClientError

  case object EndpointForbidden extends OneFrameHttpClientError

  final case class ErrorResponse(text: String) extends OneFrameHttpClientError

  final case class UnknownResponse(body: String) extends OneFrameHttpClientError

  final case class ClientError(error: Throwable) extends OneFrameHttpClientError
}
