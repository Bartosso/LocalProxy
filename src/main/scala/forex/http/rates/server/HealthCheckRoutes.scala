package forex.http.rates.server

import cats.effect.Sync
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router

class HealthCheckRoutes[F[_]: Sync] extends Http4sDsl[F] {

  private[http] val healthCheckPath = "/healthcheck"

  private val httpRoute = HttpRoutes.of[F] {
    case GET -> Root => Ok()
  }

  val routes: HttpRoutes[F] = Router(
    healthCheckPath -> httpRoute
  )

}
