package forex.config

import scala.concurrent.duration.FiniteDuration

case class ApplicationConfig(
    http: HttpConfig,
    clientConfig: ClientConfig,
    oneFrameConfig: OneFrameConfig
)

case class HttpConfig(
    host: String,
    port: Int,
    timeout: FiniteDuration
)

case class OneFrameConfig(
    cacheConfig: CacheConfig
)

case class CacheConfig(
    redisHost: Option[String],
    redisPort: Option[Int],
    ttl: FiniteDuration
)

case class ClientConfig(token: String,
                        targetHost: String,
                        targetPort: Int,
                        timeOut: FiniteDuration,
                        idleTimeout: FiniteDuration)
