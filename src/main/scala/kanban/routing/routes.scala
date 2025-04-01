package kanban.routing

import com.raquo.waypoint.*
import kanban.routing.Pages.*

val appRoot = root / "app"
val base = "/lacca/#"

val routes = List(
  Route.static(KanbanBoardPage, root / endOfSegments, basePath=base),
  Route.static(LoginPage, root / "login" / endOfSegments, basePath=base),
  Route[ProjectDetailsPage, String](
    encode =
      projectDetailsPage => projectDetailsPage.id.getOrElse(0).toString(),
    decode = arg => ProjectDetailsPage(id = arg.toIntOption),
    pattern = root / "projectDetails" / segment[String] / endOfSegments, basePath=base
  ),
  Route.static(AccountOverviewPage, root / "accountOverview" / endOfSegments, basePath=base)
)
