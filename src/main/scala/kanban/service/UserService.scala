package kanban.service

import kanban.dexie.DexieDB.dexieDB
import kanban.models.{User, UserJsObject}
import org.scalablytyped.runtime.StringDictionary
import typings.dexie.mod.{Dexie, Table, UpdateSpec}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.scalajs.js

object UserService {

  private val usersTable: Table[UserJsObject, String, UserJsObject] =
    dexieDB.table("users")

  def createUser(user: User): Future[Any] = {
    usersTable.add(user.toJsObject).toFuture
  }

  def getAllUsers(): Future[Seq[User]] = {
    usersTable.toArray().toFuture.map { usersJsArray =>
      usersJsArray.map { userJsObject =>
        User(
          id = userJsObject.id.toOption,
          name = userJsObject.name,
          age = userJsObject.age,
          email = userJsObject.email
        )
      }.toSeq
    }
  }
}
