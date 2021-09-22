package forex

import cats.effect.{ Concurrent, Timer }
import cats.implicits.toSemigroupKOps
import forex.config.ApplicationConfig
import forex.http.rates.server.{ HealthCheckRoutes, RatesHttpRoutes }
import forex.services._
import forex.programs._
import org.http4s._
import org.http4s.implicits._
import org.http4s.server.middleware.{ AutoSlash, Timeout }

class Module[F[_]: Concurrent: Timer](config: ApplicationConfig, ratesService: RatesService[F]) {

  private val ratesProgram: RatesProgram[F] = RatesProgram[F](ratesService)

  private val ratesHttpRoutes
    : HttpRoutes[F] = new RatesHttpRoutes[F](ratesProgram).routes <+> new HealthCheckRoutes[F].routes

  type PartialMiddleware = HttpRoutes[F] => HttpRoutes[F]
  type TotalMiddleware   = HttpApp[F] => HttpApp[F]

  private val routesMiddleware: PartialMiddleware = {
    { http: HttpRoutes[F] =>
      AutoSlash(http)
    }
  }

  private val appMiddleware: TotalMiddleware = { http: HttpApp[F] =>
    Timeout(config.http.timeout)(http)
  }

  private val http: HttpRoutes[F] = ratesHttpRoutes

  val httpApp: HttpApp[F] = appMiddleware(routesMiddleware(http).orNotFound)

}
