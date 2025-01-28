package kanban.ui.views

import com.raquo.laminar.api.L.{*, given}
import kanban.controllers.UserController
import kanban.controllers.UserController.userEventBus
import kanban.domain.events.UserEvent
import kanban.routing.Pages.*
import kanban.domain.models.*
import kanban.routing.Router
import kanban.service.UserService.{createUser, getAllUsers}

import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global

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
                div(cls := "user-info", s"Name: ${user.name}"),
                div(cls := "user-info", s"Alter: ${user.age}"),
                div(cls := "user-info", s"Email: ${user.email}"),
                div(cls := "user-info", s"Password: ${user.password}"),
                button(
                  cls := "delete-user-button",
                  "Löschen"
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
            onInput.mapToValue.filter(_.forall(_.isDigit)).map(_.toIntOption.getOrElse(0)) --> newAgeVar
          ),
          input(
            cls := "user-email-input",
            placeholder := "Email",
            onInput.mapToValue --> newEmailVar
          ),
          button(
            cls := "add-user-button",
            onClick --> { _ => 
              userEventBus.emit(
                UserEvent.Added(
                  User(
                    id = None,
                    name = newNameVar.now(),
                    age = newAgeVar.now(),
                    email = newEmailVar.now(),
                    password = newPasswordVar.now()
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
