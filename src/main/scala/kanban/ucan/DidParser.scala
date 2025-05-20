package kanban.ucan

import java.lang.reflect.{Constructor, Modifier}
import java.math.BigInteger
import java.security.spec.{PKCS8EncodedKeySpec, X509EncodedKeySpec}
import java.security.{KeyFactory, KeyPairGenerator, PrivateKey, Signature}
import scala.util.{Failure, Success, Try}

class DidParser(val keyConstructors: (((Byte, Byte), Array[Byte]) => KeyMaterial)) {

    def parse(did: String): Try[KeyMaterial] = {
        if (!did.startsWith("did:key:z")) throw new IllegalArgumentException(s"Expected valid did:key, got: $did")
        val nonPrefixDid = did.substring(9)
        try {
            val bytes = Base58.decode(nonPrefixDid)
            val magicBytes = (bytes(0), bytes(1))
            val publicKey = bytes.drop(2)

            val keyMaterial = keyConstructors(magicBytes, publicKey)
            Success(keyMaterial)
        } catch {
            case _: Exception =>
                throw new IllegalArgumentException("Invalid DID encoding")
        }
    }

    def keyMaterialToDid(keyMaterial: KeyMaterial): String = {
        val bytes = Array(keyMaterial.magicBytes._1, keyMaterial.magicBytes._2) ++ keyMaterial.publicKey
        s"did:key:z${Base58.encode(bytes)}"
    }
}

object DidParser {
    def create(): DidParser = {
        new DidParser(DidParser.getKeyMaterial)
    }

    def getKeyMaterial(magicBytes: (Byte, Byte), publicKey: Array[Byte]): KeyMaterial = {
        val defaultConstructors = DidParser.getDefaultConstructors(publicKey).toMap

        defaultConstructors.get(magicBytes) match {
            case Some(keyMaterial) =>
                keyMaterial
            case None =>
                throw new IllegalArgumentException(s"Unrecognized magic bytes")
        }
    }

    def getDefaultConstructors(publicKey: Array[Byte]): Seq[((Byte, Byte), KeyMaterial)] = Seq(
      Ed25519KeyMaterial(Array.emptyByteArray, None).magicBytes -> Ed25519KeyMaterial(publicKey, None),
      RsaKeyMaterial(Array.emptyByteArray, None).magicBytes -> RsaKeyMaterial(publicKey, None),
      P256KeyMaterial(Array.emptyByteArray, None).magicBytes -> P256KeyMaterial(publicKey, None),
      Secp256k1KeyMaterial(Array.emptyByteArray, None).magicBytes -> Secp256k1KeyMaterial(publicKey, None),
      BLS12381G1KeyMaterial(Array.emptyByteArray, None).magicBytes -> BLS12381G1KeyMaterial(publicKey, None),
      BLS12381G2KeyMaterial(Array.emptyByteArray, None).magicBytes -> BLS12381G2KeyMaterial(publicKey, None)
    )
}
