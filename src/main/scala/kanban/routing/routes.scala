package kanban.routing

import com.raquo.waypoint.*
import kanban.routing.Pages.*
import rdts.base.Uid

val appRoot = root / "app"
val base = "/lacca/#"

val routes = List(
  Route.static(KanbanBoardPage, root / endOfSegments, basePath = base),
  Route.static(LoginPage, root / "login" / endOfSegments, basePath = base),
  Route[ProjectDetailsPage, String](
    encode = projectDetailsPage => projectDetailsPage.id.delegate,
    decode = arg => ProjectDetailsPage(id = Uid.predefined(arg)),
    pattern = root / "projectDetails" / segment[String] / endOfSegments,
    basePath = base
  ),
  Route.static(
    AccountOverviewPage,
    root / "accountOverview" / endOfSegments,
    basePath = base
  )
)
