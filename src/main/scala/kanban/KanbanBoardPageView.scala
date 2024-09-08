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
      //kanban board view
      div(
        idAttr := "kanban-board",
        projectStatusValues.map { columnTitle =>
          div(
            cls := "kanban-column",
            h3(cls := "kanban-column-header", columnTitle),
            div(
              cls := "kanban-column-content",
              idAttr := s"column-${columnTitle}",
              children <-- projectList.signal.map(list => {
                list.filter(_.status.toString() == columnTitle).map(p => {
                  renderProjectCard(p.name)
                })
              })
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

  def renderProjectCard(projectName: String): HtmlElement = {
    div(
      className := "kanban-card",
      projectName,
      button(
        className := "delete-project-button",
        "Löschen",
        onClick --> (_ => removeProject(projectName))
      )
    )
  }
}

