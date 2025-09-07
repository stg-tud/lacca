package kanban.service

import kanban.persistence.DexieDB.dexieDB
import typings.dexie.mod.Table
import ucan.Ucan
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Promise}
import scala.scalajs.js
import scala.scalajs.js.Date
import scala.scalajs.js.UndefOr
import scala.scalajs.js.JSConverters._

object UcanTokenStore {

  trait UcanTokenRow extends js.Object {
    val cid: String
    val token: String
    val iss: String
    val aud: String
    val exp: js.UndefOr[Double]
    val nbf: js.UndefOr[Double]
    val createdAt: Double
    val capKeys: js.UndefOr[js.Array[String]]
  }

  private val ucanTable: Table[UcanTokenRow, String, UcanTokenRow] =
    dexieDB.table("ucanTokens")

  def save(
      ucan: Ucan.Ucan,
      capabilityKeys: Seq[String] = Nil
  ): Future[String] = {
    val jwt = Ucan.encodeJwt(ucan)
    Ucan.createCID(ucan).flatMap { cidObj =>
      val row = new UcanTokenRow {
        val cid = cidObj.encode()
        val token = jwt
        val iss = ucan.payload.iss
        val aud = ucan.payload.aud
        val exp = ucan.payload.exp match
          case Some(d) => d.getTime()
          case None    => js.undefined
        val nbf = ucan.payload.nbf match
          case Some(d) => d.getTime()
          case None    => js.undefined
        val createdAt = Date.now()
        val capKeys =
          if (capabilityKeys.nonEmpty) capabilityKeys.toJSArray
          else js.undefined
      }
      ucanTable.put(row).toFuture.map(_ => cidObj.encode())
    }
  }

  def saveJwt(jwt: String, capKeys: Seq[String] = Nil): Future[String] =
    scala.util
      .Try(Ucan.decodeJwt(jwt))
      .fold(
        err => Future.failed(err),
        u => save(u.get, capKeys)
      )

  // TODO: remove this demo method later
  def generateAndSaveRandom(): Future[String] =
    for {
      // random keys for demo purpose
      issuerKm <- Ucan.createDefaultKeymaterial()
      audienceKm <- Ucan.createDefaultKeymaterial()

      payload = Ucan
        .builder()
        .issuedBy(issuerKm)
        .forAudience(audienceKm)
        .withLifetime(5 * 60)
        .claimingCapability(
          "urn:kanban:test",
          "debug/test"
        ) // simple demo cap
        .withNonce()
        .build()

      ucan <- Ucan.sign(payload, issuerKm)
      cidStr <- UcanTokenStore.save(ucan)
    } yield cidStr

  def listAll(): Future[Seq[UcanTokenRow]] =
    ucanTable.toArray().toFuture.map(_.toSeq)

  def listByIssuer(did: String): Future[Seq[UcanTokenRow]] =
    ucanTable.where("iss").equalsIgnoreCase(did).toArray().toFuture.map(_.toSeq)

  def listByAudience(did: String): Future[Seq[UcanTokenRow]] =
    ucanTable.where("aud").equalsIgnoreCase(did).toArray().toFuture.map(_.toSeq)

  def listByCapKey(capKey: String): Future[Seq[UcanTokenRow]] =
    ucanTable
      .where("capKeys")
      .equalsIgnoreCase(capKey)
      .toArray()
      .toFuture
      .map(_.toSeq)

}
