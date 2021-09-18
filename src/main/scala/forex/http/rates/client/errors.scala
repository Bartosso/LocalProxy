package forex.http.rates.client

object errors {
  sealed trait OneFrameHttpClientError

  case object NotFound extends OneFrameHttpClientError

  case object EmptyResponse extends OneFrameHttpClientError

  case object EndpointForbidden extends OneFrameHttpClientError

  final case class ErrorResponse(text: String) extends OneFrameHttpClientError

  final case class UnknownResponse(body: String) extends OneFrameHttpClientError
}
