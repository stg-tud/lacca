package kanban.domain.models

import org.getshaka.nativeconverter.{NativeConverter, ParseState}
import rdts.base.{Lattice, LocalUid, Uid}
import rdts.datatypes.{GrowOnlyCounter, LastWriterWins}
import rdts.time.CausalTime

import java.nio.charset.StandardCharsets
import scala.scalajs.js
import scala.scalajs.js.Date

type ProjectId = Uid

enum ProjectStatus derives NativeConverter:
  case Neu, Geplant, InArbeit, Abrechenbar, Abgeschlossen

case class Project(
    id: ProjectId,
    name: LastWriterWins[String],
    status: LastWriterWins[ProjectStatus],
    revisorId: LastWriterWins[UserId],
    deadline: LastWriterWins[Option[Date]],
    timeTracked: GrowOnlyCounter = GrowOnlyCounter.zero
) derives NativeConverter

object Project {
  def apply(
      name: String,
      status: ProjectStatus,
      revisorId: UserId,
      deadline: Option[Date]
  ): Project = {
    new Project(
      id = Uid.gen(),
      name = LastWriterWins(CausalTime.now(), name),
      status = LastWriterWins(CausalTime.now(), status),
      revisorId = LastWriterWins(CausalTime.now(), revisorId),
      deadline = LastWriterWins(CausalTime.now(), deadline)
    )
  }

  given NativeConverter[LocalUid] with {
    extension (a: LocalUid)
      override def toNative: js.Any =
        NativeConverter[Uid].toNative(a.uid)

    override def fromNative(ps: ParseState): LocalUid =
      LocalUid(NativeConverter[Uid].fromNative(ps))
  }
  given NativeConverter[Uid] with {
    extension (a: ProjectId)
      override def toNative: js.Any =
        a.delegate

    override def fromNative(ps: ParseState): ProjectId =
      Uid.predefined(ps.json.asInstanceOf[String])
  }
  given NativeConverter[Map[Uid, Int]] with {
    extension (a: Map[Uid, Int])
      override def toNative: js.Any =
        NativeConverter[Map[String, Int]].toNative(
          a.map((k, v) => (k.delegate, v))
        )

    override def fromNative(ps: ParseState): Map[ProjectId, Int] =
      NativeConverter[Map[String, Int]]
        .fromNative(ps.json)
        .map((k, v) => (Uid.predefined(k), v))
  }
  given NativeConverter[GrowOnlyCounter] = NativeConverter.derived

  given Lattice[ProjectId] = Lattice.assertEquals
  given Lattice[Project] = Lattice.derived
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
