package kanban.routing

import com.raquo.waypoint.*
import kanban.routing.Pages.*

val appRoot = root / "app"

val routes = List(
    Route.static(KanbanBoardPage, root / endOfSegments),
    Route.static(KanbanBoardPage, root / "kanbanboard" / endOfSegments),
    Route.static(LoginPage, root / "login" / endOfSegments)
)
