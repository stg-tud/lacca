package kanban

import io.bullet.borer.*
import io.bullet.borer.derivation.MapBasedCodecs.*
import kanban.models.ProjectId

object Pages {
  sealed trait Page(val title: String)

  case object LoginPage extends Page("Login")
  case object SignupPage extends Page("Sign Up")
  case object KanbanBoardPage extends Page("Kanban Board")
  case class ProjectDetailsPage(id: ProjectId) extends Page("Project Details")
  case object AccountOverviewPage extends Page("Account Overview")

  given pageCodec: Codec[Page] = deriveAllCodecs
}
