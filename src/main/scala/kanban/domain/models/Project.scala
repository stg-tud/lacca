package kanban.domain.models

import org.getshaka.nativeconverter.{NativeConverter, ParseState}
import rdts.base.{Lattice, LocalUid, Uid}
import rdts.datatypes.{GrowOnlyCounter, LastWriterWins}
import rdts.time.CausalTime

import scala.scalajs.js
import scala.scalajs.js.Date

type ProjectId = Uid

enum ProjectStatus derives NativeConverter:
  case Neu, Geplant, InArbeit, Abrechenbar, Abgeschlossen

enum Permission derives NativeConverter:
  case None, Read, Write

case class UserPermission(
    userId: UserId,
    permission: Permission
) derives NativeConverter

case class Project(
    id: ProjectId,
    name: LastWriterWins[String],
    status: LastWriterWins[ProjectStatus],
    revisorId: LastWriterWins[UserId],
    deadline: LastWriterWins[Option[Date]],
    timeTracked: GrowOnlyCounter = GrowOnlyCounter.zero,
    permittedUsers: Option[LastWriterWins[Set[UserPermission]]] = None,
    deleted: Option[LastWriterWins[Boolean]] = Some(LastWriterWins(CausalTime.now(), false))
) derives NativeConverter

object Project {
  def apply(
      name: String,
      status: ProjectStatus,
      revisorId: UserId,
      deadline: Option[Date],
      permittedUsers: Option[Set[UserPermission]]
  ): Project = {
    new Project(
      id = Uid.gen(),
      name = LastWriterWins(CausalTime.now(), name),
      status = LastWriterWins(CausalTime.now(), status),
      revisorId = LastWriterWins(CausalTime.now(), revisorId),
      deadline = LastWriterWins(CausalTime.now(), deadline),
      permittedUsers = permittedUsers.map(set => LastWriterWins(CausalTime.now(), set)),
      deleted = Some(LastWriterWins(CausalTime.now(), false))
    )
  }

  // JSON conversion
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

  given NativeConverter[Date] with {
    extension (a: Date) override def toNative: js.Any = a.toISOString()

    override def fromNative(ps: ParseState): Date =
      new Date(ps.json.asInstanceOf[String])
  }

  given NativeConverter[Option[Date]] with {
    extension (a: Option[Date])
      override def toNative: js.Any = a match {
        case Some(date) => date.toISOString()
        case None       => null
      }

    override def fromNative(ps: ParseState): Option[Date] =
      if (ps.json == null) None
      else Some(new Date(ps.json.asInstanceOf[String]))
  }

  // TODO: check if is there any errors left
  given NativeConverter[Set[UserPermission]] with {
    extension (a: Set[UserPermission])
      override def toNative: js.Any =
        NativeConverter[Seq[UserPermission]].toNative(a.toSeq)

    override def fromNative(ps: ParseState): Set[UserPermission] =
      try {
        ps.json match
          case arr if js.Array.isArray(arr) =>
            // handle array
            NativeConverter[Seq[UserPermission]]
              .fromNative(arr)
              .toSet

          case other if js.typeOf(other) == "string" =>
            val s = other.asInstanceOf[String]
            println(s"⚠️ Received string instead of array: $s — interpreting as old format UserId")
            Set(UserPermission(Uid.predefined(s), Permission.Read)) // fallback

          case _ =>
            println(s"⚠️ Unexpected permittedUsers format: ${ps.json}")
            Set.empty

      } catch {
        case e: Exception =>
          println(s"❌ Failed to parse Set[UserPermission]: ${e.getMessage}")
          Set.empty
      }
  }

  // CRDT lattices
  given Lattice[ProjectId] = Lattice.assertEquals
  given Lattice[Project] = Lattice.derived
}
