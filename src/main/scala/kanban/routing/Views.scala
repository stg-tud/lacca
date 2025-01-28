package kanban.routing

import com.raquo.laminar.api.L.{*, given}
import com.raquo.waypoint.SplitRender
import kanban.routing.Pages.*
import kanban.routing.Router
import kanban.routing.Router.*
import kanban.ui.views.{AccountOverview, KanbanBoardPageView, LoginPageView, ProjectDetailsPageView}

val currentView = SplitRender(Router.currentPageSignal)
    .collectStatic(KanbanBoardPage)(KanbanBoardPageView())
    .collectStatic(LoginPage)(LoginPageView())
    .collectSignal[ProjectDetailsPage](ProjectDetailsPageView(_))
    .collectStatic(AccountOverviewPage)(AccountOverview())
    .signal
