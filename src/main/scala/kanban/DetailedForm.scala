package kanban

import org.scalajs.dom
import org.scalajs.dom.document
import scalatags.JsDom.all._

object DetailedForm {
  def openDetailedForm(): Unit = {
    val form = div(id := "detailed-form")(
      h3("Zeit erfassen"),
      div(
        label("Projekt"),
        input(`type` := "text", placeholder := "Projektname")
      ),
      div(
        label("Bearbeiter"),
        input(`type` := "text", placeholder := "Bearbeiter")
      ),
      div(
        label("Datum"),
        input(`type` := "date")
      ),
      div(
        label("Beginn"),
        input(`type` := "time", placeholder := "HH:MM")
      ),
      div(
        label("Ende"),
        input(`type` := "time", placeholder := "HH:MM")
      ),
      div(
        label("Beschreibung"),
        input(`type` := "text", placeholder := "Beschreibung")
      ),
      button(`type` := "button", id := "submit-detailed-form")("Speichern"),
      button(`type` := "button", id := "cancel-detailed-form")("Abbrechen")
    ).render

    val formOverlay = div(
      id := "detailed-form-overlay",
      form
    ).render

    document.body.appendChild(formOverlay)

    dom.document.getElementById("submit-detailed-form").addEventListener("click", { (e: dom.MouseEvent) =>
      document.body.removeChild(formOverlay)
    })

    dom.document.getElementById("cancel-detailed-form").addEventListener("click", { (e: dom.MouseEvent) =>
      document.body.removeChild(formOverlay)
    })
  }
}





