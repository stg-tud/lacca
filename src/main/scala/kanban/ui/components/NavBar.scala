package kanban.ui.components

import com.raquo.laminar.api.L.{*, given}

object NavBar {
    def apply(): HtmlElement = {
        div(
            idAttr := "nav-bar",
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
        )
    }
}
