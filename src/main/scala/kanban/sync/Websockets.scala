package kanban.sync

import org.scalajs.dom.WebSocket

object Websockets {
  val socket = WebSocket("ws://localhost:8050/ws")

}
