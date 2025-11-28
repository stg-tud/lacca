package kanban.auth

import ucan.{KeyMaterial => KM}
import ucan.Ucan
import kanban.service.UcanTokenStore
import kanban.domain.models.ProjectId

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Try, Success, Failure}
import scala.scalajs.js

import io.circe.Json
import rdts.base.Uid

object ProjectUcanService {
  import Capabilities.*

  private val didParser = Ucan.createDefaultDidParser()

  // issue new Ucan with same issuer and audience
  def issueProjectCreationToken(
      projectId: ProjectId,
      lifetimeSeconds: Long = 365L * 24 * 3600
  ): Future[String] = {
    KeyMaterialSingleton.initializedKeyMaterial.flatMap { issuerKm =>
      val payload = Ucan
        .builder()
        .issuedBy(issuerKm)
        .forAudience(issuerKm)
        .withLifetime(lifetimeSeconds)
        .claimingCapability(projectResource(projectId), Create)
        .withNonce()
        .build()

      Ucan.sign(payload, issuerKm).flatMap(UcanTokenStore.save)
    }
  }

  def delegateToDid(
      projectId: ProjectId,
      audienceDid: String,
      abilities: Seq[String],
      lifetimeSeconds: Long = 365L * 24 * 3600,
      caveats: List[Json] = List(Json.obj())
  ): Future[String] = {
    KeyMaterialSingleton.initializedKeyMaterial.flatMap { issuerKm =>
      val audienceKmTry = didParser.parse(audienceDid)
      audienceKmTry match {
        case Failure(e) => Future.failed(e)
        case Success(audienceKm) =>
          val createKey = projectCapKey(projectId, Create)
          UcanTokenStore.listByCapKey(createKey).flatMap { rows =>
            val unexpired = UcanTokenStore.filterUnexpired(rows)
            val maybeProofCid: Option[String] =
              unexpired.sortBy(_.createdAt).reverse.headOption.map(_.cid)

            val builder0 =
              Ucan
                .builder()
                .issuedBy(issuerKm)
                .forAudience(audienceKm)
                .withLifetime(lifetimeSeconds)
                .withNonce()

            val builderWithCaps =
              abilities.foldLeft(builder0) { (b, ability) =>
                b.claimingCapability(
                  projectResource(projectId),
                  ability,
                  caveats
                )
              }

            val builderWithProof = {
              maybeProofCid match {
                case Some(cidStr) =>
                  val cid = Ucan.createDefaultCidParser().parse(cidStr).get
                  builderWithCaps.witnessedBy(cid)

                case None =>
                  builderWithCaps
              }

            }
            val payload = builderWithProof.build()
            // Return full JWT instead of CID
            Ucan.sign(payload, issuerKm).flatMap { ucan =>
              val jwt = Ucan.encodeJwt(ucan)
              UcanTokenStore.save(ucan).map(_ => jwt)
            }
          }
      }
    }
  }

  def currentUserDid(): Future[String] =
    KeyMaterialSingleton.initializedKeyMaterial.map(didParser.keyMaterialToDid)

  def userOwnsProject(projectId: ProjectId, did: String): Future[Boolean] = {
    val key = projectCapKey(projectId, Create)
    UcanTokenStore.listByCapKey(key).map { rows =>
      val unexpired = UcanTokenStore.filterUnexpired(rows)
      unexpired.exists(r => r.iss == did && r.aud == did)
    }
  }

  // more query methods can be added here
  // get all projects a user has a specific ability for
  def getProjectsForUserWithAbility(
      did: String,
      ability: String
  ): Future[Seq[ProjectId]] = {
    UcanTokenStore.listByCapKey(s"$ProjectNs:$ability").map { rows =>
      val unexpired = UcanTokenStore.filterUnexpired(rows)
      unexpired
        .filter(r => r.iss == did || r.aud == did)
        .flatMap { r =>
          r.capKeys.toOption.getOrElse(js.Array()).toSeq
        }
        .filter(key => key.endsWith(s"#$ability"))
        .map { key =>
          // Extract projectId from capability key format (resource#ability)
          val resourcePart = key.split("#").head
          val projectIdStr =
            resourcePart.replace(projectResource(Uid.predefined("")), "")
          Uid.predefined(projectIdStr)
        }
        .distinct
    }
  }

  def tokensForUser(did: String): Future[Seq[UcanTokenStore.UcanTokenRow]] = {
    for {
      byIss <- UcanTokenStore.listByIssuer(did)
      byAud <- UcanTokenStore.listByAudience(did)
    } yield UcanTokenStore.filterUnexpired((byIss ++ byAud).distinct)
  }
}
