package kanban.ui.views

import com.raquo.laminar.api.L.{*, given}
import kanban.controllers.ProjectController.projectEventBus
import kanban.controllers.UserController
import kanban.domain.events.ProjectEvent.Added
import kanban.domain.models.{Project, ProjectStatus, User, UserId, Permission, UserPermission}
import kanban.service.UserService.getAllUsers
import kanban.ui.views.KanbanBoardPageView.toggleDisplay
import org.scalajs.dom
import rdts.base.Uid

import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js.Date
import scala.util.{Failure, Success}

object AddProjectFormView {
  val projectName = Var("")
  val projectDeadline = Var(Option.empty[Date])
  val projectRevisorId: Var[UserId] = Var(Uid.zero)
  val projectStatus = Var(ProjectStatus.Neu.toString())
  val timeTracked = Var(0.0)

  val projectStatusValues: List[String] =
    ProjectStatus.values.map(_.toString).toList

  val permittedUsers = Var(Set.empty[UserId])

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
            // placeholder option
            option(
              value := "",
              selected := true,
              disabled := true,
              hidden := true,
              "Choose revisor"
            ),
            children <-- UserController.users.signal.map { users =>
              users.map { user =>
                option(
                  // TODO: strip the emoji
                  value := user.id.delegate,
                  user.name.read
                )
              }
            },
            onChange.mapToValue --> { userId =>
              if (userId.nonEmpty) {
                projectRevisorId.set(
                  Uid.predefined(userId)
                )
              } else {
                println(s"userId is empty!!")
              }
            }
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
                )
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
          onClick --> { e =>
            toggleDisplay.update(t => "none")
            val revisorId = projectRevisorId.now()
            val allUsers = UserController.users.now()
            val permitted: Set[UserPermission] = allUsers.map { user =>
              if (user.id == revisorId) UserPermission(user.id, Permission.Write)
              else UserPermission(user.id, Permission.None)
            }.toSet
            val newProject = Project(
              name = projectName.now(),
              status = ProjectStatus.valueOf(projectStatus.now()),
              revisorId = revisorId,
              deadline = projectDeadline.now(),
              permittedUsers = Some(permitted)
            )
            projectEventBus.emit(Added(newProject))
          }
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
