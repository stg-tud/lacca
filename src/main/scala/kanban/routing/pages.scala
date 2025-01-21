package kanban.routing

import io.bullet.borer.*
import io.bullet.borer.derivation.MapBasedCodecs.*
import kanban.domain.models.ProjectId

object Pages {
  sealed trait Page(val title: String)
  case object KanbanBoardPage extends Page("Kanban Board")
  //case class ProjectDetailsPage(id: ProjectId) extends Page("Project Details")
  case object LoginPage extends Page("Login")
  given pageCodec: Codec[Page] = deriveAllCodecs
}
