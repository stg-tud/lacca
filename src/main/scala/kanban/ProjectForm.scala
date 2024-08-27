package kanban

import org.scalajs.dom
import org.scalajs.dom.document
import org.scalajs.dom.html.{Button, Div, Input}
import scalatags.JsDom.all._

object ProjectForm {
  def openAddProjectForm(): Unit = {
    val formOverlay = div(id := "form-overlay")(
      div(id := "add-project-form")(
        h3("Neues Projekt hinzufügen"),
        div(
          label(`for` := "project-name")("Projektname"),
          input(`type` := "text", id := "project-name", placeholder := "Projektname eingeben").render
        ),
        div(
          label(`for` := "project-deadline")("Fälligkeitsdatum"),
          input(`type` := "date", id := "project-deadline", placeholder := "Fälligkeitsdatum eingeben").render
        ),
        button(`type` := "button")("Hinzufügen").render,
        button(`type` := "button")("Abbrechen").render
      )
    ).render

    document.body.appendChild(formOverlay)

    formOverlay.querySelector("button:first-of-type").asInstanceOf[Button].addEventListener("click", { (e: dom.MouseEvent) =>
      addProject()
      document.body.removeChild(formOverlay)
    })

    formOverlay.querySelector("button:last-of-type").asInstanceOf[Button].addEventListener("click", { (e: dom.MouseEvent) =>
      document.body.removeChild(formOverlay)
    })
  }

  def addProject(): Unit = {
    val projectName = document.getElementById("project-name").asInstanceOf[Input].value
    val projectDeadline = document.getElementById("project-deadline").asInstanceOf[Input].value

    if (projectName.nonEmpty && projectDeadline.nonEmpty) {
      val formattedDeadline = formatDate(projectDeadline)

      val newCard = div(cls := "kanban-card", draggable := "true", data.x := "0", data.y := "0")(
        span(projectName).render,
        div(cls := "kanban-card-deadline", style := "display: flex; align-items: center;")(
          s"Deadline: $formattedDeadline"
        )
      ).render

      val neuColumn = document.getElementById("column-neu").asInstanceOf[Div]
      neuColumn.appendChild(newCard)

      DragAndDrop.initializeDragAndDrop(newCard)
    }
  }

  def formatDate(date: String): String = {
    val parts = date.split("-")
    if (parts.length == 3) {
      s"${parts(2)}.${parts(1)}.${parts(0)}"
    } else {
      date // Return original if the format is unexpected
    }
  }
}

