package kanban.sync

import com.raquo.laminar.api.L.{*, given}
import kanban.sync.TrysteroSetup.{room, updatePeers}
import typings.trystero.mod.*
import scala.scalajs.js
import scala.scalajs.js.JSConverters.*

object ReplicaSync {
  trait ReplicaInfo extends js.Object:
    val replicaId: String
    val publicKey: String

  // Setup Trystero action for exchanging ReplicaInfo
  private val replicaActions = room.makeAction[ReplicaInfo]("replicaInfo")
  private val _sendReplicaInfo: ActionSender[ReplicaInfo] = replicaActions._1
  private val _receiveReplicaInfo: ActionReceiver[ReplicaInfo] = replicaActions._2

  // Update peer list whenever we receive a replica info message
  _receiveReplicaInfo((_, _, _) => updatePeers())

  /** Send your replica’s public key to everyone or specific peers */
  def sendReplicaInfo(replicaId: String, publicKey: String, targetPeers: List[String] = List.empty): Unit =
    // Use js.Dynamic.literal to create a proper JS object
    val info = js.Dynamic.literal(
      replicaId = replicaId,
      publicKey = publicKey
    ).asInstanceOf[ReplicaInfo]

    if targetPeers.isEmpty then
      println(s"[ReplicaSync] Broadcasting replica $replicaId with pk=$publicKey to all peers")
      _sendReplicaInfo(info)
    else
      println(s"[ReplicaSync] Sending replica $replicaId with pk=$publicKey to peers: ${targetPeers.mkString(", ")}")
      _sendReplicaInfo(info, targetPeers = targetPeers.toJSArray)

  /** Listen for incoming replica public keys */
  def receiveReplicaInfo(callback: (replicaId: String, publicKey: String, peerId: String) => Unit): Unit =
    _receiveReplicaInfo((data: ReplicaInfo, peerId: String, metaData) =>
      val rid = Option(data.replicaId).getOrElse("unknown")
      val pk  = Option(data.publicKey).getOrElse("unknown")
      println(s"[ReplicaSync] Received replica $rid with pk=$pk from peer $peerId")
      callback(rid, pk, peerId)
    )
}