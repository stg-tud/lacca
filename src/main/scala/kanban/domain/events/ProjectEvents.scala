package kanban.domain.events

import kanban.domain.models.{Project, ProjectId, ProjectStatus}
import com.raquo.laminar.api.L.{*, given}

sealed trait ProjectEvent

object ProjectEvent {
    case class Added(project: Project) extends ProjectEvent
    case class Updated(id: ProjectId, updatedProject: Project) extends ProjectEvent
    case class Deleted(id: ProjectId) extends ProjectEvent
    case class StatusModified(id: ProjectId, newStatus: ProjectStatus) extends ProjectEvent
    case class ClickedOn(projectSignal: Signal[Project]) extends ProjectEvent
}