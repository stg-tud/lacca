package kanban

import org.scalajs.dom
import org.scalajs.dom.document
import org.scalajs.dom.html.{Button, Div, Input}
import scalatags.JsDom.all._

object UI {
  def setupUI(): Unit = {
    val navBar = div(id := "nav-bar")(
      for (linkText <- List("Kanzleiboard", "Kalkulationen", "Angebote", "Rechnungen", "Controlling", "Zeiten", "Einstellungen")) yield {
        a(cls := "nav-link", href := s"#${linkText.toLowerCase}")(linkText)
      }
    ).render

    document.body.appendChild(navBar)

    val kanbanBoard = div(id := "kanban-board")(
      for (columnTitle <- List("Neu", "Geplant", "In Arbeit", "Abrechenbar", "Abgeschlossen")) yield {
        div(cls := "kanban-column")(
          h3(cls := "kanban-column-header")(columnTitle),
          div(cls := "kanban-column-content", id := s"column-${columnTitle.toLowerCase.replace(" ", "-")}")
        )
      }
    ).render

    document.body.appendChild(kanbanBoard)

    val addButton = button(id := "add-project-button")("Projekt hinzufügen").render
    addButton.onclick = { (e: dom.MouseEvent) =>
      ProjectForm.openAddProjectForm()
    }

    val detailedFormButton = button(id := "open-detailed-form-button", style := "position: fixed; bottom: 20px; left: 20px;")("Zeit erfassen").render
    detailedFormButton.onclick = { (e: dom.MouseEvent) =>
      DetailedForm.openDetailedForm()
    }

    document.body.appendChild(addButton)
    document.body.appendChild(detailedFormButton)

    val filterButton = button(id := "filter-date-button", style := "position: fixed; bottom: 20px; right: 20px;")("Filtern").render
    filterButton.onclick = { (e: dom.MouseEvent) =>
      openFilterForm()
    }

    document.body.appendChild(filterButton)
  }

  def openFilterForm(): Unit = {
    val formOverlay = div(id := "filter-form-overlay")(
      div(id := "filter-form")(
        h3("Filteroptionen"),
        div(
          label(`for` := "filter-date")("Fälligkeitsdatum"),
          input(`type` := "date", id := "filter-date").render
        ),
        button(`type` := "button", id := "apply-filter-button")("Filter anwenden").render,
        button(`type` := "button", id := "cancel-filter-button")("Abbrechen").render
      )
    ).render

    document.body.appendChild(formOverlay)

    dom.document.getElementById("apply-filter-button").addEventListener("click", { (e: dom.MouseEvent) =>
      applyDateFilter()
      document.body.removeChild(formOverlay)
    })

    dom.document.getElementById("cancel-filter-button").addEventListener("click", { (e: dom.MouseEvent) =>
      document.body.removeChild(formOverlay)
    })
  }

  def applyDateFilter(): Unit = {
    val filterDate = document.getElementById("filter-date").asInstanceOf[Input].value

    if (filterDate.nonEmpty) {
      val kanbanCards = document.querySelectorAll(".kanban-card")

      for (i <- 0 until kanbanCards.length) {
        val card = kanbanCards(i).asInstanceOf[Div]
        val deadlineDiv = card.querySelector(".kanban-card-deadline")
        if (deadlineDiv != null) {
          val deadlineText = deadlineDiv.textContent.replace("Deadline: ", "")
          val deadlineParts = deadlineText.split("\\.")
          val deadline = s"${deadlineParts(2)}-${deadlineParts(1)}-${deadlineParts(0)}"
          if (deadline.compareTo(filterDate) < 0) {
            card.style.display = "none" 
          } else {
            card.style.display = "block"
          }
        }
      }
    }
  }
}
