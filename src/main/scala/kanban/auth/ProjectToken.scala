package kanban.auth

import ucan.{Ucan, KeyMaterial}
import scala.concurrent.{ExecutionContext, Future}
import typings.dexie.mod.Table
import kanban.persistence.DexieDB.dexieDB
import scala.scalajs.js
import scala.scalajs.js.JSConverters._

object ProjectToken {

  // IndexedDB table for tokens
  trait TokenEntry extends js.Object {
    val id: String
    val jwt: String
    val cid: String
  }

  private val tokenTable: Table[TokenEntry, String, TokenEntry] =
    dexieDB.table("tokens")

  private def saveToken(id: String, jwt: String, cid: String)
    (implicit ec: ExecutionContext): Future[Unit] = {
  
    // Ensure id is not empty otherwise use a browser-compatible unique string
    val validId = if (id.nonEmpty) id else js.Date.now().toString + Math.random().toString

    val entry = new TokenEntry {
      val id: String = validId
      val jwt: String = jwt
      val cid: String = cid
    }
    tokenTable.put(entry).toFuture.map(_ => ())
  }

  private def getToken(id: String)(implicit ec: ExecutionContext): Future[Option[TokenEntry]] = {
    tokenTable.get(id).toFuture.map(_.toOption)
  }

  // Root Token creation when a person creates a project
  def createProjectRootToken(
      projectId: String,
      lifetimeSec: Int = 86400
  )(implicit ec: ExecutionContext): Future[(String, String)] = {

    KeyMaterialSingleton.keyMaterial.now() match {
      case Some(ownerKey) =>
        val payload = Ucan.builder()
          .issuedBy(ownerKey)
          .forAudience(ownerKey)
          .withLifetime(lifetimeSec)
          .claimingCapability(s"project:$projectId", "read")
          .claimingCapability(s"project:$projectId", "create")
          .claimingCapability(s"project:$projectId", "update")
          .claimingCapability(s"project:$projectId", "delete")
          .build()

        Ucan.sign(payload, ownerKey).flatMap { ucan =>
          Ucan.createCID(ucan).flatMap { cid =>
            val cidStr = cid.encode()
            val jwt    = Ucan.encodeJwt(ucan)

            saveToken(projectId, jwt, cidStr).map(_ => (jwt, cidStr))
          }
        }

      case None =>
        Future.failed(new Exception("No key material available. Cannot create project token."))
    }
  }

  def createDelegationToken(
    projectId: String,
    collaboratorKey: KeyMaterial,
    capabilities: Seq[String],
    lifetimeSec: Int,
    proofCid: Ucan.CID
  )(implicit ec: ExecutionContext): Future[(String, Ucan.CID)] = {

    KeyMaterialSingleton.keyMaterial.now() match {
      case Some(ownerKey) =>
        val base = Ucan.builder()
          .issuedBy(ownerKey)
          .forAudience(collaboratorKey)
          .withLifetime(lifetimeSec)
          .witnessedBy(proofCid)

        val payload = capabilities.foldLeft(base)((b, cap) =>
          b.claimingCapability(s"project:$projectId", cap)
        ).build()

        Ucan.sign(payload, ownerKey).flatMap { ucan =>
          Ucan.createCID(ucan).flatMap { cid =>
            val jwt = Ucan.encodeJwt(ucan)
            saveToken(projectId, jwt, cid.encode()).map(_ => (jwt, cid))
          }
        }

      case None =>
        Future.failed(new Exception("No key material available. Cannot create delegation token."))
    }
  }
}
