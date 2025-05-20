package kanban.ucan

import org.scalajs.dom.KeyAlgorithmIdentifier

//import java.security.{KeyFactory, PrivateKey, Signature}
//import java.security.spec.{PKCS8EncodedKeySpec, X509EncodedKeySpec}
import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.typedarray.*
import scala.concurrent.ExecutionContext.Implicits.global
import typings.std.global.crypto

sealed trait KeyMaterial {
  val publicKey: Array[Byte]
  val privateKey: Option[Array[Byte]]
  val name: String
  val magicBytes: (Byte, Byte)
  val jwtAlg: String

  def create(): KeyMaterial
  def sign(payload: Array[Byte]): Array[Byte]
  def verify(payload: Array[Byte], signature: Array[Byte]): Boolean
}

case class Ed25519KeyMaterial(
    override val publicKey: Array[Byte],
    override val privateKey: Option[Array[Byte]]
) extends KeyMaterial {
  val name = "Ed25519"
  val magicBytes = (0xed.toByte, 0x01.toByte)
  val jwtAlg = "EdDSA"

  //TODO: SHOULD RETURN A FUTURE
  def create(): Future[KeyMaterial] = scala.concurrent.Await
    .result(Ed25519KeyMaterial.create(), scala.concurrent.duration.Duration.Inf)
  def sign(payload: Array[Byte]): Array[Byte] =
    Ed25519KeyMaterial.sign(this, payload)
  def verify(payload: Array[Byte], signature: Array[Byte]): Boolean =
    Ed25519KeyMaterial.verify(publicKey, payload, signature)
}

case class RsaKeyMaterial(
    override val publicKey: Array[Byte],
    override val privateKey: Option[Array[Byte]]
) extends KeyMaterial {
  val name = "RSA"
  val magicBytes = (0x85.toByte, 0x24.toByte)
  val jwtAlg = "RS256"

  def create(): KeyMaterial = RsaKeyMaterial(publicKey, privateKey)
  def sign(payload: Array[Byte]): Array[Byte] = Array.emptyByteArray
  def verify(payload: Array[Byte], signature: Array[Byte]): Boolean = false
}

case class P256KeyMaterial(
    override val publicKey: Array[Byte],
    override val privateKey: Option[Array[Byte]]
) extends KeyMaterial {
  val name = "P256"
  val magicBytes = (0x80.toByte, 0x24.toByte)
  val jwtAlg = "ES256"

  def create(): KeyMaterial = P256KeyMaterial(publicKey, privateKey)
  def sign(payload: Array[Byte]): Array[Byte] = Array.emptyByteArray
  def verify(payload: Array[Byte], signature: Array[Byte]): Boolean = false
}

case class Secp256k1KeyMaterial(
    override val publicKey: Array[Byte],
    override val privateKey: Option[Array[Byte]]
) extends KeyMaterial {
  val name = "SECP256K1"
  val magicBytes = (0xe7.toByte, 0x01.toByte)
  val jwtAlg = "ES256K"

  def create(): KeyMaterial = Secp256k1KeyMaterial(publicKey, privateKey)
  def sign(payload: Array[Byte]): Array[Byte] = Array.emptyByteArray
  def verify(payload: Array[Byte], signature: Array[Byte]): Boolean = false
}

case class BLS12381G1KeyMaterial(
    override val publicKey: Array[Byte],
    override val privateKey: Option[Array[Byte]]
) extends KeyMaterial {
  val name = "BLS12381G1"
  val magicBytes = (0xea.toByte, 0x01.toByte)
  val jwtAlg = "BLS"

  def create(): KeyMaterial = BLS12381G1KeyMaterial(publicKey, privateKey)
  def sign(payload: Array[Byte]): Array[Byte] = Array.emptyByteArray
  def verify(payload: Array[Byte], signature: Array[Byte]): Boolean = false
}

case class BLS12381G2KeyMaterial(
    override val publicKey: Array[Byte],
    override val privateKey: Option[Array[Byte]]
) extends KeyMaterial {
  val name = "BLS12381G2"
  val magicBytes = (0xeb.toByte, 0x01.toByte)
  val jwtAlg = "BLS"

  def create(): KeyMaterial = BLS12381G2KeyMaterial(publicKey, privateKey)
  def sign(payload: Array[Byte]): Array[Byte] = Array.emptyByteArray
  def verify(payload: Array[Byte], signature: Array[Byte]): Boolean = false
}

object Ed25519KeyMaterial {
//    def create(): Ed25519KeyMaterial = {
//      // TODO: USE SUBTLE CRYTPO INSTEAD OF JAVA CRYPTO
//      val algo = js.Dynamic.literal(
//        name = "Ed25519"
//      )
//      val keyPairGenerator = KeyPairGenerator.getInstance("Ed25519")
//      val keyPair = keyPairGenerator.generateKeyPair()
//
//      val publicKey = keyPair.getPublic.getEncoded
//      val privateKey = Option(keyPair.getPrivate.getEncoded)
//
//        Ed25519KeyMaterial(publicKey, privateKey)
//    }
  def create(): Future[Ed25519KeyMaterial] = {
    val algo: KeyAlgorithmIdentifier = "Ed25519"
    val extractable = true
    val keyUsages =
      js.Array(org.scalajs.dom.KeyUsage.sign, org.scalajs.dom.KeyUsage.verify)
    val subtle = crypto.subtle

    val promise = subtle
      .generateKey(algo, extractable, keyUsages)
      .asInstanceOf[js.Promise[js.Dynamic]]

    promise.toFuture.flatMap { keyPair =>
      val publicKeyPromise = subtle.exportKey(
        org.scalajs.dom.KeyFormat.raw,
        keyPair
          .asInstanceOf[js.Dynamic]
          .selectDynamic("publicKey")
          .asInstanceOf[org.scalajs.dom.CryptoKey]
      )
      val privateKeyPromise = subtle
        .exportKey(
          org.scalajs.dom.KeyFormat.pkcs8,
          keyPair
            .asInstanceOf[js.Dynamic]
            .selectDynamic("privateKey")
            .asInstanceOf[org.scalajs.dom.CryptoKey]
        )
        .asInstanceOf[js.Promise[ArrayBuffer]]

    for {
      pub <- publicKeyPromise.toFuture
      priv <- privateKeyPromise.toFuture
    } yield {
      Ed25519KeyMaterial(
        new Int8Array(pub.asInstanceOf[ArrayBuffer]).toArray,
        Some(new Int8Array(priv).toArray)
      )
    }
    }
    // println(s"Generated Ed25519 key pair:\nPublic Key: ${new Int8Array(pub).toArray.mkString(",")}\nPrivate Key: ${new Int8Array(priv).toArray.mkString(",")}")
  }

//    def sign(keyMaterial: Ed25519KeyMaterial, payload: Array[Byte]): Array[Byte] = {
//        val privateKeyBytes =
//            keyMaterial.privateKey.getOrElse(throw new IllegalArgumentException("Private key is required for signing"))
//        val keyFactory = KeyFactory.getInstance("Ed25519")
//        val privateKeySpec = new PKCS8EncodedKeySpec(privateKeyBytes)
//        val privateKey: PrivateKey = keyFactory.generatePrivate(privateKeySpec)
//
//        val signature = Signature.getInstance("Ed25519")
//        signature.initSign(privateKey)
//        signature.update(payload)
//        signature.sign()
//    }

  def sign(
      keyMaterial: Ed25519KeyMaterial,
      payload: Array[Byte]
  ): Future[Array[Byte]] = {
    val privateKeyBytes =
      keyMaterial.privateKey.getOrElse(
        throw new IllegalArgumentException(
          "Private key is required for signing"
        )
      )
    val algo = js.Dynamic.literal(name = "Ed25519")
    val extractable = false
    val keyUsages = js.Array("sign")

    val importKeyPromise = crypto.subtle
      .importKey(
        format = "pkcs8",
        // keyData = Int8Array.from(privateKeyBytes).buffer,
        keyData = ,
        algorithm = algo,
        extractable = extractable,
        keyUsages = keyUsages
      )
      .asInstanceOf[js.Promise[org.scalajs.dom.CryptoKey]]

    importKeyPromise.toFuture.flatMap { privateKey =>
      crypto.subtle
        .sign(
          algorithm = "Ed25519",
          key = privateKey,
          data = new Int8Array(payload).buffer
        )
        .asInstanceOf[js.Promise[js.typedarray.ArrayBuffer]]
        .toFuture
        .map { sigBuf =>
          new Int8Array(sigBuf).toArray
        }
    }
  }

//    def verify(pubKey: Array[Byte], payload: Array[Byte], signature: Array[Byte]): Boolean = {
//        try {
//            val keyFactory = KeyFactory.getInstance("Ed25519")
//            val publicKeySpec = new X509EncodedKeySpec(pubKey)
//            val publicKey = keyFactory.generatePublic(publicKeySpec)
//
//            val verifier = Signature.getInstance("Ed25519")
//            verifier.initVerify(publicKey)
//            verifier.update(payload)
//            verifier.verify(signature)
//        } catch {
//            case _: Exception => false
//        }
//    }

  def verify(
      pubKey: Array[Byte],
      payload: Array[Byte],
      signature: Array[Byte]
  ): Future[Boolean] = {
    val algo = js.Dynamic.literal(name = "Ed25519")
    val extractable = false
    val keyUsages = js.Array("verify")

    val importKeyPromise = crypto.subtle
      .importKey(
        format = "raw",
        // keyData = new Int8Array(pubKey).buffer,
        keyData = new Int8Array(pubKey.toJSArray).buffer,
        algorithm = algo,
        extractable = extractable,
        keyUsages = keyUsages
      )
      .asInstanceOf[js.Promise[org.scalajs.dom.CryptoKey]]

    importKeyPromise.toFuture.flatMap { publicKey =>
      crypto.subtle
        .verify(
          algorithm = "Ed25519",
          key = publicKey,
          signature = new Int8Array(signature).buffer,
          data = new Int8Array(payload).buffer
        )
        .asInstanceOf[js.Promise[Boolean]]
        .toFuture
    }
  }
}
