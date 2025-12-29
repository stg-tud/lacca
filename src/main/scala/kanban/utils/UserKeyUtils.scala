package kanban.utils

import kanban.sync.Replica
import kanban.domain.models.UserId
import scala.concurrent.Future
import ucan.Base32
import ucan.Base58
import scala.concurrent.ExecutionContext.Implicits.global

object UserKeyUtils {

  /** Finds and returns a user's public key from the replicaIdTable */
  def findUserPublicKey(userId: UserId): Future[Option[String]] = {
    Replica.replicaIdTable.toArray().toFuture.map { entries =>
      entries.find(e => e.userId.stripPrefix("🪪") == userId.delegate).map(_.publicKey)
    }
  }

  /** Converts a Base32-encoded Ed25519 public key into a did:key string */
  def makeDidFromPublicKey(pubKeyStr: String): String = {
    val pubKeyBytes: Array[Byte] = Base32.decode(pubKeyStr)
    val prefix: Array[Byte] = Array(0xED.toByte, 0x01.toByte)
    val combined: Array[Byte] = prefix ++ pubKeyBytes
    val base58Encoded: String = Base58.encode(combined)
    s"did:key:z$base58Encoded"
  }

  /** Finds and returns audience id from the audience did */
  def lookupUserIdByDid(audienceDid: String): Future[Option[String]] = {
    val didBase58 = audienceDid.stripPrefix("did:key:z")
    val didBytes = Base58.decode(didBase58)
    val pubKeyBytes = didBytes.drop(2)
    val pubKeyBase32 = Base32.encode(pubKeyBytes)

    Replica.replicaIdTable.toArray().toFuture.map { entries =>
      entries.toSeq.find(_.publicKey == pubKeyBase32).map(_.userId)
    }
  }

  /** Parses a capability string and returns (projectId, permission) */
  def parseCapability(cap: String): (String, String) = {
    val parts = cap.split(":") // ["kanban", "project", <projectid>#<permission>]
    if parts.length == 3 then
      val projectAndPerm = parts(2).split("#") // [projectid, permission]
      val projectId = projectAndPerm(0)
      val permission = projectAndPerm.lift(1).getOrElse("None")
      (projectId, permission)
    else
      ("Unknown", "Unknown")
  }
}