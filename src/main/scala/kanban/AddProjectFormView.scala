package kanban

import com.raquo.laminar.api.L.{*, given}
import kanban.KanbanBoardPageView.*
import kanban.models.*
import org.scalajs.dom

import scala.scalajs.js.Date

object AddProjectFormView {
  val projectName = Var("")
  val deadline = Var(Option.empty[Date])
  val revisor = Var(Revisors.Manas.toString())
  val revisorValues: List[String] =
    Revisors.values.map(_.toString).toList
  val projectStatus = Var(ProjectStatus.Neu.toString())
  val projectStatusValues: List[String] =
    ProjectStatus.values.map(_.toString).toList
  val timeTracked = Var(0.0)

  def apply(): HtmlElement = {
    div(
      idAttr := "add-project-form",
      h3(
        "Neues Projekt hinzufügen",
        input(
          typ := "text",
          idAttr := "project-name",
          placeholder := "Projektname eingeben",
          onInput.mapToValue --> projectName
        ),
        label(
          forId := "project-column",
          "Status von dem Projekt",
          select(
            idAttr := "project-column",
            value <-- projectStatus.signal.map(_.toString),
            onChange.mapToValue --> projectStatus,
            projectStatusValues.map(status =>
              option(
                value := status,
                status
              )
            )
          )
        ),
        br(),
        label(
          "Bearbeiter",
          select(
            idAttr := "revisors",
            value <-- revisor.signal.map(_.toString),
            onChange.mapToValue --> revisor,
            revisorValues.map(revisorName =>
              option(
                value := revisorName,
                revisorName
              )
            )
          )
        ),
        br(),
        label(
          "Fälligkeitsdatum",
          input(
            typ := "date",
            idAttr := "deadline",
            onInput.mapToValue --> { dateStr =>
              if (dateStr.nonEmpty) {
                deadline.set(
                  Some(new Date(dateStr))
                ) // Set deadline as a Date object
              } else {
                deadline.set(None) // No deadline selected
              }
            }
          )
        ),
        button(
          typ := "submit",
          idAttr := "submit-button",
          "Hinzufügen",
          onClick.map(_ =>
            toggleDisplay.update(_ => "none")
            ProjectCommands.add(
              Project(
                id = projectName.now(),
                name = projectName.now(),
                status = ProjectStatus.valueOf(projectStatus.now()),
                revisor = Revisors.valueOf(revisor.now()),
                deadline = deadline.now()
              )
            )
          ) --> projectCommandBus
        ),
        button(
          idAttr := "cancel-button",
          "Abbrechen",
          onClick --> { e =>
            toggleDisplay.update(t => "none")
          }
        )
      )
    )
  }
}
