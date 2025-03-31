package kanban.routing

import com.raquo.waypoint.*
import kanban.routing.Pages.*

val appRoot = root / "app"

val routes = List(
  Route.static(KanbanBoardPage, root / endOfSegments, basePath=Route.fragmentBasePath),
  Route.static(KanbanBoardPage, root / "kanbanboard" / endOfSegments, basePath=Route.fragmentBasePath),
  Route.static(LoginPage, root / "login" / endOfSegments, basePath=Route.fragmentBasePath),
  Route[ProjectDetailsPage, String](
    encode =
      projectDetailsPage => projectDetailsPage.id.getOrElse(0).toString(),
    decode = arg => ProjectDetailsPage(id = arg.toIntOption),
    pattern = root / "projectDetails" / segment[String] / endOfSegments, basePath=Route.fragmentBasePath
  ),
  Route.static(AccountOverviewPage, root / "accountOverview" / endOfSegments, basePath=Route.fragmentBasePath)
)
