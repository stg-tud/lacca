package kanban.routing

import com.raquo.waypoint.*
import kanban.routing.Pages.*

val appRoot = root / "app"

val routes = List(
  Route.static(KanbanBoardPage, root / endOfSegments),
  Route.static(KanbanBoardPage, root / "kanbanboard" / endOfSegments),
  Route.static(LoginPage, root / "login" / endOfSegments),
  Route[ProjectDetailsPage, String](
    encode =
      projectDetailsPage => projectDetailsPage.id.getOrElse(0).toString(),
    decode = arg => ProjectDetailsPage(id = arg.toIntOption),
    pattern = root / "projectDetails" / segment[String] / endOfSegments
  ),
  Route.static(AccountOverviewPage, root / "accountOverview" / endOfSegments)
)
