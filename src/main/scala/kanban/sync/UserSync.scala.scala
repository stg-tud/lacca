package kanban.sync

import scala.scalajs.js
import scala.scalajs.js.JSConverters.*
import kanban.sync.TrysteroSetup.{room, updatePeers}
import typings.trystero.mod.ActionSender
import typings.trystero.mod.ActionReceiver
import kanban.domain.models.User
import org.getshaka.nativeconverter.NativeConverter

object UserSync {
  private trait UserUpdate extends js.Object:
    val id: String
    val payload: js.Any

  private val userActions = room.makeAction[UserUpdate]("uUp")
  private val _sendUserUpdate: ActionSender[UserUpdate] =
    userActions._1

  private val _receiveUserUpdate: ActionReceiver[UserUpdate] =
    userActions._2

  // TODO: Should this method be called here?
  _receiveUserUpdate((_, _, _) =>
    updatePeers()
  ) // update peer list when we receive updates

  def sendUserUpdate(
      user: User,
      targetPeers: List[String] = List.empty
  ): Unit =
    println("sending user update")
    val update = new UserUpdate {
      val id = user.id.delegate
      val payload = user.toNative
    }
    if targetPeers.isEmpty then _sendUserUpdate(update)
    else _sendUserUpdate(data = update, targetPeers = targetPeers.toJSArray)

  def receiveUserUpdate(callback: User => Unit): Unit =
    _receiveUserUpdate((data: UserUpdate, peerId: String, metaData) =>
      val incoming = NativeConverter[User].fromNative(data.payload)
      callback(incoming)
    )
}
