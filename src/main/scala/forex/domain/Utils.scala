package forex.domain

import io.circe.generic.extras.decoding.{ EnumerationDecoder, UnwrappedDecoder }
import io.circe.generic.extras.encoding.{ EnumerationEncoder, UnwrappedEncoder }
import io.circe.{ Decoder, Encoder }
import org.http4s.circe.jsonEncoderOf
import org.http4s.EntityEncoder

object Utils {

  implicit def valueClassEncoder[A: UnwrappedEncoder]: Encoder[A] = implicitly
  implicit def valueClassDecoder[A: UnwrappedDecoder]: Decoder[A] = implicitly

  implicit def enumEncoder[A: EnumerationEncoder]: Encoder[A] = implicitly
  implicit def enumDecoder[A: EnumerationDecoder]: Decoder[A] = implicitly

  implicit def jsonEncoder[A <: Product: Encoder, F[_]]: EntityEncoder[F, A] = jsonEncoderOf[F, A]

}
