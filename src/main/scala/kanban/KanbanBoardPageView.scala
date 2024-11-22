package kanban

import com.raquo.airstream.ownership.ManualOwner
import com.raquo.laminar.api.L.{*, given}
import kanban.AddProjectFormView.*
import kanban.DragAndDrop.*
import kanban.models.*
import org.scalajs.dom

import scala.scalajs.js.Date

enum ProjectCommands:
  case add(project: Project)
  case delete(id: ProjectId)
  case modifyStatus(id: ProjectId, newStatus: ProjectStatus)

object KanbanBoardPageView {
  val toggleDisplay: Var[String] = Var("none")
  val projectStatusValues: List[String] =
    ProjectStatus.values.map(_.toString).toList
  val revisors: List[String] = List("Manas", "Jakob", "Julian", "Bach", "Bearbeiter")
  val selectedRevisorVar = Var("Bearbeiter")
  val selectedDeadlineVar = Var(Option.empty[Date])

  val projectCommandBus: EventBus[ProjectCommands] = new EventBus[ProjectCommands]
  val projectsSignal: Signal[List[Project]] = projectCommandBus.stream.scanLeft(List())((projectList, command) => command match
    case ProjectCommands.add(project) => projectList :+ project
    case ProjectCommands.delete(id) => projectList.filter(_.id != id)
    case ProjectCommands.modifyStatus(id, newStatus) => projectList.map(project =>
      if project.id == id then project.copy(status = newStatus) else project)).toObservable
  given ManualOwner()
  projectCommandBus.stream.addObserver(Observer(c => println(s"command: $c")))
//  projectStream.addObserver(Observer(projects => println(s"projects: $projects")))

  def apply(): HtmlElement = {
    // Function to update project status
    def updateProjectStatus(projectId: ProjectId, newStatus: ProjectStatus): Unit = {
        projectCommandBus.emit(ProjectCommands.modifyStatus(projectId, newStatus))
    }

    setupDragAndDrop(updateProjectStatus, projectsSignal)
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
              children <-- projectsSignal.signal.combineWith(selectedRevisorVar.signal, selectedDeadlineVar.signal).map {
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
              }.split(_.id)(renderProjectCard)
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
    projectCommandBus.emit(ProjectCommands.add(project))
  }

  def removeProject(projectId: ProjectId): Unit = {
    projectCommandBus.emit(ProjectCommands.delete(projectId))
  }

  def renderProjectCard(projectId: String, initialProject: Project, projectSignal: Signal[Project]): HtmlElement = {
    div(
      className := "kanban-card",
      text <-- projectSignal.map(_.name),
      button(
        className := "delete-project-button",
        "Löschen",
        onClick --> (_ => removeProject(projectId))
      ),
      br(),
      text <-- projectSignal.map(p => formatDate(p.deadline)),
      br(),
      text <-- projectSignal.map(_.revisor.toString),
      dataAttr("name") <-- projectSignal.map(_.name),
      dataAttr("x") := "0",
      dataAttr("y") := "0"
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

