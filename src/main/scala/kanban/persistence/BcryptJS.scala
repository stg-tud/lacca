package kanban.persistence

import scala.scalajs.js
import scala.scalajs.js.annotation._

@js.native
@JSImport("bcryptjs", JSImport.Namespace)
object BcryptJS extends js.Object {
  // Method to hash a password with salt rounds
  def hashSync(data: String, saltRounds: Int): String = js.native
  
  // Method to compare a plain text password with a hashed one
  def compareSync(data: String, hash: String): Boolean = js.native
}

