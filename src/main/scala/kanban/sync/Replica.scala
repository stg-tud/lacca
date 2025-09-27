package kanban.sync

import com.raquo.airstream.state.Var
import kanban.domain.models.Project.given
import kanban.persistence.DexieDB.dexieDB
import org.getshaka.nativeconverter.NativeConverter
import rdts.base.LocalUid
import typings.dexie.mod.Table

import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js
import scala.util.{Failure, Success}
import kanban.auth.KeyMaterialSingleton
import ucan.Base32

object Replica {
  val keyMaterial = KeyMaterialSingleton.keyMaterial

  println(s"Initializing Replica... ${keyMaterial.now()}")

  private val replicaIdTable: Table[replicaDBEntry, Int, replicaDBEntry] =
    dexieDB.table("replicas")

  val id: Var[Option[LocalUid]] = Var(None)

  trait replicaDBEntry extends js.Object:
    val slot: Int
    val localUid: js.Any
    val publicKey: String

  replicaIdTable
    .get(0)
    .toFuture
    .map(_.toOption)
    .onComplete {
      case Failure(f) => println(f)
      case Success(Some(value)) =>
        id.set(Some(NativeConverter[LocalUid].fromNative(value.localUid)))
        println(s"Found replicaId ${id.now().get.show} in database with publicKey ${value.publicKey}")
        // update publicKey if keyMaterial is available
        keyMaterial.now().foreach { km =>
          val pk = Base32.encode(km.publicKey)
          val updatedEntry = new replicaDBEntry:
            val slot: Int = 0
            val localUid: js.Any = value.localUid
            val publicKey: String = pk
          replicaIdTable.put(updatedEntry)
        }

      case Success(None) =>
        val newId = LocalUid.gen()
        id.set(Some(newId))
        keyMaterial.now().foreach { km =>
          val pk = Base32.encode(km.publicKey)
          val entry = new replicaDBEntry:
            val slot: Int = 0
            val localUid: js.Any = newId.toNative
            val publicKey: String = pk
          replicaIdTable.add(entry)
          println(s"Generated new replicaId $newId with publicKey $pk")
        }
    }
}