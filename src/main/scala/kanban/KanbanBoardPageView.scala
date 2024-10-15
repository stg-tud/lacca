package kanban

import org.scalajs.dom
import org.scalajs.dom.document
import kanban.NavBar
import com.raquo.laminar.api.L.{*, given}
import kanban.DragAndDrop.*
import com.raquo.laminar.api.features.unitArrows
import kanban.AddProjectFormView.*
import kanban.Pages.*
import kanban.models.*
import scala.scalajs.js.Date

object KanbanBoardPageView {
  val toggleDisplay: Var[String] = Var("none")
  val projectList: Var[List[Project]] = Var(List[Project]())
  val projectStatusValues: List[String] =
    ProjectStatus.values.map(_.toString).toList
  val revisors: List[String] = List("Manas", "Jakob", "Julian", "Bach", "Bearbeiter")
  val selectedRevisorVar = Var("Bearbeiter")
  val selectedDeadlineVar = Var(Option.empty[Date])

  def apply(): HtmlElement = {
    setupDragAndDrop()
    val kanbanElement = div(
      idAttr := "kanbanboard-container",
      NavBar(),
      // Date filter
      input(
        typ := "date", // Input field for date selection
        onInput.mapToValue --> { dateStr =>
          if (dateStr.nonEmpty) {
            selectedDeadlineVar.set(Some(new Date(dateStr))) // Set the selected deadline as a Date
          } else {
            selectedDeadlineVar.set(None) // No date selected
          }
        }
      ),
      // Revisor filter
      select(
        idAttr := "revisor",
        // Populate the dropdown options based on the `revisors` list
        revisors.map(revisor => 
          if (revisor == "Bearbeiter") option(value := revisor, revisor, selected := true)
          else option(value := revisor, revisor)),
        onChange.mapToValue --> { value =>
          selectedRevisorVar.set(value) // Update the selected revisor variable
        }
      ),
      div(
        idAttr := "kanban-board",
        projectStatusValues.map { columnTitle =>
          div(
            cls := "kanban-column",
            h3(cls := "kanban-column-header", columnTitle),
            div(
              cls := "kanban-column-content",
              idAttr := s"column-${columnTitle}",
              children <-- projectList.signal.combineWith(selectedRevisorVar.signal, selectedDeadlineVar.signal).map {
                (list: List[Project], selectedRevisor: String, selectedDeadline: Option[Date]) =>
                  list
                    .filter(p => p.status.toString == columnTitle) // Filter by status (column)
                    .filter(p => selectedRevisor == "Bearbeiter" || p.revisor.toString == selectedRevisor) // Filter by revisor
                    .filter { p =>
                      // Ensure p.deadline is defined and compare it with the selectedDeadline
                      p.deadline.exists(deadlineDate =>
                        selectedDeadline.forall(selectedDate => deadlineDate.getTime() == selectedDate.getTime())
                      )
                    }
                    .map(p => renderProjectCard(p.name, p.revisor, p.deadline)) // Render project cards
              }
            )
          )
        }
      ),
      button(
        idAttr := "add-project-button",
        "projekt hinzufügen",
        onClick --> { e =>
          {
            toggleDisplay.update(t => "")
          }
        }
      ),
      div(
        idAttr := "form-overlay",
        display <-- toggleDisplay,
        AddProjectFormView()
      )
    )
    kanbanElement
  }

  def addNewProject(project: Project): Unit = {
    projectList.update(list => list :+ project)
  }

  def removeProject(projectName: String): Unit = {
    projectList.update(list => list.filter(_.name != projectName))
  }

  def renderProjectCard(projectName: String, revisorName: Revisors, deadline: Option[Date]): HtmlElement = {
    div(
      className := "kanban-card",
      projectName,
      button(
        className := "delete-project-button",
        "Löschen",
        onClick --> (_ => removeProject(projectName))
      ),
      br(),
      formatDate(deadline),
      br(),
      revisorName.toString
    )
  }

  // Function to format the Date as "YYYY-MM-DD"
  def formatDate(date: Option[Date]): String = {
    if (date.nonEmpty) {
            val convertedDate: Date = date.getOrElse(new Date())
            convertedDate.toLocaleDateString() // Formats as "YYYY-MM-DD"
    } else {
            ""
    }
  }
}

