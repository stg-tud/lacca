package kanban.ui.components

import com.raquo.laminar.api.L.{*, given}
import kanban.controllers.ProjectController.projectEventBus
import kanban.domain.events.ProjectEvent.{ClickedOn, Deleted}
import kanban.domain.models.{Project, ProjectId}
import kanban.ui.views.DragAndDrop.isDragging
import org.scalajs.dom.HTMLElement

import scala.scalajs.js.Date

object ProjectCard {
  def apply(
      projectId: ProjectId,
      projectSignal: Signal[Project]
  ): HtmlElement = {
    div(
      className := "kanban-card",
      text <-- projectSignal.map(_.name.read),
      br(),
      button(
        className := "delete-project-button",
        "Löschen",
        onClick --> { _ =>
          println(s"project delete button clicked!!")
          projectEventBus.emit(Deleted(projectId)) }
      ),
      br(),
      text <-- projectSignal.map(p => formatDate(p.deadline.read)),
      br(),
      text <-- projectSignal.map(_.revisorId.value.map(_.toString).getOrElse("")),
      dataAttr("project-id") <-- projectSignal.map(_.id.delegate),
      dataAttr("name") <-- projectSignal.map(_.name.read),
      dataAttr("x") := "0",
      dataAttr("y") := "0",

      onClick --> { e =>
        if (!isDragging && !e.target.asInstanceOf[HTMLElement].classList.contains("delete-project-button"))
          projectEventBus.emit(ClickedOn(projectId)) }
    )
  }

  def formatDate(date: Option[Date]): String = {
    if (date.nonEmpty) {
      val convertedDate: Date = date.getOrElse(new Date())
      convertedDate.toLocaleDateString // Formats as "YYYY-MM-DD"
    } else {
      ""
    }
  }
}
