package kanban.ucan

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Base64
import scala.util.{Failure, Success, Try}

class CidParser {
    // Multibase prefixes
    val Base32Prefix = 'b'
    val Base58Prefix = 'z'
    val Base64Prefix = 'm'

    // Multicodec codes
    val CodecRaw = 0x55
    val CodecDagCbor = 0x71
    val CodecDagJson = 0x0129

    // Multihash codes
    val Sha2_256 = 0x12
    val Sha2_512 = 0x13
    val Sha3_256 = 0x16
    val Sha3_512 = 0x14

    case class CID(version: Int, codec: Int, hashType: Int, hash: Array[Byte]) {
        def encode(): String = {
            val multicodecBytes = varintEncode(codec)
            val hashTypeBytes = varintEncode(hashType)
            val hashLenBytes = varintEncode(hash.length)

            val buffer = ByteBuffer.allocate(
              2 + multicodecBytes.length + hashTypeBytes.length +
                  hashLenBytes.length + hash.length
            )
            buffer.put(varintEncode(version))
            buffer.put(multicodecBytes)
            buffer.put(hashTypeBytes)
            buffer.put(hashLenBytes)
            buffer.put(hash)

            // encode with base32 as required by spec
            val encoded = Base32.encode(buffer.array())
            s"$Base32Prefix$encoded"
        }
    }

    // Parse a CID string into a CID object
    def parse(cidStr: String): Try[CID] = Try {
        if (cidStr.isEmpty) {
            throw new IllegalArgumentException("Empty CID string")
        }

        // Handle CIDv0 (base58btc encoded)
        if (cidStr.startsWith("Qm")) {
            val decoded = kanban.ucan.Base58.decode(cidStr)
            if (decoded.length != 34 || decoded(0) != 0x12 || decoded(1) != 0x20) {
                throw new IllegalArgumentException("Invalid CIDv0 format")
            }
            val hash = new Array[Byte](32)
            System.arraycopy(decoded, 2, hash, 0, 32)
            return Success(CID(0, CodecDagCbor, Sha2_256, hash))
        }

        // Handle CIDv1
        val multibasePrefix = cidStr.charAt(0)
        val content = cidStr.substring(1)

        val decoded = multibasePrefix match {
            case Base32Prefix => Base32.decode(content)
            case Base58Prefix => kanban.ucan.Base58.decode(content)
            case Base64Prefix => Base64.getDecoder.decode(content)
            case _            => throw new IllegalArgumentException(s"Unsupported multibase prefix: $multibasePrefix")
        }

        val buffer = ByteBuffer.wrap(decoded)

        val version = varintDecode(buffer)
        if (version != 1) {
            throw new IllegalArgumentException(s"Unsupported CID version: $version")
        }

        val codec = varintDecode(buffer)
        val hashType = varintDecode(buffer)
        val hashLength = varintDecode(buffer)

        val hash = new Array[Byte](hashLength)
        buffer.get(hash)

        CID(version, codec, hashType, hash)
    }

    // Create a new CID from raw bytes
    def create(version: Int, codec: Int, hashType: Int, data: Array[Byte]): CID = {
        val hash = hashType match {
            case Sha2_256 => MessageDigest.getInstance("SHA-256").digest(data)
            case Sha2_512 => MessageDigest.getInstance("SHA-512").digest(data)
            case Sha3_256 => MessageDigest.getInstance("SHA3-256").digest(data)
            case Sha3_512 => MessageDigest.getInstance("SHA3-512").digest(data)
            case _        => throw new IllegalArgumentException(s"Unsupported hash type: $hashType")
        }

        CID(version, codec, hashType, hash)
    }

    private def varintEncode(value: Int): Array[Byte] = {
        if (value < 0) {
            throw new IllegalArgumentException("Varint encoding requires non-negative values")
        }

        if (value < 128) {
            Array(value.toByte)
        } else {
            val buffer = new scala.collection.mutable.ArrayBuffer[Byte]()
            var v = value
            while (v >= 128) {
                buffer += ((v & 0x7f | 0x80).toByte)
                v >>= 7
            }
            buffer += v.toByte
            buffer.toArray
        }
    }

    private def varintDecode(buffer: ByteBuffer): Int = {
        var result = 0
        var shift = 0
        var b: Int = 0

        while ({
            if (shift >= 32) {
                throw new IllegalArgumentException("Varint is too long")
            }
            b = buffer.get() & 0xff
            result |= (b & 0x7f) << shift
            shift += 7
            (b & 0x80) != 0
        }) {}

        result
    }
}

object CidParser {
    def create(): CidParser = new CidParser()
}
