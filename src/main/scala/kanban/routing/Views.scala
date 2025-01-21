package kanban.routing

import com.raquo.laminar.api.L.{*, given}
import com.raquo.waypoint.SplitRender
import kanban.routing.Pages.*
import kanban.routing.Router
import kanban.routing.Router.*
import kanban.ui.views.{KanbanBoardPageView, LoginPageView}

val currentView = SplitRender(Router.currentPageSignal)
    .collectStatic(KanbanBoardPage)(KanbanBoardPageView())
    .collectStatic(LoginPage)(LoginPageView())
    .signal
