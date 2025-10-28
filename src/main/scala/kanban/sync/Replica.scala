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
import kanban.ui.views.GlobalState
import com.raquo.airstream.ownership.ManualOwner

object Replica {
  implicit val owner: ManualOwner = new ManualOwner
  val keyMaterial = KeyMaterialSingleton.keyMaterial

  val replicaIdTable: Table[replicaDBEntry, Int, replicaDBEntry] =
    dexieDB.table("replicas")

  val id: Var[Option[LocalUid]] = Var(None)

  trait replicaDBEntry extends js.Object:
    val slot: Int
    val localUid: js.Any
    val publicKey: String
    val userId: String

  // use slot = 0 only as a default for the first replica
  val defaultSlot = 0

  // Only initialize replica after login
  // Only run after a userId is available
    GlobalState.userIdVar.signal.foreach {
      case Some(userId) =>
        // initialize replica for this user
        println(s"[Replica] Initializing replica for userId = $userId")

        replicaIdTable
          .get(defaultSlot)
          .toFuture
          .map(_.toOption)
          .onComplete {
            case Failure(f) =>
              println(s"[$userId] Failed to get replica: ${f.getMessage}")

            case Success(Some(value)) =>
              id.set(Some(NativeConverter[LocalUid].fromNative(value.localUid)))
              println(
                s"With the user id: [$userId] found replicaId ${id.now().get.show} in database with publicKey ${value.publicKey}"
              )
              keyMaterial.now().foreach { km =>
                val pk = Base32.encode(km.publicKey)
                val updatedEntry = js.Dynamic.literal(
                  slot = value.slot,
                  localUid = value.localUid,
                  publicKey = pk,
                  userId = userId
                  ).asInstanceOf[replicaDBEntry]
                replicaIdTable.put(updatedEntry)
              }

            case Success(None) =>
              val newId = LocalUid.gen()
              id.set(Some(newId))
              val uid = userId // ensure to have the right userId, not null
              keyMaterial.now().foreach { km =>
                val pk = Base32.encode(km.publicKey)
                // dynamically choose next slot
                replicaIdTable.count().toFuture.foreach { count =>
                  val newSlot = count.toInt
                  val entry = js.Dynamic.literal(
                    slot = newSlot,
                    localUid = newId.toNative,
                    publicKey = pk,
                    userId = uid
                    ).asInstanceOf[replicaDBEntry]
                  replicaIdTable.add(entry)
                  println(
                    s"With the user id: [$userId], generated new replicaId $newId with publicKey $pk at slot $newSlot"
                  )
                }
              }
          }

      case None =>
        // not logged in yet
        println("Not logged in — replicaId will not be created until login.")
    }
}
