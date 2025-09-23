package kanban.auth

import ucan.Ucan
import ucan.Ucan.ProofResolver
import scala.util.{Try, Success, Failure}
import kanban.service.UcanTokenStore

object DexieProofResolver extends ProofResolver {
  @volatile private var _proofs: Map[String, String] = Map.empty

  private val _revocations = new Ucan.InMemoryRevocationStore()
  override def revocationStore: Ucan.RevocationStore = _revocations

  override def proofs(): Map[String, String] = _proofs

  override def resolve(cid: Ucan.CID): Try[Ucan.Ucan] = {
    val key = cid.encode()
    _proofs.get(key) match {
      case Some(jwt) => Ucan.decodeJwt(jwt)
      case None =>
        Failure(new SecurityException(s"Proof not found in cache: cid=$key"))
    }
  }

  def seedFromDb(): scala.concurrent.Future[Unit] = {
    import scala.concurrent.ExecutionContext.Implicits.global
    UcanTokenStore.listAll().map { rows =>
      _proofs = rows.map(r => r.cid -> r.token).toMap
      ()
    }
  }

  def onTokenSaved(cid: String, jwt: String): Unit = {
    _proofs = _proofs + (cid -> jwt)
  }
}
