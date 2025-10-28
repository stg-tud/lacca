package kanban.ui.views

import com.raquo.laminar.api.L.{*, given}
import kanban.routing.Pages.KanbanBoardPage
import kanban.routing.Router
import kanban.service.UserService.*
import scala.concurrent.{Future, ExecutionContext}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Success, Failure}
import scala.scalajs.js

// Global state for username with default "Guest"
object GlobalState {
  // Restore saved username and userId from localStorage
  private val storedUsername =
    Option(js.Dynamic.global.localStorage.getItem("username").asInstanceOf[String])
  private val storedUserId =
    Option(js.Dynamic.global.localStorage.getItem("userId").asInstanceOf[String])

  val usernameVar = Var(storedUsername.getOrElse("Guest"))
  val userIdVar   = Var(storedUserId)
}

object LoginPageView {
  def apply(): HtmlElement = {
    val messageVar = Var("")
    val usernameVar = Var("")
    val passwordVar = Var("")

    // Function to check credentials against the database
    def checkCredentials(
        username: String,
        password: String
    ): Future[Boolean] = {
      getAllUsers().map { users =>
        // Check if the user exists and credentials match
        users.exists(user =>
          user.name.read == username && checkPassword(
            password,
            user.password.read
          )
        )
      }
    }

    val loginFormElement = div(
      className := "login-container",
      h2(cls := "login-heading", "Login to Your Account"),
      form(
        onSubmit.preventDefault.mapTo(()) --> { _ =>
          val username = usernameVar.now()
          val password = passwordVar.now()
          // Check credentials in the database
          checkCredentials(username, password).onComplete {
            case Success(isValid) =>
              if (isValid) {
                messageVar.set("Login successful")
                // Find the userId from the DB
                getAllUsers().foreach { users =>
                  users.find(u =>
                    u.name.read == username && checkPassword(password, u.password.read)
                  ).foreach { user =>
                    val userId = user.id.show
                    GlobalState.usernameVar.set(username)
                    GlobalState.userIdVar.set(Some(userId))
                    // print userId to verify
                    println(s"[Login] Logged in successfully with userId = $userId")
                    // Store in localStorage, avoid losing login when reload
                    js.Dynamic.global.localStorage.setItem("username", username)
                    js.Dynamic.global.localStorage.setItem("userId", userId)

                    Router.pushState(KanbanBoardPage)
                  }
                }
              } else {
                messageVar.set("Incorrect credentials, please try again")
              }
            case Failure(exception) =>
              messageVar.set(
                s"Error checking credentials: ${exception.getMessage}"
              )
          }
        },
        div(
          className := "form-group",
          label(forId := "username", "Username:"),
          input(
            typ := "text",
            idAttr := "username",
            // name := "username",
            cls := "form-input",
            required := true,
            onInput.mapToValue --> usernameVar // Bind username input
          )
        ),
        div(
          className := "form-group",
          label(forId := "password", "Password:"),
          input(
            typ := "password",
            idAttr := "password",
            // name := "password",
            cls := "form-input",
            required := true,
            onInput.mapToValue --> passwordVar // Bind password input
          )
        ),
        div(
          className := "form-group",
          button(
            typ := "submit",
            cls := "login-button",
            "Login"
          )
        ),
        div(
          cls := "error-message",
          child.text <-- messageVar.signal
        )
      )
    )
    return loginFormElement
  }
}
