package kanban.ui.views

import com.raquo.laminar.api.L.{*, given}
import kanban.controllers.UserController
import kanban.controllers.UserController.userEventBus
import kanban.domain.events.UserEvent
import kanban.routing.Pages.*
import kanban.domain.models.*
import kanban.routing.Router
import kanban.service.UserService.*

import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global
import kanban.domain.events.UserEvent.Deleted

object AccountOverview {
  def apply(): HtmlElement = {
    // Variable to hold the input data for the new user
    val newNameVar: Var[String] = Var("")
    val newAgeVar: Var[Int] = Var(0)
    val newEmailVar: Var[String] = Var("")
    val newPasswordVar: Var[String] = Var("")

    div(
      cls := "modal",
      div(
        cls := "modal-content",
        h2("Kontoübersicht"),

        // Displaying the list of users
        div(
          cls := "users-list",
          children <-- UserController.users.signal.map { users =>
            users.map { user =>
              div(
                cls := "user-item",
                div(cls := "user-info", s"Name: ${user.name.read}"),
                div(cls := "user-info", s"Alter: ${user.age.read}"),
                div(cls := "user-info", s"Email: ${user.email.read}"),
                div(cls := "user-info", s"Password: ${user.password.read}"),
                button(
                  cls := "delete-user-button",
                  "Löschen",
                  onClick --> { _ =>
                    userEventBus.emit(Deleted(user.id))
                  }
                )
              )
            }
          }
        ),

        // Form to add a new user
        div(
          cls := "add-user-form",
          input(
            cls := "user-name-input",
            placeholder := "Name",
            onInput.mapToValue --> newNameVar
          ),
          input(
            cls := "user-age-input",
            placeholder := "Alter",
            `type` := "number",
            onInput.mapToValue
              .filter(_.forall(_.isDigit))
              .map(_.toIntOption.getOrElse(0)) --> newAgeVar
          ),
          input(
            cls := "user-email-input",
            placeholder := "Email",
            onInput.mapToValue --> newEmailVar
          ),
          input(
            cls := "user-password-input",
            placeholder := "Password",
            onInput.mapToValue --> newPasswordVar
          ),
          button(
            cls := "add-user-button",
            onClick --> { _ =>
              val hashedPassword = hashPassword(newPasswordVar.now())
              userEventBus.emit(
                UserEvent.Added(
                  User(
                    name = newNameVar.now(),
                    age = newAgeVar.now(),
                    email = newEmailVar.now(),
                    password = hashedPassword
                  )
                )
              )
            },
            "Benutzer hinzufügen"
          )
        ),
        button(
          cls := "go-back-button",
          onClick --> { _ =>
            Router.pushState(KanbanBoardPage)
          },
          "Zurück"
        )
      )
    )
  }
}
