package kanban

import org.scalajs.dom
import org.scalajs.dom.document
import org.scalajs.dom.html.{Button, Div, Input, Span}

object ProjectForm {
  def openAddProjectForm(): Unit = {
    // Create form
    val formOverlay = document.createElement("div").asInstanceOf[Div]
    formOverlay.id = "form-overlay"

    val form = document.createElement("div").asInstanceOf[Div]
    form.id = "add-project-form"

    val formTitle = document.createElement("h3")
    formTitle.textContent = "Neues Projekt hinzufügen"

    val inputName = document.createElement("input").asInstanceOf[Input]
    inputName.id = "project-name"
    inputName.placeholder = "Projektname eingeben"

    val inputDeadline = document.createElement("input").asInstanceOf[Input]
    inputDeadline.id = "project-deadline"
    inputDeadline.`type` = "date"
    inputDeadline.placeholder = "Fälligkeitsdatum eingeben"

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
    form.appendChild(inputName)
    form.appendChild(inputDeadline) 
    form.appendChild(submitButton)
    form.appendChild(cancelButton)
    formOverlay.appendChild(form)
    document.body.appendChild(formOverlay)
  }

  def addProject(): Unit = {
    val projectName = document.getElementById("project-name").asInstanceOf[Input].value
    val projectDeadline = document.getElementById("project-deadline").asInstanceOf[Input].value 

    if (projectName.nonEmpty && projectDeadline.nonEmpty) {
      val formattedDeadline = formatDate(projectDeadline)

      val newCard = document.createElement("div").asInstanceOf[Div]
      newCard.setAttribute("class", "kanban-card")

      val deadlineDiv = document.createElement("div").asInstanceOf[Div]
      deadlineDiv.setAttribute("class", "kanban-card-deadline")
      deadlineDiv.textContent = s"Deadline: $formattedDeadline"
      
      deadlineDiv.style.display = "flex"
      deadlineDiv.style.setProperty("align-items", "center")

      newCard.appendChild(document.createTextNode(projectName))
      newCard.appendChild(deadlineDiv) 
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

  // Helper function to format the date
  def formatDate(date: String): String = {
    val parts = date.split("-")
    if (parts.length == 3) {
      s"${parts(2)}.${parts(1)}.${parts(0)}"
    } else {
      date // Return original if the format is unexpected
    }
  }
} //





  