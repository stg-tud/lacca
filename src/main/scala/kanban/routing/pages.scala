package kanban.routing

import kanban.domain.models.ProjectId
import org.getshaka.nativeconverter.{NativeConverter, ParseState}
import rdts.base.Uid

object Pages {
  sealed trait Page(val title: String) derives NativeConverter
  case object KanbanBoardPage extends Page("Kanban Board")
  case class ProjectDetailsPage(id: ProjectId) extends Page("Project Details")
  case object LoginPage extends Page("Login")
  case object AccountOverviewPage extends Page("Account Overview")
}
