package kanban.persistence

import scala.scalajs.js
import scala.scalajs.js.annotation.*

@js.native
@JSImport("trystero", JSImport.Namespace)
object TrysteroJS extends js.Object {
  def join(namespace: String, options: js.UndefOr[js.Object] = js.undefined): TrysteroRoom = js.native
}

@js.native
trait TrysteroRoom extends js.Object {
  def makeAction(action: String): js.Function1[js.Any, Unit] = js.native
  def onAction(action: String, callback: js.Function1[js.Any, Unit]): Unit = js.native
}

object Trystero {
  def join(namespace: String): TrysteroRoom = TrysteroJS.join(namespace)
}


