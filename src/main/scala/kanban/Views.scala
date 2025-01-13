package kanban

import kanban.Router.*
import kanban.Pages.*
import com.raquo.laminar.api.L.{*, given}
import com.raquo.waypoint.SplitRender

val views = SplitRender(Router.currentPageSignal)
  .collectStatic(LoginPage)(LoginPageView())
  .collectStatic(SignupPage)(SignupPageView())
  .collectStatic(KanbanBoardPage)(KanbanBoardPageView())
  .collectSignal[ProjectDetailsPage](ProjectDetailsPageView(_))
  .collectStatic(AccountOverviewPage)(AccountOverview())
  .signal
