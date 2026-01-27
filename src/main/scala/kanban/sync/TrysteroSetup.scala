package kanban.sync

import com.raquo.laminar.api.L.{*, given}
import org.scalajs.dom.{RTCConfiguration, RTCIceServer, RTCPeerConnection}
import typings.trystero.mod.*
import typings.trystero.mod


import scala.scalajs.js

object TrysteroSetup {
  private val eturn = new RTCIceServer:
    urls = js.Array(
      "stun:relay1.expressturn.com:443",
      "turn:relay1.expressturn.com:3478",
      "turn:relay1.expressturn.com:443"
    )
    username = "000000002080715755"
    credential = "XJd2kZ1y0x5Qv5c5eWTuNA0Lmho"

  private val tturn = new RTCIceServer:
    urls = "stun:stun.t-online.de:3478"

  private val rtcConf = new RTCConfiguration:
    iceServers = js.Array(eturn, tturn)

  private object DefaultConfig extends RelayConfig, BaseRoomConfig, TurnConfig {
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
}
