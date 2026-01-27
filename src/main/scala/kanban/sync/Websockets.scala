package kanban.sync

import kanban.domain.models.{Project, User}
import kanban.sync.UpdateType.{Join, ReplicaUpdate, TokenUpdate, UserUpdate}
import org.getshaka.nativeconverter.NativeConverter
import org.scalajs.dom.WebSocket
import rdts.base.Uid

object Websockets {
  private val socket = WebSocket("ws://localhost:8050/ws")

  var projectCallback: Project => Unit = _ => ()
  var tokenCallback: (String, String, String) => Unit = (_,_,_) => ()
  var joinCallback: Uid => Unit = _ => ()
  var userCallback: User => Unit = _ => ()
  var replicaCallback: (String, String, String) => Unit = (_,_,_) => ()

  socket.onmessage = {
    event =>
      val incoming = NativeConverter[WebsocketsUpdate].fromJson(event.data.asInstanceOf[String])
      incoming.kind match {
        case UpdateType.ProjectUpdate =>
          val project = NativeConverter[Project].fromJson(incoming.payload)
          projectCallback(project)
        case TokenUpdate => 
          val projectId :: userId :: token :: Nil = incoming.payload.split(",").toList : @unchecked
          tokenCallback(projectId, userId, token)
        case Join =>
          joinCallback(Uid.predefined(incoming.payload))
        case UserUpdate =>
          userCallback(NativeConverter[User].fromJson(incoming.payload))
        case ReplicaUpdate =>
          val userId :: replicaId :: publicKey :: Nil = incoming.payload.split(",").toList : @unchecked
          replicaCallback(userId, replicaId, publicKey)
      }
  }
  
  def sendUpdate(t: UpdateType, payload: String) =
    socket.send(WebsocketsUpdate(t, payload).toJson)
    
  socket.onopen = {
    _ =>
      sendUpdate(Join, "hi!")
  }
    
}

enum UpdateType:
  case ProjectUpdate
  case TokenUpdate
  case Join
  case UserUpdate
  case ReplicaUpdate

case class WebsocketsUpdate(
    kind: UpdateType,
    payload: String
)

given NativeConverter[WebsocketsUpdate] = NativeConverter.derived
