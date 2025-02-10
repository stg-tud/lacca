package kanban.ui.components

import com.raquo.laminar.api.L.{*, given}
import kanban.routing.Pages.{AccountOverviewPage, LoginPage}
import kanban.routing.Router
import kanban.ui.views.GlobalState // Import global username
import kanban.service.UserService.*
import scala.scalajs.js

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
          child.text <-- GlobalState.usernameVar.signal // Display the logged-in username
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
                // Log out by clearing global username and localStorage, then redirect to LoginPage
                GlobalState.usernameVar.set("Guest")
                js.Dynamic.global.localStorage.removeItem("username") // Remove username from localStorage
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
