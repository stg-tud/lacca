// LEGACY CODE
package kanban.ucan

import java.util.Base64
import scala.collection.mutable.ArrayBuffer
import scala.util.{Failure, Success, Try}

// https://github.com/ChainAgnostic/varsig?tab=readme-ov-file#3-varsig-format
object Varsig {
    val Version = "0.1.0"
    val Prefix: Int = 0x34
    val RsaSignLength: Int = 0x100 // 256 Bytes

    val Base64Encoder = Base64.getEncoder().withoutPadding()
    val Base64Decoder = Base64.getDecoder()

    case class UnknownHeaderError(header: Array[Byte])
        extends Exception(s"Could not decode unknown header: ${header.map("0x" ++ "%02x".format(_)).mkString(", ")}")
    case class UnknownKeyTypeError(keyType: String)
        extends Exception(s"Could not encode unsupported key type: $keyType")

    sealed trait KeyType { def name: String }
    case object RS256 extends KeyType { val name = "RS256" }
    case object RS512 extends KeyType { val name = "RS512" }
    case object Ed25519 extends KeyType { val name = "Ed25519" }
    case object Secp256k1 extends KeyType { val name = "Secp256k1" }
    case object ES256 extends KeyType { val name = "ES256" }
    case object ES256K extends KeyType { val name = "ES256K" }
    case object ES512 extends KeyType { val name = "ES512" }

    private lazy val encodingMap: Map[KeyType, String] = Map(
      // RSA key types
      RS256 -> makeHeader(Prefix, Multicodec.RsaPub, Multicodec.Sha256, RsaSignLength, Multicodec.DagCbor),
      RS512 -> makeHeader(Prefix, Multicodec.RsaPub, Multicodec.Sha512, RsaSignLength, Multicodec.DagCbor),

      // Ed25519
      Ed25519 -> makeHeader(Prefix, Multicodec.Ed25519Pub, Multicodec.DagCbor),

      // Secp256k1
      Secp256k1 -> makeHeader(Prefix, Multicodec.Secp256k1Pub, Multicodec.Sha256, Multicodec.DagCbor),

      // ECDSA key types
      ES256 -> makeHeader(Prefix, Multicodec.Es256, Multicodec.Sha256, Multicodec.DagCbor),
      ES256K -> makeHeader(Prefix, Multicodec.Es256K, Multicodec.Sha256, Multicodec.DagCbor),
      ES512 -> makeHeader(Prefix, Multicodec.Es512, Multicodec.Sha512, Multicodec.DagCbor)
    )
    private lazy val decodingMap: Map[String, KeyType] = encodingMap.map(_.swap)

    def decode(header: Array[Byte]): Try[KeyType] = {
        val headerStr = Base64Encoder.encodeToString(header)
        decodingMap.get(headerStr).map(Success(_)).getOrElse(Failure(UnknownHeaderError(header)))
    }

    def encode(keyType: KeyType): Try[Array[Byte]] = {
        encodingMap
            .get(keyType)
            .map(h => Success(Base64Decoder.decode(h)))
            .getOrElse(Failure(UnknownKeyTypeError(keyType.name)))
    }

    private def makeHeader(vals: Int*): String = {
        val buffer = new ArrayBuffer[Byte]()

        vals.foreach { v =>
            buffer ++= encodeUvarint(v.toLong)
        }

        Base64Encoder.encodeToString(buffer.toArray)
    }

    // https://github.com/multiformats/unsigned-varint
    private def encodeUvarint(value: Long): Array[Byte] = {
        if (value == 0) return Array[Byte]()

        val buffer = new ArrayBuffer[Byte]()
        var v = value
        while (v >= 0x80) {
            buffer += ((v & 0x7f) | 0x80).toByte
            v >>= 7
        }
        buffer += v.toByte
        buffer.toArray
    }

    object Multicodec {
        // RSA
        val RsaPub: Int = 0x1205
        // Ed25519
        val Ed25519Pub: Int = 0xed
        // Secp256k1
        val Secp256k1Pub: Int = 0xe7
        // ECDSA / ES types
        val Es256: Int = 0x1200
        val Es256K: Int = 0x1201 // Added secp256k specific multicodec
        val Es512: Int = 0x1202
        // Hash algorithms
        val Sha256: Int = 0x12
        val Sha512: Int = 0x13
        // Encoding info (more here https://github.com/ChainAgnostic/varsig?tab=readme-ov-file#4-payload-encoding)
        val DagCbor: Int = 0x71
    }
}
