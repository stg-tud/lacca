package kanban.controllers

import scala.util.{Failure, Success}
import com.raquo.laminar.api.L.{*, given}
import kanban.domain.models.User
import kanban.service.UserService.usersObservable
import scala.concurrent.ExecutionContext.Implicits.global

object UserController {
  val users: Var[List[User]] = Var(List.empty)

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
}
