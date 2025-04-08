package kanban.ui.components

import com.raquo.laminar.api.L.{*, given}
import kanban.sync.TrysteroSetup

object UserCounter {
  val numPeers: Signal[Int] = TrysteroSetup.peerList.signal.map(_.size)
  val peerNames: Signal[List[String]] =
    TrysteroSetup.peerList.signal.map(_.map(_._1))
  def apply(): HtmlElement = {
    div(
      span(
        text <-- TrysteroSetup.userId.signal.map(id => s"ID: $id")
      ),
      " | ",
      span(
        text <-- numPeers.map(userNum => s"Nutzer*innen online: $userNum"),
        title <-- peerNames.map(peers =>
          s"Peers: ${if peers.isEmpty then "None" else peers.mkString(", ")}"
        )
      )
    )
  }
}
