package kanban

import org.scalajs.dom
import org.scalajs.dom.document
import org.scalajs.dom.html.{Button, Div, Input}

object ProjectForm {
  def openAddProjectForm(): Unit = {
    // Create form
    val formOverlay = document.createElement("div").asInstanceOf[Div]
    formOverlay.id = "form-overlay"

    val form = document.createElement("div").asInstanceOf[Div]
    form.id = "add-project-form"

    val formTitle = document.createElement("h3")
    formTitle.textContent = "Neues Projekt hinzufügen"

    val input = document.createElement("input").asInstanceOf[Input]
    input.id = "project-name"
    input.placeholder = "Projektname eingeben"

    val submitButton = document.createElement("button").asInstanceOf[Button]
    submitButton.textContent = "Hinzufügen"
    submitButton.addEventListener("click", { (e: dom.MouseEvent) =>
      addProject()
      document.body.removeChild(formOverlay)
    })

    val cancelButton = document.createElement("button").asInstanceOf[Button]
    cancelButton.textContent = "Abbrechen"
    cancelButton.addEventListener("click", { (e: dom.MouseEvent) =>
      document.body.removeChild(formOverlay)
    })

    form.appendChild(formTitle)
    form.appendChild(input)
    form.appendChild(submitButton)
    form.appendChild(cancelButton)
    formOverlay.appendChild(form)
    document.body.appendChild(formOverlay)
  }

  def addProject(): Unit = {
    val projectName = document.getElementById("project-name").asInstanceOf[Input].value
    if (projectName.nonEmpty) {
      val newCard = document.createElement("div").asInstanceOf[Div]
      newCard.setAttribute("class", "kanban-card")
      newCard.textContent = projectName
      newCard.setAttribute("draggable", "true")

      val neuColumn = document.getElementById("column-neu").asInstanceOf[Div]
      neuColumn.appendChild(newCard)

      // Set initial data-x and data-y attributes for drag position
      newCard.setAttribute("data-x", "0")
      newCard.setAttribute("data-y", "0")

      // Re-initialize drag and drop for new card
      DragAndDrop.initializeDragAndDrop(newCard)
    }
  }
}
