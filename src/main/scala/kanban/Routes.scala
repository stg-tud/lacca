package kanban

import kanban.Pages.*
import com.raquo.waypoint.*

val appRoot = root / "app"

val routes = List(
  Route.static(LoginPage, root / "login" / endOfSegments),
  Route.static(SignupPage, root / "signup" / endOfSegments)
)
