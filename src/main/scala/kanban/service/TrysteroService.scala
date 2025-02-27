package kanban.service

import org.scalajs.dom.{RTCConfiguration, RTCIceServer}
import typings.trystero.mod.*

import scala.scalajs.js

object TrysteroService {
  object eturn extends RTCIceServer:
    urls = js.Array(
      "stun:relay1.expressturn.com:3478",
      "turn:relay1.expressturn.com:3478"
    )
    username = "efMS8M021S1G8NJ8J7"
    credential = "qrBXTlhKtCJDykOK"
  object tturn extends RTCIceServer:
    urls = js.Array("stun:stun.t-online.de:3478")
  object rconfig extends RTCConfiguration:
    iceServers = js.Array(eturn, tturn)
  object DefaultConfig extends RelayConfig, BaseRoomConfig {
    var appId = "lacca_test_1270"
    rtcConfig = rconfig
  }

  val room = joinRoom(DefaultConfig, "testroom")

  // setup actions
  private val actions: js.Tuple3[ActionSender[String], ActionReceiver[
    String
  ], ActionProgress] = room.makeAction[String]("message")
  val receiveMessage: ActionReceiver[String] = actions._2
  val sendMessage: ActionSender[String] = actions._1

  // listen for incoming messages
  println(s"my peer ID is $selfId")
  room.onPeerJoin(peerId => println(s"$peerId joined"))
  room.onPeerLeave(peerId => println(s"$peerId left"))
  receiveMessage((data, peerId, metaData) => println(s"got $data from $peerId"))
}
