package kanban.sync

import com.raquo.laminar.api.L.{*, given}
import kanban.domain.models.Project
import org.getshaka.nativeconverter.NativeConverter
import kanban.sync.TrysteroSetup.{room, updatePeers}
import typings.trystero.mod.*

import scala.scalajs.js
import scala.scalajs.js.JSConverters.*
import scala.scalajs.js.JSON

object ProjectSync {
  private trait ProjectUpdate extends js.Object:
    val id: String
    val payload: js.Any

  private val projectActions = room.makeAction[ProjectUpdate]("pUp")
  private val _sendProjectUpdate: ActionSender[ProjectUpdate] =
    projectActions._1

  private val _receiveProjectUpdate: ActionReceiver[ProjectUpdate] =
    projectActions._2

  _receiveProjectUpdate((_, _, _) =>
    updatePeers()
  ) // update peer list when we receive updates

  def sendProjectUpdate(
                         project: Project,
                         targetPeers: List[String] = List.empty
                       ): Unit = {

    println("sending project update")
    val update = new ProjectUpdate {
      val id = project.id.delegate
      val payload = project.toNative
    }
    // send project update via trystero
    if targetPeers.isEmpty then _sendProjectUpdate(update)
    else _sendProjectUpdate(data = update, targetPeers = targetPeers.toJSArray)

    // send project update via websockets
    Websockets.socket.send(JSON.stringify(update))
  }


  def receiveProjectUpdate(callback: Project => Unit): Unit = {
    // receive updates via trystero
    _receiveProjectUpdate((data: ProjectUpdate, peerId: String, metaData) =>
      val incoming = NativeConverter[Project].fromNative(data.payload)
      callback(incoming)
    )
    // TODO: receive updates via websockets
  }
}
