package kanban.controllers

import scala.util.{Failure, Success}
import com.raquo.laminar.api.L.{*, given}
import kanban.domain.events.UserEvent
import kanban.domain.models.User
import kanban.service.{UserService, TrysteroService}
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
          // Merge new users with existing ones before updating and broadcasting
          val mergedUsers = (users.now() ++ usersSeq.toList).distinctBy(_.id)
          users.set(mergedUsers)
          println(s"Users changed: $mergedUsers")
          // Send message via Trystero after a user change
          val usersMessage = mergedUsers.toList.map(user => 
            s"${user.id.getOrElse("")}:${user.name}:${user.age}:${user.email}:${user.password}"
          ).mkString(",")
          TrysteroService.sendMessage(s"Users changed: $usersMessage")
        }
        case Failure(exception) =>
          println(s"Error observing users: $exception")
      },
    error = (error) => println(s"Error observing users: $error")
    // complete = ? (not needed)
  )

  // Listen for incoming messages via Trystero
  TrysteroService.receiveMessage((data, peerId, metaData) => {
    println(s"Received message from $peerId: $data")
    if (data.startsWith("Users changed:")) {
      // The message contains a serialized list of users, deserialize and update users
      val usersData = data.stripPrefix("Users changed: ")
      try {
        // Deserialize the message manually
        val updatedUsers = usersData.split(",").toList.map { userStr =>
          val parts = userStr.split(":")
          if (parts.length == 5) {
            val userId = Some(parts(0).toInt) // ID as Int
            val userName = parts(1)
            val userAge = parts(2).toInt
            val userEmail = parts(3)
            val userPassword = parts(4)

            // Create a User object with the parsed data
            User(id = userId, name = userName, age = userAge, email = userEmail, password = userPassword)
          } else {
            throw new Exception(s"Invalid user data: $userStr")
          }
        }
        // Merge users instead of replacing
        val mergedUsers = (users.now() ++ updatedUsers).distinctBy(_.id)
        users.set(mergedUsers)

        println(s"Updated local users list: $mergedUsers")
      } catch {
        case e: Exception =>
          println(s"Failed to parse users data: $e")
      }
    }

    // Handle user deletion
    if (data.startsWith("User with ID")) {
      val userIdStr = data.stripPrefix("User with ID ").split(" ")(0)
      try {
        val userId = userIdStr.toIntOption
        userId match {
          case Some(id) =>
            // Remove user from the local list
            val updatedUsers = users.now().filterNot(_.id.contains(id))
            users.set(updatedUsers)
            println(s"User with ID $id deleted locally")
          case None =>
            println(s"Invalid user ID received for deletion")
        }
      } catch {
        case e: Exception =>
          println(s"Failed to parse user ID for deletion: $e")
      }
    }
  })

  userEventBus.events.foreach {
    case UserEvent.Added(user) =>
      println("create user event received")

      // Prevent redundant user addition
      if (users.now().exists(_.id == user.id)) {
        println(s"User ${user.id.getOrElse("")} already exists, skipping add")
      } else {
        UserService.createUser(user).onComplete {
          case Success(_) =>
            //println(s"User with id: ${user.id.get} added successfully!")
            println(s"User added successfully!")
            // After adding user, send a message to other peers
            val message = s"User ${user.name} added! ${user.id.getOrElse("")}:${user.name}:${user.age}:${user.email}:${user.password}"
            TrysteroService.sendMessage(message)
          case Failure(exception) =>
            println(
              s"Failed to add user with id: ${user.id.get}. Exception: $exception"
            )
        }
      }

    case UserEvent.Deleted(id) =>
      println("delete user event received")
      id match {
        case Some(userId) =>
          UserService.deleteUser(Some(userId)).onComplete {
            case Success(_) =>
              println(s"User with id: $userId deleted successfully!")
              // Send message to other peers
              val message = s"User with ID $userId deleted!"
              TrysteroService.sendMessage(message)
            case Failure(exception) =>
              println(s"Failed to delete user with id: $userId. Exception: $exception")
            }
        case None =>
          println("User ID is not defined, cannot delete.")
      }
  }

  given ManualOwner()
}
