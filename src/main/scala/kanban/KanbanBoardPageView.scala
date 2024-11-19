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
  val showKanbanBoard: Var[Boolean] = Var(true) // Initially, the Kanban board is shown
  val selectedProjectVar: Var[Option[Project]] = Var(None)

  def apply(): HtmlElement = {
    div(
      NavBar(),
      child <-- showKanbanBoard.signal.map { showKanban =>
        if (showKanban) renderKanbanBoard()
        else renderProjectDetails()
      }
    )
  }

  def addNewProject(project: Project): Unit = {
    projectList.update(list => list :+ project)
  }

  def removeProject(projectName: String): Unit = {
    projectList.update(list => list.filter(_.name != projectName))
  }

  def renderProjectCard(
    projectName: String, status: ProjectStatus, revisorName: Revisors, deadline: Option[Date], timeTracked: Double
    ): HtmlElement = {
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
      revisorName.toString,
      onClick --> { _ =>
        // Handle the click and set the selected project for details view
        val selectedProject = Project(projectName, status, revisorName, deadline, timeTracked)
        selectedProjectVar.set(Some(selectedProject))
        showKanbanBoard.set(false) // Hide Kanban board and show project details
      }
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

  def renderKanbanBoard(): HtmlElement = {
    setupDragAndDrop()
    val kanbanElement = div(
      idAttr := "kanbanboard-container",
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
                      selectedDeadline match {
                        case Some(selectedDate) =>
                          p.deadline.exists(_.getTime() == selectedDate.getTime()) // Only projects with a matching deadline appear
                        case None =>
                          true // No deadline filter selected, show all projects
                          }
                        }
                    .map(p => renderProjectCard(p.name, p.status, p.revisor, p.deadline, p.timeTracked)) // Render project cards
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

  def renderProjectDetails(): HtmlElement = {
    div(
      child <-- selectedProjectVar.signal.map {
        case Some(project) => ProjectDetailsPageView(project)
        case None => div("No project selected") // Message or empty div if no project is selected
      }
    )
  }
}

