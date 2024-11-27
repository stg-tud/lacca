package kanban

import com.raquo.airstream.ownership.ManualOwner
import com.raquo.laminar.api.L.{*, given}
import kanban.AddProjectFormView.*
import kanban.DragAndDrop.*
import kanban.ProjectCommands.update
import kanban.models.*

import scala.scalajs.js.Date

enum ProjectCommands:
  case add(project: Project)
  case delete(id: ProjectId)
  case update(id: ProjectId, newValue: Project)
  case modifyStatus(id: ProjectId, newStatus: ProjectStatus)

object KanbanBoardPageView {
  val toggleDisplay: Var[String] = Var("none")
  val projectStatusValues: List[String] =
    ProjectStatus.values.map(_.toString).toList
  val revisors: List[String] =
    List("Manas", "Jakob", "Julian", "Bach", "Bearbeiter")
  val selectedRevisorVar = Var("Bearbeiter")
  val selectedDeadlineVar = Var(Option.empty[Date])
  val showKanbanBoard: Var[Boolean] = Var(
    true
  ) // Initially, the Kanban board is shown
  val selectedProjectVar: Var[Option[ProjectId]] = Var(Option.empty[ProjectId])

  val projectCommandBus: EventBus[ProjectCommands] =
    new EventBus[ProjectCommands]
  val projectsMap: Signal[Map[ProjectId, Project]] = projectCommandBus.stream
    .scanLeft(Map.empty[ProjectId, Project])((projectMap, command) =>
      command match
        case ProjectCommands.add(project) =>
          projectMap + (project.id -> project)
        case ProjectCommands.delete(id) => projectMap.removed(id)
        case ProjectCommands.update(id, newProject) =>
          projectMap.updated(id, newProject)
        case ProjectCommands.modifyStatus(id, newStatus) =>
          projectMap.updatedWith(id)(old => old.map(_.copy(status = newStatus)))
    )
  val projectsSignal: Signal[List[Project]] = projectsMap.map(_.values.toList)
  given ManualOwner()
  projectCommandBus.stream.addObserver(Observer(c => println(s"command: $c")))

  val selectedProject: Signal[Option[Project]] =
    projectsMap.combineWith(selectedProjectVar).map {
      case (projects, Some(id)) => projects.get(id)
      case _                    => None
    }

  def apply(): HtmlElement = {
    setupDragAndDrop(updateProjectStatus)
    div(
      NavBar(),
      child <-- showKanbanBoard.signal.map { showKanban =>
        if (showKanban) renderKanbanBoard()
        else renderProjectDetails()
      }
    )
  }

  // Function to update project status
  def updateProjectStatus(
      projectId: ProjectId,
      newStatus: ProjectStatus
  ): Unit = {
    projectCommandBus.emit(ProjectCommands.modifyStatus(projectId, newStatus))
  }

  def addNewProject(project: Project): Unit = {
    projectCommandBus.emit(ProjectCommands.add(project))
  }

  def removeProject(projectId: ProjectId): Unit = {
    projectCommandBus.emit(ProjectCommands.delete(projectId))
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
    val kanbanElement = div(
      idAttr := "kanbanboard-container",
      // Date filter
      input(
        typ := "date", // Input field for date selection
        onInput.mapToValue --> { dateStr =>
          if (dateStr.nonEmpty) {
            selectedDeadlineVar.set(
              Some(new Date(dateStr))
            ) // Set the selected deadline as a Date
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
          if (revisor == "Bearbeiter")
            option(value := revisor, revisor, selected := true)
          else option(value := revisor, revisor)
        ),
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
              children <-- projectsSignal
                .combineWith(
                  selectedRevisorVar.signal,
                  selectedDeadlineVar.signal
                )
                .map {
                  (
                      list: List[Project],
                      selectedRevisor: String,
                      selectedDeadline: Option[Date]
                  ) =>
                    list
                      .filter(p =>
                        p.status.toString == columnTitle
                      ) // Filter by status (column)
                      .filter(p =>
                        selectedRevisor == "Bearbeiter" || p.revisor.toString == selectedRevisor
                      ) // Filter by revisor
                      .filter { p =>
                        selectedDeadline match {
                          case Some(selectedDate) =>
                            p.deadline.exists(
                              _.getTime() == selectedDate.getTime()
                            ) // Only projects with a matching deadline appear
                          case None =>
                            true // No deadline filter selected, show all projects
                        }
                      }
                }
                .split(_.id)(renderProjectCard)
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

  def renderProjectCard(
      projectId: String,
      initialProject: Project,
      projectSignal: Signal[Project]
  ): HtmlElement = {
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
      dataAttr("y") := "0",
      onClick --> { _ =>
        // Handle the click and set the selected project for details view
        selectedProjectVar.set(Some(projectId))
        showKanbanBoard.set(false) // Hide Kanban board and show project details
      }
    )
  }

  def renderProjectDetails(): HtmlElement = {
    div(
      child <-- selectedProject.map {
        case Some(project) => ProjectDetailsPageView(project)
        case None =>
          div(
            "No project selected"
          ) // Message or empty div if no project is selected
      }
    )
  }
}
