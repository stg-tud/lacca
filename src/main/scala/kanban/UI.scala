package kanban

import org.scalajs.dom
import org.scalajs.dom.document
import org.scalajs.dom.html.{Anchor, Button, Div}

object UI {
  def setupUI(): Unit = {
    // Create navigation bar
    val navBar = document.createElement("div").asInstanceOf[Div]
    navBar.id = "nav-bar"

    val links = List("Kanzleiboard", "Kalkulationen", "Angebote", "Rechnungen", "Controlling", "Zeiten", "Einstellungen")

    for (linkText <- links) {
      val link = document.createElement("a").asInstanceOf[Anchor]
      link.href = s"#${linkText.toLowerCase}"
      link.textContent = linkText
      link.setAttribute("class", "nav-link")
      navBar.appendChild(link)
    }

    document.body.appendChild(navBar)

    // Create Kanban board
    val kanbanBoard = document.createElement("div").asInstanceOf[Div]
    kanbanBoard.id = "kanban-board"

    val columns = List("Neu", "Geplant", "In Arbeit", "Abrechenbar", "Abgeschlossen")

    for (columnTitle <- columns) {
      val column = document.createElement("div").asInstanceOf[Div]
      column.setAttribute("class", "kanban-column")

      val columnHeader = document.createElement("h3")
      columnHeader.setAttribute("class", "kanban-column-header")
      columnHeader.textContent = columnTitle

      val columnContent = document.createElement("div").asInstanceOf[Div]
      columnContent.setAttribute("class", "kanban-column-content")
      columnContent.id = s"column-${columnTitle.toLowerCase.replace(" ", "-")}"

      column.appendChild(columnHeader)
      column.appendChild(columnContent)
      kanbanBoard.appendChild(column)
    }

    document.body.appendChild(kanbanBoard)

    // Create "Projekt hinzufügen" button
    val addButton = document.createElement("button").asInstanceOf[Button]
    addButton.textContent = "Projekt hinzufügen"
    addButton.id = "add-project-button"
    addButton.addEventListener("click", { (e: dom.MouseEvent) =>
      ProjectForm.openAddProjectForm()
    })

    document.body.appendChild(addButton)
  }
}
