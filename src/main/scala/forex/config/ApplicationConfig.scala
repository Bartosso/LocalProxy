package forex.config

import scala.concurrent.duration.FiniteDuration

case class ApplicationConfig(
    http: HttpConfig,
    clientConfig: ClientConfig,
    cacheConfig: CacheConfig
)

case class HttpConfig(
    host: String,
    port: Int,
    timeout: FiniteDuration
)

case class CacheConfig(
    redisHost: Option[String],
    redisPort: Option[Int],
    ttl: FiniteDuration
)

case class ClientConfig(token: String,
                        targetHost: String,
                        targetPort: Int,
                        timeout: FiniteDuration,
                        idleTimeout: FiniteDuration)
