package kanban

import kanban.Router.*
import kanban.Pages.*
import scala.scalajs.js
import scala.scalajs.js.annotation.*

import org.scalajs.dom

import com.raquo.laminar.api.L.{*, given}

object SignupPageView {
  def apply(): HtmlElement = {
    val signupFormElement = div(
      className := "container",
      h2("Signup"),
      form(
        onSubmit.preventDefault.mapTo(()) --> { _ =>
          Router.pushState(LoginPage)
        },
        div(
          className := "form-group",
          label(forId := "email", "Email:"),
          input(
            typ := "email",
            idAttr := "email",
            required := true
          )
        ),
        div(
          className := "form-group",
          label(forId := "username", "Username:"),
          input(
            typ := "text",
            idAttr := "username",
            required := true
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
