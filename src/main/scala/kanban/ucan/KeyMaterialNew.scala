//package kanban.ucan
//
//import scala.scalajs.js
//import scala.scalajs.js.typedarray._
//import scala.scalajs.js.annotation._
//import scala.concurrent.Future
//import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
//
//@js.native
//@JSGlobalScope
//object CryptoAPI extends js.Object {
//  val crypto: js.Dynamic = js.native
//}
//
//sealed trait KeyMaterial {
//  val publicKey: Array[Byte]
//  val privateKey: Option[Array[Byte]]
//  val name: String
//  val magicBytes: (Byte, Byte)
//  val jwtAlg: String
//
//  def create(): Future[KeyMaterial]
//  def sign(payload: Array[Byte]): Future[Array[Byte]]
//  def verify(payload: Array[Byte], signature: Array[Byte]): Future[Boolean]
//}
//
//case class Ed25519KeyMaterial(
//                               override val publicKey: Array[Byte],
//                               override val privateKey: Option[Array[Byte]]
//                             ) extends KeyMaterial {
//  val name = "Ed25519"
//  val magicBytes = (0xed.toByte, 0x01.toByte)
//  val jwtAlg = "EdDSA"
//
//  def create(): Future[KeyMaterial] = Ed25519KeyMaterial.create()
//  def sign(payload: Array[Byte]): Future[Array[Byte]] = Ed25519KeyMaterial.sign(this, payload)
//  def verify(payload: Array[Byte], signature: Array[Byte]): Future[Boolean] =
//    Ed25519KeyMaterial.verify(publicKey, payload, signature)
//}
//
//object Ed25519KeyMaterial {
//  def create(): Future[Ed25519KeyMaterial] = {
//    val algo = js.Dynamic.literal(
//      name = "Ed25519"
//    )
//    val extractable = true
//    val keyUsages = js.Array("sign", "verify")
//    val subtle = CryptoAPI.crypto.subtle
//
//    val promise = subtle.generateKey(algo, extractable, keyUsages)
//      .asInstanceOf[js.Promise[js.Dynamic]]
//
//    promise.toFuture.flatMap { keyPair =>
//      val publicKeyPromise = subtle.exportKey("raw", keyPair.publicKey)
//        .asInstanceOf[js.Promise[ArrayBuffer]]
//      val privateKeyPromise = subtle.exportKey("pkcs8", keyPair.privateKey)
//        .asInstanceOf[js.Promise[ArrayBuffer]]
//
//      for {
//        pub <- publicKeyPromise.toFuture
//        priv <- privateKeyPromise.toFuture
//      } yield {
//        Ed25519KeyMaterial(
//          new Int8Array(pub).toArray,
//          Some(new Int8Array(priv).toArray)
//        )
//      }
//    }
//  }
//
//  def sign(keyMaterial: Ed25519KeyMaterial, payload: Array[Byte]): Future[Array[Byte]] = {
//    val subtle = CryptoAPI.crypto.subtle
//    val privateKeyBytes = keyMaterial.privateKey.getOrElse(
//      throw new IllegalArgumentException("Private key is required for signing")
//    )
//    val importAlgo = js.Dynamic.literal(name = "Ed25519")
//    val keyPromise = subtle.importKey(
//      "pkcs8",
//      new Int8Array(privateKeyBytes).buffer,
//      importAlgo,
//      false,
//      js.Array("sign")
//    ).asInstanceOf[js.Promise[js.Dynamic]]
//
//    keyPromise.toFuture.flatMap { privateKey =>
//      val signPromise = subtle.sign(
//        js.Dynamic.literal(name = "Ed25519"),
//        privateKey,
//        new Int8Array(payload).buffer
//      ).asInstanceOf[js.Promise[ArrayBuffer]]
//
//      signPromise.toFuture.map { sig =>
//        new Int8Array(sig).toArray
//      }
//    }
//  }
//
//  def verify(pubKey: Array[Byte], payload: Array[Byte], signature: Array[Byte]): Future[Boolean] = {
//    val subtle = CryptoAPI.crypto.subtle
//    val importAlgo = js.Dynamic.literal(name = "Ed25519")
//    val keyPromise = subtle.importKey(
//      "raw",
//      new Int8Array(pubKey).buffer,
//      importAlgo,
//      false,
//      js.Array("verify")
//    ).asInstanceOf[js.Promise[js.Dynamic]]
//
//    keyPromise.toFuture.flatMap { publicKey =>
//      val verifyPromise = subtle.verify(
//        js.Dynamic.literal(name = "Ed25519"),
//        publicKey,
//        new Int8Array(signature).buffer,
//        new Int8Array(payload).buffer
//      ).asInstanceOf[js.Promise[Boolean]]
//
//      verifyPromise.toFuture
//    }
//  }
//}