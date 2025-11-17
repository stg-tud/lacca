package kanban.server

import cats.effect.IOApp
import cats.effect.IO
import Server.server
import cats.effect.std.Queue
import org.http4s.websocket.WebSocketFrame
import fs2.Stream
import fs2.concurrent.Topic
import scala.concurrent.duration.DurationInt

object Main extends IOApp.Simple {
  override def run: IO[Unit] = {
    for {
      q <- Queue.unbounded[IO, WebSocketFrame]
      t <- Topic[IO, WebSocketFrame]
      s <- Stream(
        Stream.fromQueueUnterminated(q).through(t.publish),
        Stream.awakeEvery[IO](30.seconds).map(_ => WebSocketFrame.Ping()).through(t.publish),
        Stream.eval(server[IO](q,t))
      ).parJoinUnbounded.compile.drain
    } yield s
  }
}
