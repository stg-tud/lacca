package kanban.ui.components

import com.raquo.laminar.api.L.{*, given}

object NavBar {
  def apply(): HtmlElement = {
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
