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

  // Extracted method for handling incoming user changes from Trystero
  def handleUsersChangedMessage(usersData: String): Unit = {
    try {
      val updatedUsers = usersData.split(",").toList.map { userStr =>
        val parts = userStr.split(":")
        if (parts.length == 5) {
          val userId = Some(parts(0).toInt)
          val userName = parts(1)
          val userAge = parts(2).toInt
          val userEmail = parts(3)
          val userPassword = parts(4)

          User(id = userId, name = userName, age = userAge, email = userEmail, password = userPassword)
        } else {
          throw new Exception(s"Invalid user data: $userStr")
        }
      }

      val existingUsers = users.now()
      val newUsers = updatedUsers.filterNot(user => existingUsers.exists(_.id == user.id))

      if (newUsers.nonEmpty) {
        val mergedUsers = (existingUsers ++ newUsers).distinctBy(_.id)
        users.set(mergedUsers)

        newUsers.foreach { user =>
          UserService.createUser(user).foreach(_ =>
            println(s"User ${user.name} saved to IndexedDB on this peer")
          )
        }

        println(s"Updated local users list: $mergedUsers")
      } else {
        println("No new users to add, skipping update.")
      }
    } catch {
      case e: Exception =>
        println(s"Failed to parse users data: $e")
    }
  }

  // Extracted method to handle user deletion logic
  def deleteUserAndNotify(userId: Int): Unit = {
    val existingUsers = users.now()
    val userToDelete = existingUsers.find(_.id.contains(userId))

    userToDelete match {
      case Some(user) =>
        // Remove user from the local list
        val updatedUsers = existingUsers.filterNot(_.id.contains(userId))
        users.set(updatedUsers)
        println(s"User with ID $userId deleted locally")

        // Send message to other peers about the user deletion
        val message = s"User with ID $userId deleted!"
        TrysteroService.sendMessage(message)

        // Delete user from IndexedDB after propagating the message
        UserService.deleteUser(Some(userId)).onComplete {
          case Success(_) =>
            println(s"User with ID $userId deleted from IndexedDB")
          case Failure(exception) =>
            println(s"Failed to delete user with ID $userId from IndexedDB. Exception: $exception")
        }

      case None =>
        println(s"User with ID $userId not found locally, skipping deletion.")
    }
  }

  // Extracted method for adding users and notifying peers
  def addUserAndNotify(user: User): Unit = {
    UserService.createUser(user).onComplete {
      case Success(_) =>
        println(s"User added successfully!")
        // After adding user, send a message to other peers
        val message = s"User ${user.name} added! ${user.id.getOrElse("")}:${user.name}:${user.age}:${user.email}:${user.password}"
        TrysteroService.sendMessage(message)
      case Failure(exception) =>
        println(s"Failed to add user with id: ${user.id.get}. Exception: $exception")
    }
  }

  // Observable subscription to handle user list changes and broadcast them
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
  )

  // Listen for incoming messages via Trystero
  TrysteroService.receiveMessage((data, peerId, metaData) => {
    println(s"Received message from $peerId: $data")
    if (data.startsWith("Users changed:")) {
      handleUsersChangedMessage(data.stripPrefix("Users changed: "))
    }

    // Listen for user deletion messages
    if (data.startsWith("User with ID")) {
      val userIdStr = data.stripPrefix("User with ID ").split(" ")(0)
      try {
        val userId = userIdStr.toIntOption
        userId.foreach(deleteUserAndNotify)
      } catch {
        case e: Exception =>
          println(s"Failed to parse user ID for deletion: $e")
      }
    }
  })

  // Listen for add and delete user events from the event bus
  userEventBus.events.foreach {
    case UserEvent.Added(user) =>
      println("create user event received")
      addUserAndNotify(user)

    case UserEvent.Deleted(id) =>
      println("delete user event received")
      id.foreach(deleteUserAndNotify)
  }

  given ManualOwner()
}
