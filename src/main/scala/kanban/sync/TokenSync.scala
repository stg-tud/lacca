package kanban.sync

import typings.trystero.mod.*
import kanban.sync.TrysteroSetup.room
import scala.scalajs.js
import scala.scalajs.js.JSConverters.*

object TokenSync {
  /** Token message sent between peers */
  trait TokenMessage extends js.Object:
    val projectId: String
    val userId: String
    val token: String

  object TokenMessage {
    def apply(projectId: String, userId: String, token: String): TokenMessage =
      js.Dynamic.literal(
        projectId = projectId,
        userId = userId,
        token = token
      ).asInstanceOf[TokenMessage]
  }

  // Trystero action for sending/receiving tokens
  private val tokenActions = room.makeAction[TokenMessage]("ucanTok")
  private val _sendToken: ActionSender[TokenMessage] = tokenActions._1
  private val _receiveToken: ActionReceiver[TokenMessage] = tokenActions._2

  /** Send token to all peers or a subset */
  def sendToken(
      projectId: String,
      userId: String,
      token: String,
      targetPeers: List[String] = Nil
  ): Unit =
    val msg = TokenMessage(projectId, userId, token)
    println(s"[TokenSync] Sending token -> projectId: $projectId, userId: $userId, token: $token")
    if targetPeers.isEmpty then
      _sendToken(msg)
    else
      _sendToken(data = msg, targetPeers = targetPeers.toJSArray)

  /** Register a callback when a peer sends a token */
  def receiveToken(callback: (String, String, String) => Unit): Unit =
    _receiveToken { (data: TokenMessage, peerId: String, metaData) =>
      // safely unwrap, in case something went wrong
      val projectId = Option(data.projectId).getOrElse("null")
      val userId    = Option(data.userId).getOrElse("null")
      val token     = Option(data.token).getOrElse("null")
      println(s"[TokenSync] Received token success")
      callback(projectId, userId, token)
    }
}