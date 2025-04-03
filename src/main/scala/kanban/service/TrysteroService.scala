package kanban.service

import com.raquo.laminar.api.L.{*, given}
import kanban.domain.models.Project
import org.getshaka.nativeconverter.NativeConverter
import org.scalajs.dom.{RTCConfiguration, RTCIceServer, RTCPeerConnection}
import typings.trystero.mod
import typings.trystero.mod.*

import scala.scalajs.js
import scala.scalajs.js.JSConverters.*

object TrysteroService {
  private val eturn = new RTCIceServer:
    urls = js.Array(
      "stun:relay1.expressturn.com:443",
      "turn:relay1.expressturn.com:3478",
      "turn:relay1.expressturn.com:443"
    )
    username = "efMS8M021S1G8NJ8J7"
    credential = "qrBXTlhKtCJDykOK"
  private val tturn = new RTCIceServer:
    urls = "stun:stun.t-online.de:3478"
  private val rtcConf = new RTCConfiguration:
    iceServers = js.Array(eturn, tturn)

  private object DefaultConfig extends RelayConfig, BaseRoomConfig {
    var appId = "lacca_test_1270"
    rtcConfig = rtcConf
  }

  // Public API
  val room: Room = joinRoom(DefaultConfig, "testroom")
  val peerList: Var[List[(String, RTCPeerConnection)]] = Var(List.empty)
  val userId: Var[String] = Var(selfId)

  // setup actions
  private val actions: js.Tuple3[ActionSender[String], ActionReceiver[
    String
  ], ActionProgress] = room.makeAction[String]("message")
  val receiveMessage: ActionReceiver[String] = actions._2
  val sendMessage: ActionSender[String] = actions._1

  // listen for incoming messages
  println(s"my peer ID is $selfId")
  room.onPeerJoin(peerId =>
    println(s"$peerId joined")
    updatePeers()
  )
  room.onPeerLeave(peerId =>
    println(s"$peerId left")
    updatePeers()
  )
  receiveMessage((data, peerId, metaData) => println(s"got $data from $peerId"))

  def updatePeers(): Unit =
    peerList.set(room.getPeers().toList)

  // everything related to sending and receiving projects starts here
  private trait ProjectUpdate extends js.Object:
    val id: String
    val payload: js.Any // the native project json object

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
  ): Unit =
    println("sending project update")
    val update = new ProjectUpdate {
      val id = project.id.delegate
      val payload = project.toNative
    }
    if targetPeers.isEmpty then _sendProjectUpdate(update)
    else _sendProjectUpdate(data = update, targetPeers = targetPeers.toJSArray)

  def receiveProjectUpdate(callback: Project => Unit): Unit =
    _receiveProjectUpdate((data: ProjectUpdate, peerId: String, metaData) =>
      val incoming = NativeConverter[Project].fromNative(data.payload)
      callback(incoming)
    )

}
