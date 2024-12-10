package kanban.models

import scala.scalajs.js

type UserId = String

case class User(
    id: UserId,
    name: String,
    age: Int,
    email: String
) {
  def toJsObject: UserJsObject = {
    js.Dynamic
      .literal(
        id = this.id,
        name = this.name,
        age = this.age,
        email = this.email
      )
      .asInstanceOf[UserJsObject]
  }
}

trait UserJsObject extends js.Object {
  val id: String
  val name: String
  val age: Int
  val email: String
}
