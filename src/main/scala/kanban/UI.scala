package kanban

import org.scalajs.dom
import org.scalajs.dom.document
import scalatags.JsDom.all._

object UI {
  def setupUI(): Unit = {
    // Create navigation bar using ScalaTags
    val navBar = div(id := "nav-bar")(
      for (linkText <- List("Kanzleiboard", "Kalkulationen", "Angebote", "Rechnungen", "Controlling", "Zeiten", "Einstellungen")) yield {
        a(cls := "nav-link", href := s"#${linkText.toLowerCase}")(linkText)
      }
    ).render

    document.body.appendChild(navBar)

    // Create filter input
    val filterInput = input(
      `type` := "date",
      id := "start",
      placeholder := "Zeitraum"
    ).render

    // Attach the input box to the document
    document.body.appendChild(filterInput)

    // Create Kanban board using ScalaTags
    val kanbanBoard = div(id := "kanban-board")(
      for (columnTitle <- List("Neu", "Geplant", "In Arbeit", "Abrechenbar", "Abgeschlossen")) yield {
        div(cls := "kanban-column")(
          h3(cls := "kanban-column-header")(columnTitle),
          div(cls := "kanban-column-content", id := s"column-${columnTitle.toLowerCase.replace(" ", "-")}")
        )
      }
    ).render

    document.body.appendChild(kanbanBoard)

    // Create "Projekt hinzufügen" button using ScalaTags
    val addButton = button(id := "add-project-button")("Projekt hinzufügen").render
    addButton.onclick = { (e: dom.MouseEvent) =>
      ProjectForm.openAddProjectForm()
    }

    document.body.appendChild(addButton)
  }
}
