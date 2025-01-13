package kanban

import kanban.Router.*
import org.scalajs.dom
import com.raquo.laminar.api.L.{*, given}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object NavBar {
  def apply(): HtmlElement = {
    // Signal to control the visibility of the account sidebar
    val showSidebar = Var(false)

    div(
      idAttr := "nav-bar",
      div(
        List(
          "Kanzleiboard",
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
      button(
        idAttr := "show-account-sidebar-button",
        "Username",
        onClick --> { _ =>
          showSidebar.update(currentValue => !currentValue) // Toggle the sidebar visibility
        }
      ),
      AccountSidebar(showSidebar)
    )
  }
}
