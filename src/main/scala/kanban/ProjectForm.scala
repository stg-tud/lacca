package kanban

import org.scalajs.dom
import org.scalajs.dom.document
import org.scalajs.dom.html.{Input, Select}
import scalatags.JsDom.all._

object ProjectForm {
  def openAddProjectForm(): Unit = {
    // Create form using ScalaTags
    val form = div(id := "add-project-form")(
      h3("Neues Projekt hinzufügen"),
      input(`type` := "text", id := "project-name", placeholder := "Projektname eingeben"),
      label(`for` := "project-column")("Status von dem Projekt"),
      select(id := "project-column")(
        option(value := "column-neu")("Neu"),
        option(value := "column-geplant")("Geplant"),
        option(value := "column-in-arbeit")("In Arbeit"),
        option(value := "column-abrechenbar")("Abrechenbar"),
        option(value := "column-abgeschlossen")("Abgeschlossen")
      ),
      button(`type` := "button", id := "submit-button")("Hinzufügen"),
      button(`type` := "button", id := "cancel-button")("Abbrechen")
    ).render

    // Create the form overlay using ScalaTags
    val formOverlay = div(
      id := "form-overlay",
      form
    ).render

    // Append the formOverlay to the document body
    document.body.appendChild(formOverlay)

    // Add event listeners
    dom.document.getElementById("submit-button").addEventListener("click", { (e: dom.MouseEvent) =>
      addProject()
      document.body.removeChild(formOverlay)
    })

    dom.document.getElementById("cancel-button").addEventListener("click", { (e: dom.MouseEvent) =>
      document.body.removeChild(formOverlay)
    })
  }

  def addProject(): Unit = {
    val projectName = document.getElementById("project-name").asInstanceOf[Input].value
    val selectedColumn = document.getElementById("project-column").asInstanceOf[Select].value
    if (projectName.nonEmpty) {
      // Create the new Kanban card using ScalaTags
      val newCard = div(
        cls := "kanban-card",
        projectName,
        button(
          `type` := "button",
          cls := "delete-project-button" // Added class for styling and identification
        )("Löschen")
      ).render

      // Find the target column and append the new card to it
      val targetColumn = document.getElementById(selectedColumn)
      if (targetColumn != null) {
        targetColumn.appendChild(newCard)
      }

      // Add event listener for the delete button
      newCard.querySelector(".delete-project-button").addEventListener("click", { (e: dom.MouseEvent) =>
        newCard.parentNode.removeChild(newCard)
      })

      // Set initial data-x and data-y attributes for drag position
      newCard.setAttribute("draggable", "true")
      newCard.setAttribute("data-x", "0")
      newCard.setAttribute("data-y", "0")

      // Re-initialize drag and drop for new card
      DragAndDrop.initializeDragAndDrop(newCard)
    }
  }
}
