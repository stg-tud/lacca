package kanban.auth

import ucan.{KeyMaterial => KM}
import ucan.Ucan
import kanban.service.UcanTokenStore
import kanban.domain.models.ProjectId

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object ProjectUcanService {
  private val ProjectNs = "kanban:project"
  private val Create = "create"

  def resourceFor(projectId: ProjectId): String =
    s"$ProjectNs:${projectId.delegate}"

  // issue new Ucan with same issuer and audience
  def issue(
      projectId: ProjectId,
      creator: KM,
      lifetimeSeconds: Long = 365L * 24 * 3600
  ): Future[Ucan.Ucan] = {
    val payload =
      Ucan
        .builder()
        .issuedBy(creator)
        .forAudience(creator)
        .withLifetime(lifetimeSeconds)
        .claimingCapability(resourceFor(projectId), Create)
        .withNonce()
        .build()

    Ucan.sign(payload, creator)
  }

  def issueAndSave(projectId: ProjectId, creator: KM): Future[String] = {
    println(s"issueAndSave called for projectId: ${projectId}")
    val capKey = s"${resourceFor(projectId)}#$Create"
    issue(projectId, creator).flatMap(ucan =>
      UcanTokenStore.save(ucan, capabilityKeys = Seq(capKey))
    )
  }
}
