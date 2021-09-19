package forex.config

import cats.effect.{ Blocker, ContextShift, Resource, Sync }
import pureconfig.ConfigSource
import pureconfig.generic.auto._
import pureconfig.module.catseffect2.syntax._

object Config {

  /**
    * @param path the property path inside the default configuration
    */
  def resource[F[_]: Sync: ContextShift](path: String, blocker: Blocker): Resource[F, ApplicationConfig] =
    Resource.eval(ConfigSource.default.at(path).loadF[F, ApplicationConfig](blocker))

}
