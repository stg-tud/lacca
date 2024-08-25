package kanban

import kanban.Router.*
import org.scalajs.dom
import com.raquo.laminar.api.L.{*, given}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

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
