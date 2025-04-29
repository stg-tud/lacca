package kanban.controllers

import scala.util.{Failure, Success}
import com.raquo.laminar.api.L.{*, given}
import kanban.domain.events.UserEvent
import kanban.domain.models.User
import kanban.service.UserService
import kanban.service.UserService.usersObservable
import com.raquo.airstream.ownership.ManualOwner
import kanban.sync.TrysteroSetup
import kanban.sync.UserSync.{receiveUserUpdate, sendUserUpdate}
import rdts.base.Lattice
import kanban.service.UserService.*

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}
import rdts.base.Uid
import kanban.domain.events.UserEvent.Deleted
import kanban.domain.models.UserId

object UserController {
  val users: Var[List[User]] = Var(List.empty)

  val userEventBus: EventBus[UserEvent] = new EventBus[UserEvent]

  TrysteroSetup.room.onPeerJoin(peerId =>
    users
      .now()
      .foreach(user => sendUserUpdate(user, List(peerId)))
  )

  // listen for user updates from other peers
  receiveUserUpdate((newUser: User) =>
    val userId = newUser.id
    val oldUser: Future[User] = UserService.getUserById(userId)
    val oldPlusMerged = oldUser.map { old => // old value and merged value
      (old, Lattice[User].merge(old, newUser))
    }
    oldPlusMerged.onComplete {
      case Success((old, m)) =>
        if (old <= m) {
          UserService.updateUser(userId, m)
        } else {
          UserService.updateUser(userId, newUser)
        }
      case Failure(_) =>
        UserService.updateUser(userId, newUser)
    }
  )

  // Observable subscription to handle user list changes and broadcast them
  usersObservable.subscribe(
    next = (queryResultFuture) =>
      queryResultFuture.onComplete {
        case Success(usersSeq) => {
          users.set(usersSeq.toList)
          // println(s"Users changed: $mergedUsers")
          // Send message via Trystero after a user change
          // val usersMessage = mergedUsers.toList
          //   .map(user =>
          //     s"${user.id.getOrElse("")}:${user.name}:${user.age}:${user.email}:${user.password}"
          //   )
          //   .mkString(",")
          // TrysteroSetup.sendMessage(s"Users changed: $usersMessage")
        }
        case Failure(exception) =>
          println(s"Error observing users: $exception")
      },
    error = (error) => println(s"Error observing users: $error")
  )

  // Listen for add and delete user events from the event bus
  userEventBus.events.foreach {
    case UserEvent.Added(user) =>
      println("create user event received")
      createUser(user)
      sendUserUpdate(user)
    // addUserAndNotify(user)

    case UserEvent.Deleted(userId) =>
      println("delete user event received")
      deleteUser(userId).onComplete {
        case Success(_) =>
          println(s"User with id: ${userId.toString()} deleted successfully")
        case Failure(exception) =>
          println(
            s"Failed to delete user with id: ${userId.toString()}. Exception: $exception"
          )
      }

    case UserEvent.Updated(userId, updatedUser) =>
      println("update user event received")
      updateUser(userId, updatedUser).onComplete {
        case Success(_) =>
          sendUserUpdate(updatedUser)
          println(s"User with id: ${userId.toString()} updated.")
        case Failure(exception) =>
          println(
            s"Failed to update user with id: ${userId.toString()}. Exception: $exception"
          )
      }
  }

  given ManualOwner()
}
