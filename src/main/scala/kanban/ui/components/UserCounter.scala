package kanban.ui.components

import com.raquo.laminar.api.L.{*, given}
import kanban.service.TrysteroService

object UserCounter {
  val numPeers: Signal[Int] = TrysteroService.peerList.signal.map(_.size)
  val peerNames: Signal[List[String]] =
    TrysteroService.peerList.signal.map(_.map(_._1))
  def apply(): HtmlElement = {
    div(
      span(
        text <-- TrysteroService.userId.signal.map(id => s"ID: $id")
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
