package kanban

import io.bullet.borer.*
import io.bullet.borer.derivation.MapBasedCodecs.*

object Pages {
  sealed trait Page(val title: String)

  case object LoginPage extends Page("Login")
  case object SignupPage extends Page("Sign Up")
  case object KanbanBoardPage extends Page("Kanban Board")
  case class ProjectDetailsPage(id: String) extends Page("Project Details")

  given pageCodec: Codec[Page] = deriveAllCodecs
}
