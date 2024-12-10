package kanban

import com.raquo.laminar.api.L.{*, given}
import kanban.KanbanBoardPageView.*
import kanban.models.*
import kanban.service.UserService.getAllUsers
import org.scalajs.dom
import scala.util.{Success, Failure}
import scala.scalajs.js.Date
import scala.concurrent.ExecutionContext.Implicits.global


object AddProjectFormView {

  val projectName = Var("")
  val projectDeadline = Var(Option.empty[Date])
  val projectRevisorId = Var("")
  val projectStatus = Var(ProjectStatus.Neu.toString())
  val projectStatusValues: List[String] =
    ProjectStatus.values.map(_.toString).toList
  val timeTracked = Var(0.0)
  val revisorsListVar: Var[List[User]] = Var(List())
  getAllUsers().onComplete {
    case Success(users) =>
      println(s"getAllUsers called from AddProjectFormView")
      users.map(u => println(s"user retrieved from db: ${u.name}, ${u.id}"))
      revisorsListVar.set(users.toList)
    case Failure(exception) =>
      println(s"Failed to retrieve users from the db! Exception: $exception")
  }

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
            //placeholder option
            option(
              value := "",
              selected := true,
              disabled := true,
              hidden := true,
              "Choose revisor"
            ),
            children <-- revisorsListVar.signal.map { users =>
              users.map {
                user =>
                  option(
                    value := user.id,
                    user.name
                  )
              }
            },
            onChange.mapToValue --> projectRevisorId
//            value <-- revisor.signal.map(_.toString),
//            onChange.mapToValue --> revisor,
//            revisorValues.map(revisorName =>
//              option(
//                value := revisorName,
//                revisorName
//              )
//            )
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
                projectDeadline.set(
                  Some(new Date(dateStr))
                ) // Set deadline as a Date object
              } else {
                projectDeadline.set(None) // No deadline selected
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
            //TODO: CHECK IF ANY OF THESE FIELDS ARE EMPTY BEFORE
            // CREATING A PROJECT OBJECT IN THE DB
            ProjectCommands.add(
              Project(
                id = projectName.now(),
                name = projectName.now(),
                status = ProjectStatus.valueOf(projectStatus.now()),
                revisorId = projectRevisorId.now(),
                deadline = projectDeadline.now()
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
