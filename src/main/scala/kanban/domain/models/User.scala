package kanban.domain.models

import scala.scalajs.js
import scala.scalajs.js.JSConverters.JSRichOption
import scala.scalajs.js.UndefOr

type UserId = Option[Int]

case class User(
                   id: UserId,
                   name: String,
                   age: Int,
                   email: String,
                   password: String
               ) {
    def toJsObject: UserJsObject = {
        js.Dynamic
            .literal(
                id = this.id.orUndefined,
                name = this.name,
                age = this.age,
                email = this.email,
                password = this.password
            )
            .asInstanceOf[UserJsObject]
    }
}

trait UserJsObject extends js.Object {
    val id: UndefOr[Int]
    val name: String
    val age: Int
    val email: String
    val password: String
}
