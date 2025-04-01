package kanban.routing

import com.raquo.waypoint.*
import kanban.routing.Pages.*
import rdts.base.Uid

val appRoot = root / "app"

val routes = List(
  Route.static(KanbanBoardPage, root / endOfSegments),
  Route.static(KanbanBoardPage, root / "kanbanboard" / endOfSegments),
  Route.static(LoginPage, root / "login" / endOfSegments),
  Route[ProjectDetailsPage, String](
    encode =
      projectDetailsPage => projectDetailsPage.toString(),
    decode = arg => ProjectDetailsPage(id = Uid.predefined(arg)),
    pattern = root / "projectDetails" / segment[String] / endOfSegments
  ),
  Route.static(AccountOverviewPage, root / "accountOverview" / endOfSegments)
)
