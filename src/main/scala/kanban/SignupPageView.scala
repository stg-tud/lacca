package kanban

import kanban.Router.*
import kanban.Pages.*

import scala.scalajs.js
import scala.scalajs.js.annotation.*
import org.scalajs.dom
import com.raquo.laminar.api.L.{*, given}
import kanban.models.User
import kanban.service.UserService.createUser
import scala.concurrent.ExecutionContext.Implicits.global


object SignupPageView {

  val nameVar = Var("")
  val ageVar = Var(0)
  val emailVar = Var("")

  def apply(): HtmlElement = {
    val signupFormElement = div(
      className := "container",
      h2("Signup"),
      form(
        onSubmit.preventDefault.mapTo(()) --> { _ =>
          createUser(
            User(
              id = None,
              name = nameVar.now(),
              age = ageVar.now(),
              email = emailVar.now()
            )
          ).onComplete(_ => Router.pushState(LoginPage))
        },
        div(
          className := "form-group",
          label(forId := "email", "Email:"),
          input(
            typ := "email",
            idAttr := "email",
            required := true,
            onInput.mapToValue --> emailVar
          )
        ),
        div(
          className := "form-group",
          label(forId := "username", "Username:"),
          input(
            typ := "text",
            idAttr := "username",
            required := true,
            onInput.mapToValue --> nameVar
          )
        ),
        div(
          className := "form-group",
          label(forId := "password", "Password:"),
          input(
            typ := "password",
            idAttr := "password",
            required := true
          )
        ),
        div(
          className := "form-group",
          label(forId := "age", "age:"),
          input(
            idAttr := "age",
            required := true,
            onInput.mapToValue.map(_.toInt) --> ageVar
          )
        ),
        div(
          className := "form-group",
          button(
            typ := "submit",
            "Login"
          )
        )
      )
    )
    return signupFormElement
  }

}
