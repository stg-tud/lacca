package kanban.service

import kanban.auth.DexieProofResolver
import kanban.persistence.DexieDB.dexieDB
import typings.dexie.mod.Table
import ucan.Ucan

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Promise}
import scala.scalajs.js
import scala.scalajs.js.Date
import scala.scalajs.js.UndefOr
import scala.scalajs.js.JSConverters.*

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

  private def computeCapKeys(ucan: Ucan.Ucan): Seq[String] = {
    ucan.payload.cap.flatMap { cap =>
      cap.abilities.keys.map(ability => s"${cap.resource}#$ability")
    }.distinct
  }

  def save(
      ucan: Ucan.Ucan
  ): Future[String] = {
    val jwt = Ucan.encodeJwt(ucan)
    val caps = computeCapKeys(ucan)
    Ucan.createCID(ucan).flatMap { cidObj =>
      val cidStr = cidObj.encode()
      val row = new UcanTokenRow {
        val cid = cidStr
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
          if (caps.nonEmpty) caps.toJSArray
          else js.undefined
      }
      ucanTable.put(row).toFuture.map { _ =>
        DexieProofResolver.onTokenSaved(cidStr, jwt)
        cidStr
      }
    }
  }

  def saveJwt(jwt: String): Future[String] =
    scala.util
      .Try(Ucan.decodeJwt(jwt))
      .fold(
        err => Future.failed(err),
        ucan => save(ucan.get)
      )

  // TODO: remove this demo method later
  def generateAndSaveRandom(): Future[String] =
    for {
      // keys for demo purposes
      issuerKm <- Ucan.createDefaultKeymaterial()
      audienceKm <- Ucan.createDefaultKeymaterial()

      // build a minimal, valid payload
      payload = Ucan
        .builder()
        .issuedBy(issuerKm)
        .forAudience(audienceKm)
        .withLifetime(5 * 60) // 5 minutes
        .claimingCapability(
          "urn:kanban:test",
          "debug/test"
        )
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

  def filterUnexpired(
      rows: Seq[UcanTokenRow],
      nowMs: Double = Date.now()
  ): Seq[UcanTokenRow] = {
    rows.filter { r =>
      r.exp.fold(true)(_.toDouble > nowMs)
    }
  }
}
