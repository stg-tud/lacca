package kanban.service

import kanban.domain.models.{User, UserId, UserJsObject}
import kanban.persistence.DexieDB.dexieDB
import org.scalablytyped.runtime.StringDictionary
import typings.dexie.mod.{Dexie, Table, UpdateSpec, liveQuery}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.scalajs.js
import kanban.persistence.BcryptJS

object UserService {

  private val usersTable: Table[UserJsObject, Int, UserJsObject] =
    dexieDB.table("users")

  def createUser(user: User): Future[Any] = {
    // Add the user and return the result to get the ID after the user is inserted
    usersTable.add(user.toJsObject).toFuture.map { id =>
      user.copy(id = Some(id)) // Assign the generated ID to the user
    }
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

  def hashPassword(password: String): String = {
    // Generate a hash for the password
    BcryptJS.hashSync(password, 10)  // 10 is the number of salt rounds
  }

  def checkPassword(password: String, hash: String): Boolean = {
    BcryptJS.compareSync(password, hash)
  }

  def saveUserSession(username: String): Unit = {
    // Save the username in localStorage
    js.Dynamic.global.localStorage.setItem("username", username)
  }

  def getLoggedInUser(): Option[String] = {
    // Retrieve the username from localStorage
    val username = js.Dynamic.global.localStorage.getItem("username")
    if (username != null) Some(username.toString) else None
  }
  
}
