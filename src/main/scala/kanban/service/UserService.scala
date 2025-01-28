package kanban.service

import kanban.domain.models.{User, UserId, UserJsObject}
import kanban.persistence.DexieDB.dexieDB
import org.scalablytyped.runtime.StringDictionary
import typings.dexie.mod.{Dexie, Table, UpdateSpec, liveQuery}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.scalajs.js

object UserService {

  private val usersTable: Table[UserJsObject, Int, UserJsObject] =
    dexieDB.table("users")

  def createUser(user: User): Future[Any] = {
    usersTable.add(user.toJsObject).toFuture
  }

  def getAllUsers(): Future[Seq[User]] = {
    println(s"getAllUsers called!!")
    usersTable.toArray().toFuture.map { usersJsArray =>
      usersJsArray.map { userJsObject =>
        fromJsObject(userJsObject)
      }.toSeq
    }
  }
  val usersObservable = liveQuery(() => getAllUsers())
  
  def deleteUser(id: UserId): Future[Unit] = {
    usersTable.delete(id.getOrElse{
        println(s"userId is not defined!!")
        0
    }).toFuture
  }

  def fromJsObject(userJsObject: UserJsObject): User = {
    User(
      id = userJsObject.id.toOption,
      name = userJsObject.name,
      age = userJsObject.age,
      email = userJsObject.email,
      password = userJsObject.password
    )
  }
}
