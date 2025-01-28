package kanban.ui.components

import com.raquo.laminar.api.L.{*, given}
import kanban.routing.Pages.{AccountOverviewPage, LoginPage}
import kanban.routing.Router

object AccountSidebar {
  def apply(showSidebar: Var[Boolean]): HtmlElement = {
    div(
      idAttr := "account-sidebar",
      cls := "account-sidebar",
      display <-- showSidebar.signal.map {
        case true => "block" // Show sidebar when true
        case false => "none" // Hide sidebar when false
      },
      div(
        cls := "account-sidebar-header",
        div(
          cls := "username",
          "Username" // Replace with dynamic data if necessary
        ),
        button(
          cls := "close-sidebar-button",
          onClick --> { _ =>
            showSidebar.set(false) // Hide sidebar when close button is clicked
          },
          "X"
        )
      ),
      div(
        cls := "account-sidebar-links",
        ul(
          li(
            a(
              href := "#",
              "Kontoübersicht",
              onClick --> { _ =>
                // Navigate to the Account Overview page when clicked
                Router.pushState(AccountOverviewPage)
              },
            )
          ),
          li(
            a(
              href := "#",
              onClick --> { _ =>
                // Redirect to the Login page when clicked
                Router.pushState(LoginPage)
              },
              "Abmelden"
            )
          )
        )
      )
    )
  }
}
