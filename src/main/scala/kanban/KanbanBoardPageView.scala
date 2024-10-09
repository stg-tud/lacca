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

object KanbanBoardPageView {
  val toggleDisplay: Var[String] = Var("none")
  val projectList: Var[List[Project]] = Var(List[Project]())
  val projectStatusValues: List[String] =
    ProjectStatus.values.map(_.toString).toList
  val revisors: List[String] = List("Manas", "Jakob", "Julian", "Bach", "Bearbeiter")
  val selectedRevisorVar = Var("Bearbeiter")

  def apply(): HtmlElement = {
    setupDragAndDrop()
    val kanbanElement = div(
      idAttr := "kanbanboard-container",
      NavBar(),
      //date filter
      input(
        typ := "date",
        idAttr := "start",
        placeholder := "Zeitraum"
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
              children <-- projectList.signal.combineWith(selectedRevisorVar.signal).map {
                case (list, selectedRevisor) =>
                  list
                    .filter(p => p.status.toString == columnTitle) // Filter by status (column)
                    .filter(p => selectedRevisor == "Bearbeiter" || p.revisor.toString == selectedRevisor) // Filter by revisor
                    .map(p => renderProjectCard(p.name, p.revisor)) // Render project cards
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

  def renderProjectCard(projectName: String, revisorName: Revisors): HtmlElement = {
    div(
      className := "kanban-card",
      projectName,
      button(
        className := "delete-project-button",
        "Löschen",
        onClick --> (_ => removeProject(projectName))
      ),
      revisorName.toString
    )
  }
}

