package kanban.ui.components

import com.raquo.laminar.api.L.{*, given}
import kanban.routing.Pages.KanbanBoardPage
import kanban.routing.Router
import kanban.ui.views.GlobalState // Import global username

object NavBar {
  def apply(): HtmlElement = {
    val showSidebar = Var(false)
    div(
      idAttr := "nav-bar",
      div(
        cls := "nav-bar-container",
        div(
          cls := "kanzleimagement-text",
          "Kanzleimanagement"
        ),
        span(
          cls := "separator",
          "|"
        ),
        a(
          cls := "nav-link",
          href := Router.relativeUrlForPage(KanbanBoardPage),
          "Kanzleiboard"
        ),
        List(
          "Kalkulationen",
          "Angebote",
          "Rechnungen",
          "Controlling",
          "Zeiten",
          "Einstellungen"
        ).map { linkText =>
          a(
            cls := "nav-link",
            href := s"#${linkText.toLowerCase}",
            linkText
          )
        }
      ),
      UserCounter(),
      a(
        cls := "nav-link username-link", // Style it as a link
        child.text <-- GlobalState.usernameVar.signal, // Display the current username
        onClick --> { _ =>
          showSidebar.update(currentValue =>
            !currentValue
          ) // Toggle the sidebar visibility
        }
      ),
      AccountSidebar(showSidebar)
    )
  }
}
