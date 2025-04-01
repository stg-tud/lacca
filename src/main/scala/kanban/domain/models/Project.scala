package kanban.domain.models

import scala.scalajs.js
import scala.scalajs.js.Date
import rdts.base.Uid
import rdts.datatypes.LastWriterWins
import rdts.time.CausalTime

type ProjectId = Uid

enum ProjectStatus:
  case Neu, Geplant, InArbeit, Abrechenbar, Abgeschlossen

case class Project(
    id: ProjectId,
    name: LastWriterWins[String],
    status: LastWriterWins[ProjectStatus],
    revisorId: LastWriterWins[Uid],
    deadline: LastWriterWins[Option[Date]],
    timeTracked: LastWriterWins[Double] = LastWriterWins(CausalTime.now(), 0.0)
) {
  // TODO: REVIEW THE MERGE FUNCTION
  // Should project be a CRDT?
  def merge(other: Project): Project = {
    if (id == other.id) {
      Project(
        id,
        name.merge(other.name),
        status.merge(other.status),
        revisorId.merge(other.revisorId),
        deadline.merge(other.deadline),
        timeTracked.merge(other.timeTracked)
      )
    } else {
      this
    }
  }

}

object Project {
  def apply(
      name: String,
      status: ProjectStatus,
      revisorId: Uid,
      deadline: Option[Date]
  ): Project = {
    Project(
      id = Uid.gen(),
      name = LastWriterWins(CausalTime.now(), name),
      status = LastWriterWins(CausalTime.now(), status),
      revisorId = LastWriterWins(CausalTime.now(), revisorId),
      deadline = LastWriterWins(CausalTime.now(), deadline),
      timeTracked = LastWriterWins(CausalTime.now(), 0.0)
    )
  }

  def fromJsObject(
      projectJsObject: ProjectJsObject
  ): Project = {
    Project(
      id = Uid.predefined(projectJsObject.id),
      name = LastWriterWins(
        CausalTime(
          projectJsObject.name.timestamp.toLong,
          0,
          Long.MinValue
        ),
        projectJsObject.name.payload
      ),
      status = LastWriterWins(
        CausalTime(
          projectJsObject.status.timestamp.toLong,
          0,
          Long.MinValue
        ),
        ProjectStatus.valueOf(projectJsObject.status.payload)
      ),
      revisorId = LastWriterWins(
        CausalTime(
          projectJsObject.revisorId.timestamp.toLong,
          0,
          Long.MinValue
        ),
        Uid.predefined(projectJsObject.revisorId.payload)
      ),
      deadline = LastWriterWins(
        CausalTime(
          projectJsObject.deadline.timestamp.toLong,
          0,
          Long.MinValue
        ),
        projectJsObject.deadline.payload.toOption.map { dateString =>
          new Date(dateString.toString)
        }
      ),
      timeTracked = LastWriterWins(
        CausalTime(
          projectJsObject.timeTracked.timestamp.toLong,
          0,
          Long.MinValue
        ),
        projectJsObject.timeTracked.payload
      )
    )
  }

  def toJsObject(project: Project): ProjectJsObject = {
    js.Dynamic
      .literal(
        id = project.id.toString,
        name = js.Dynamic.literal(
          timestamp = project.name.timestamp.time.toString,
          payload = project.name.read
        ),
        status = js.Dynamic.literal(
          timestamp = project.status.timestamp.time.toString,
          payload = project.status.read.toString
        ),
        revisorId = js.Dynamic.literal(
          timestamp = project.revisorId.timestamp.time.toString,
          payload = project.revisorId.read.toString
        ),
        deadline = js.Dynamic.literal(
          timestamp = project.deadline.timestamp.time.toString,
          payload = project.deadline.read.toString
          // TODO: REMOVE FOLLOWING COMMENTED CODE

          // payload = this.deadline.read.map(_.toString).getOrElse("")
        ),
        timeTracked = js.Dynamic.literal(
          timestamp = project.timeTracked.timestamp.time.toString,
          payload = project.timeTracked.read
        )
        // TODO: REMOVE FOLLOWING COMMENTED CODE

        // name = this.name.read.toString,
        // status = this.status.read.toString,
        // revisorId = this.revisorId.read.toString,
        // deadline = this.deadline.read.toString
      )
      .asInstanceOf[ProjectJsObject]
  }
}

trait NameLWW extends js.Object {
  val timestamp: String
  val payload: String
}

trait StatusLWW extends js.Object {
  val timestamp: String
  val payload: String
}

trait RevisorIdLWW extends js.Object {
  val timestamp: String
  val payload: String
}

trait DeadlineLWW extends js.Object {
  val timestamp: String
  val payload: js.UndefOr[Date]
}

trait TimeTrackedLWW extends js.Object {
  val timestamp: String
  val payload: Double
}

trait ProjectJsObject extends js.Object {
  val id: String
  val name: NameLWW
  val status: StatusLWW
  val revisorId: RevisorIdLWW
  val deadline: DeadlineLWW
  val timeTracked: TimeTrackedLWW
}
