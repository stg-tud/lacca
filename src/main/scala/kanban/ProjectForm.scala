package kanban

import org.scalajs.dom
import org.scalajs.dom.document
import org.scalajs.dom.html.{Button, Div, Input, Label, Select, Option}

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

    // Create dropdown for selecting the column
    val labelColumn = document.createElement("label").asInstanceOf[Label]
    labelColumn.textContent = "Status von dem Projekt"
    labelColumn.id = "label-column"
    val selectColumn = document.createElement("select").asInstanceOf[Select]
    selectColumn.id = "project-column"
    val columns = List("Neu", "Geplant", "In Arbeit", "Abrechenbar", "Abgeschlossen")
    for (column <- columns) {
      val option = document.createElement("option").asInstanceOf[Option]
      option.value = s"column-${column.toLowerCase.replace(" ", "-")}"
      option.textContent = column
      selectColumn.appendChild(option)
    }

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
    form.appendChild(labelColumn)
    form.appendChild(selectColumn)
    form.appendChild(submitButton)
    form.appendChild(cancelButton)
    formOverlay.appendChild(form)
    document.body.appendChild(formOverlay)
  }

  def addProject(): Unit = {
    val projectName = document.getElementById("project-name").asInstanceOf[Input].value
    val selectedColumn = document.getElementById("project-column").asInstanceOf[Select].value
    if (projectName.nonEmpty) {
      val newCard = document.createElement("div").asInstanceOf[Div]
      newCard.setAttribute("class", "kanban-card")
      newCard.textContent = projectName
      newCard.setAttribute("draggable", "true")
      val deleteButton = document.createElement("button").asInstanceOf[Button]
      deleteButton.textContent = "Löschen"
      deleteButton.id = "delete-project-button"
      deleteButton.addEventListener("click", { (e: dom.MouseEvent) =>
        newCard.parentNode.removeChild(newCard)
        deleteButton.parentNode.removeChild(deleteButton)
      })

      val neuColumn = document.getElementById("column-neu").asInstanceOf[Div]
      newCard.appendChild(deleteButton)
      neuColumn.appendChild(newCard)
      newCard.setAttribute("draggable", "true")

      val targetColumn = document.getElementById(selectedColumn).asInstanceOf[Div]
      targetColumn.appendChild(newCard)


      // Set initial data-x and data-y attributes for drag position
      newCard.setAttribute("data-x", "0")
      newCard.setAttribute("data-y", "0")

      // Re-initialize drag and drop for new card
      DragAndDrop.initializeDragAndDrop(newCard)
    }
  }
}
