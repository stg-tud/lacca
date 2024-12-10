package kanban.models

import scala.scalajs.js
import scala.scalajs.js.Date

type ProjectId = String

enum ProjectStatus:
  case Neu, Geplant, InArbeit, Abrechenbar, Abgeschlossen

case class Project(
    id: ProjectId,
    name: String,
    status: ProjectStatus,
    revisorId: UserId,
    deadline: Option[Date],
    timeTracked: Double = 0
) {

  def toJsObject: ProjectJsObject = {
    js.Dynamic
      .literal(
        id = this.id,
        name = this.name,
        status = this.status.toString,
        revisorId = this.revisorId,
        deadline = this.deadline.toString,
        timeTracked = this.timeTracked
      )
      .asInstanceOf[ProjectJsObject]
  }
}

trait ProjectJsObject extends js.Object {
  val id: String
  val name: String
  val status: String
  val revisorId: String
  val deadline: js.Date
  val timeTracked: Double
}
