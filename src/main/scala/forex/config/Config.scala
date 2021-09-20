package forex.config

import cats.effect.{ Blocker, ContextShift, Sync }
import pureconfig.ConfigSource
import pureconfig.generic.auto._
import pureconfig.module.catseffect2.syntax._

object Config {

  /**
    * @param path the property path inside the default configuration
    */
  def load[F[_]: Sync: ContextShift](path: String, blocker: Blocker): F[ApplicationConfig] =
    ConfigSource.default.at(path).loadF[F, ApplicationConfig](blocker)

}
