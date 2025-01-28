package kanban.controllers

import scala.util.{Failure, Success}
import com.raquo.laminar.api.L.{*, given}
import kanban.domain.events.UserEvent
import kanban.domain.models.User
import kanban.service.UserService
import kanban.service.UserService.usersObservable
import com.raquo.airstream.ownership.ManualOwner

import scala.concurrent.ExecutionContext.Implicits.global

object UserController {
  val users: Var[List[User]] = Var(List.empty)

  val userEventBus: EventBus[UserEvent] = new EventBus[UserEvent]

  usersObservable.subscribe(
    next = (queryResultFuture) =>
      queryResultFuture.onComplete {
        case Success(usersSeq) => {
          users.set(usersSeq.toList)
          println(s"Users changed: ${usersSeq.toList}")
        }
        case Failure(exception) =>
          println(s"Error observing users: $exception")
      },
    error = (error) => println(s"Error observing users: $error")
    // complete = ? (not needed)
  )

  userEventBus.events.foreach {
    case UserEvent.Added(user) =>
      println("create user event received")
      UserService.createUser(user).onComplete {
        case Success(_) =>
          println(s"User with id: ${user.id.get} added successfully!")
        case Failure(exception) =>
          println(
            s"Failed to add user with id: ${user.id.get}. Exception: $exception"
          )
      }
    case UserEvent.Deleted(id) =>
      println("delete user event received")
      UserService.deleteUser(id).onComplete {
        case Success(_) =>
          println(s"User with id: ${id.get} deleted successfully!")
        case Failure(exception) =>
          println(
            s"Failed to delete user with id: ${id.get}. Exception: $exception"
          )
      }
  }

  given ManualOwner()
}
