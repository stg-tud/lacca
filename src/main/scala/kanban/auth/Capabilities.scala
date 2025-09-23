package kanban.auth

import kanban.domain.models.ProjectId

object Capabilities {
  val ProjectNs = "kanban:project"

  val Create = "create"
  val Read = "read"
  val Update = "update"
  val Delete = "delete"

  def projectResource(id: ProjectId): String =
    s"$ProjectNs:${id.delegate}"

  def capKey(resource: String, ability: String): String =
    s"$resource#$ability"

  def projectCapKey(id: ProjectId, ability: String): String =
    capKey(projectResource(id), ability)
}
