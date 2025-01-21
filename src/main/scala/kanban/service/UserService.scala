package kanban.service

import kanban.domain.models.{User, UserJsObject}
import kanban.persistence.DexieDB.dexieDB
import org.scalablytyped.runtime.StringDictionary
import typings.dexie.mod.{Dexie, Table, UpdateSpec}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.scalajs.js

object UserService {

    private val usersTable: Table[UserJsObject, Int, UserJsObject] =
        dexieDB.table("users")

    def createUser(user: User): Future[Any] = {
        usersTable.add(user.toJsObject).toFuture
    }
//    val user = User(
//        id = None,
//        name = "John Doe",
//        age = 30,
//        email = "jondoe@doe.com"
//    )
//    createUser(user).onComplete {
//        case scala.util.Success(value) => println(s"User created successfully!!")
//        case scala.util.Failure(exception) => println(s"User creation failed!! Exception: $exception")
//    }

    def getAllUsers(): Future[Seq[User]] = {
        println(s"getAllUsers called!!")
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
