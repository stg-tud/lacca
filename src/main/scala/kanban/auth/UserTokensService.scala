package kanban.auth

import ucan.Ucan
import scala.scalajs.js
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import kanban.domain.models.ProjectId
import kanban.service.UcanTokenStore
import rdts.base.Uid

object UserTokensService {
  import kanban.auth.Capabilities.*

  // returns all unexpired capability keys the user currently has
  def capabilityKeysForDid(did: String): Future[Set[String]] =
    for {
      byIss <- UcanTokenStore.listByIssuer(did)
      byAud <- UcanTokenStore.listByAudience(did)
    } yield {
      val rows = UcanTokenStore.filterUnexpired((byIss ++ byAud).distinct)
      rows.flatMap(r => r.capKeys.toOption.getOrElse(js.Array()).toSeq).toSet
    }

  def hasAbility(
      did: String,
      projectId: ProjectId,
      ability: String
  ): Future[Boolean] = {
    UcanTokenStore.listByCapKey(projectCapKey(projectId, ability)).map { rows =>
      UcanTokenStore
        .filterUnexpired(rows)
        .exists(r => r.aud == did || r.iss == did)
    }
  }

  // get all unexpired tokens where the user is either issuer or audience
  def tokensForUser(did: String): Future[Seq[UcanTokenStore.UcanTokenRow]] = {
    for {
      byIss <- UcanTokenStore.listByIssuer(did)
      byAud <- UcanTokenStore.listByAudience(did)
    } yield UcanTokenStore.filterUnexpired((byIss ++ byAud).distinct)
  }

  // Get all abilities a user has for a specific project
  def getUserAbilitiesForProject(
      did: String,
      projectId: ProjectId
  ): Future[Set[String]] = {
    val projectRes = projectResource(projectId)
    tokensForUser(did).map { tokens =>
      tokens
        .flatMap(r => r.capKeys.toOption.getOrElse(js.Array()).toSeq)
        .filter(_.startsWith(s"$projectRes#"))
        .map(key => key.split("#")(1))
        .toSet
    }
  }

}
