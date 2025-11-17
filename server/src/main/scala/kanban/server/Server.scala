package kanban.server

import org.http4s.ember.server.EmberServerBuilder
import com.comcast.ip4s.*
import cats.effect.kernel.Async
import cats.effect.std.Queue
import cats.syntax.all.*
import fs2.concurrent.Topic
import fs2.io.net.Network
import fs2.io.file.Files
import org.http4s.server.staticcontent.{FileService, fileService}
import org.http4s.websocket.WebSocketFrame

object Server {

  def server[F[_]: {Async, Files, Network}](q: Queue[F, WebSocketFrame], t: Topic[F, WebSocketFrame]): F[Unit] = {

    val host = host"0.0.0.0"

    val port = port"8050"

    EmberServerBuilder
      .default[F]
      .withHost(host)
      .withPort(port)
//      .withHttpApp(fileService[F](FileService.Config("./static")).orNotFound)
      .withHttpWebSocketApp(wsb => new Routes().service(wsb, q, t))
      .withHttp2
      .build
      .useForever
      .void
  }

}
