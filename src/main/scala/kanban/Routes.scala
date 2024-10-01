package kanban

import com.raquo.waypoint.*
import kanban.Pages.*

val appRoot = root / "app"

val routes = List(
  Route.static(KanbanBoardPage, root / endOfSegments),
  Route.static(LoginPage, root / "login" / endOfSegments),
  Route.static(SignupPage, root / "signup" / endOfSegments),
  Route.static(KanbanBoardPage, root / "kanbanboard" / endOfSegments)
)
