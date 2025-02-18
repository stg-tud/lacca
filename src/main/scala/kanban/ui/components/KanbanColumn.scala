package kanban.ui.components

import com.raquo.laminar.api.L.{*, given}
import kanban.domain.models.Project
import kanban.ui.components.ProjectCard

object KanbanColumn {
    def apply(title: String, projects: Signal[List[Project]]): HtmlElement = {
        div(
            cls := "kanban-column",
            h3(
                cls := "kanban-column-header", title
            ),
            div(
                cls := "kanban-column-content",
                idAttr := s"column-$title",
                child <-- projects.map(_.nonEmpty).map {
                  case true => div(children <-- projects.split(_.id)((id, initial, projectSignal) => ProjectCard(id, projectSignal)))
                  case false => div(cls := "empty-column")
                }
            )
        )
    }
}
