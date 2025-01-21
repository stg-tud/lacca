package kanban.domain.models

import kanban.domain.models.UserId
import scala.scalajs.js
import scala.scalajs.js.Date
import scala.scalajs.js.JSConverters.JSRichOption

type ProjectId = Option[Int]

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
                id = this.id.orUndefined,
                name = this.name,
                status = this.status.toString,
                revisorId = this.revisorId.orUndefined,
                deadline = this.deadline.orUndefined,
                timeTracked = this.timeTracked
            )
            .asInstanceOf[ProjectJsObject]
    }
}

trait ProjectJsObject extends js.Object {
    val id: js.UndefOr[Int]
    val name: String
    val status: String
    val revisorId: js.UndefOr[Int]
    val deadline: js.UndefOr[Date]
    val timeTracked: Double
}
