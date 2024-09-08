package kanban

import kanban.Router.*
import com.raquo.laminar.api.L.{*, given}
import org.scalajs.dom
import kanban.Pages.*
import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object LoginPageView {
  def apply(): HtmlElement = {

    val messageVar = Var("")

    val loginFormElement = div(
      className := "login-form-container",
      h2("Login"),
      form(
        onSubmit.preventDefault.mapTo(()) --> { _ =>
          // messageVar.set("Incorrect credentials")
          // TODO: only allow after credentials match
          Router.pushState(KanbanBoardPage)
        },
        div(
          className := "form-group",
          label(forId := "username", "Username:"),
          input(
            typ := "text",
            idAttr := "username",
            // name := "username",
            required := true
          )
        ),
        div(
          className := "form-group",
          label(forId := "password", "Password:"),
          input(
            typ := "password",
            idAttr := "password",
            // name := "password",
            required := true
          )
        ),
        div(
          className := "form-group",
          button(
            typ := "submit",
            "Login"
          )
        ),
        child.text <-- messageVar.signal
      )
    )

    return loginFormElement
  }
}
