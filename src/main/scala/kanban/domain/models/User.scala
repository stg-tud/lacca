package kanban.domain.models

import org.getshaka.nativeconverter.{NativeConverter, ParseState}
import rdts.base.{Lattice, LocalUid, Uid}
import rdts.datatypes.LastWriterWins
import rdts.time.CausalTime

import scala.scalajs.js
import scala.scalajs.js.JSConverters.JSRichOption
import scala.scalajs.js.UndefOr

type UserId = Uid

case class User(
    id: UserId,
    name: LastWriterWins[String],
    age: LastWriterWins[Int],
    email: LastWriterWins[String],
    password: LastWriterWins[String]
) derives NativeConverter 

object User {
  def apply(
      name: String,
      age: Int,
      email: String,
      password: String
           ): User = {
    new User(
      id = Uid.gen(),
        name = LastWriterWins(CausalTime.now(), name),
        age = LastWriterWins(CausalTime.now(), age),
        email = LastWriterWins(CausalTime.now(), email),
        password = LastWriterWins(CausalTime.now(), password)
    )
  }

  /** Default user instance at the beginning to log in */
  def default(): User = new User(
    id = Uid.predefined("default-admin-uid"),  // fixed UID
    name = LastWriterWins(CausalTime.now(), "admin"),
    age = LastWriterWins(CausalTime.now(), 0),
    email = LastWriterWins(CausalTime.now(), "admin@gmail.com"),
    password = LastWriterWins(CausalTime.now(), "admin")
  )
  
  given NativeConverter[LocalUid] with {
    extension (a: LocalUid)
      override def toNative: js.Any =
          NativeConverter[Uid].toNative(a.uid)
    override def fromNative(ps: ParseState): LocalUid =
        LocalUid(NativeConverter[Uid].fromNative(ps))
  }
  given NativeConverter[Uid] with {
    extension (a: UserId)
      override def toNative: js.Any =
          a.delegate
    override def fromNative(ps: ParseState): UserId =
        Uid.predefined(ps.json.asInstanceOf[String])
  }
  given NativeConverter[Map[Uid, Int]] with {
    extension (a: Map[Uid, Int])
      override def toNative: js.Any =
          NativeConverter[Map[String, Int]].toNative(
            a.map((k,v) => (k.delegate, v))
          )
          
    override def fromNative(ps: ParseState): Map[UserId, Int] =
        NativeConverter[Map[String, Int]]
            .fromNative(ps.json)
            .map((k,v) => (Uid.predefined(k), v))
  }
  
  given Lattice[UserId] = Lattice.assertEquals
  given Lattice[User] = Lattice.derived
}
