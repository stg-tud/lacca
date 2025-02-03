package kanban.ui.components

import com.raquo.laminar.api.L.{*, given}
import kanban.controllers.ProjectController.projectEventBus
import kanban.domain.models.{Project, ProjectId}
import kanban.domain.events.ProjectEvent.{ClickedOn, Deleted}
import org.scalajs.dom.HTMLElement

import scala.scalajs.js.Date

object ProjectCard {
  def apply(
      projectId: ProjectId,
      projectSignal: Signal[Project]
  ): HtmlElement = {
    div(
      className := "kanban-card",
      text <-- projectSignal.map(_.name),
      br(),
      button(
        className := "delete-project-button",
        "Löschen",
        onClick --> { _ => projectEventBus.emit(Deleted(projectId)) }
      ),
      br(),
      text <-- projectSignal.map(p => formatDate(p.deadline)),
      br(),
      text <-- projectSignal.map(_.revisorId.getOrElse(0).toString),
      dataAttr("project-id") <-- projectSignal.map(_.id.get.toString),
      dataAttr("name") <-- projectSignal.map(_.name),
      dataAttr("x") := "0",
      dataAttr("y") := "0",

      onClick --> { e =>
        if (!e.target.asInstanceOf[HTMLElement].classList.contains("delete-project-button"))
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
