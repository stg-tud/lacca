package kanban.service

import kanban.domain.models.{User, UserId}
import kanban.persistence.DexieDB.dexieDB
import org.scalablytyped.runtime.StringDictionary
import typings.dexie.mod.{Dexie, Table, UpdateSpec, liveQuery}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.scalajs.js
import kanban.persistence.BcryptJS
import org.getshaka.nativeconverter.NativeConverter

object UserService {

  private val usersTable: Table[js.Any, String, js.Any] =
    dexieDB.table("users")

  def createUser(user: User): Future[Any] = {
    // Add the user and return the result to get the ID after the user is inserted
    usersTable.put(user.toNative).toFuture
  }

  def getAllUsers(): Future[Seq[User]] = {
    println(s"getAllUsers called!!")
    usersTable.toArray().toFuture.map { usersJsArray =>
      usersJsArray
        .map(entry => NativeConverter[User].fromNative(entry))
        .toSeq
    }
  }

  def getUserById(userId: UserId): Future[User] = {
    usersTable.get(userId.delegate).toFuture.map { userJsObject =>
      if (userJsObject.isEmpty) {
        throw new Exception(s"User ${userId.show} not found!!")
      } else {
        NativeConverter[User].fromNative(userJsObject.get)
      }
    }
  }

  val usersObservable = liveQuery(() => getAllUsers())

  def deleteUser(id: UserId): Future[Unit] = {
    usersTable
      .delete(id.delegate)
      .toFuture
  }

  def updateUser(userId: UserId, user: User): Future[String] =
    println("updateUser called")
    usersTable
      .put(user.toNative)
      .toFuture

  def hashPassword(password: String): String = {
    // Generate a hash for the password
    BcryptJS.hashSync(password, 10) // 10 is the number of salt rounds
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
