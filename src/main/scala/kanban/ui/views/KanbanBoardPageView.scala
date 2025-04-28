package kanban.ui.views

import com.raquo.laminar.api.L.{*, given}
import kanban.controllers.{ProjectController, UserController}
import kanban.domain.models.{Project, ProjectStatus, UserId}
import kanban.sync.Replica
import kanban.ui.components.{KanbanColumn, NavBar}
import kanban.ui.views.DragAndDrop.setupDragAndDrop
import rdts.base.Uid

import scala.scalajs.js.Date

object KanbanBoardPageView {

  println(s"current replicaId: ${Replica.id.now()}")
  val selectedDeadlineVar: Var[Option[Date]] = Var(Option.empty[Date])
  val selectedRevisorIdVar: Var[UserId] = Var(Uid.zero)
  val projectStatusValues: List[String] =
    ProjectStatus.values.map(_.toString).toList
  val toggleDisplay: Var[String] = Var("none")

  def apply(): HtmlElement = {
    setupDragAndDrop()
    div(
      NavBar(),
      div(
        idAttr := "kanbanboard-container",
        div(
          // date filter
          idAttr := "filter-container",
          input(
            typ := "date",
            onInput.mapToValue --> { dateStr =>
              if (dateStr.nonEmpty) {
                selectedDeadlineVar.set(
                  Some(new Date(dateStr))
                )
              } else {
                selectedDeadlineVar.set(None)
              }
            }
          ),
          // Revisor Filter
          select(
            idAttr := "revisor",
            option(
              value := "",
              selected := true,
              hidden := true,
              disabled := true,
              "Bearbeiter"
            ),
            option(
              value := Uid.zero.delegate, // Value for "All Revisors"
              "Bearbeiter" // Label for the "All Revisors" option
            ),
            children <-- UserController.users.signal.map { users =>
              users.map { user =>
                option(value := user.id.delegate, user.name.read)
              }
            },
            onChange.mapToValue --> { value =>
              selectedRevisorIdVar.set(Uid.predefined(value))
            }
          )
        ),

        // kanban-board
        div(
          idAttr := "kanban-board",
          projectStatusValues.map { status =>
            KanbanColumn(
              title = status,
              projects = ProjectController.projects.signal
                .combineWith(
                  selectedRevisorIdVar.signal,
                  selectedDeadlineVar.signal
                )
                .map {
                  (
                      projectsList: List[Project],
                      selectedRevisorId: Uid,
                      selectedDeadline: Option[Date]
                  ) =>
                    {
                      projectsList
                        .filter(p =>
                          p.status.value.toString == status &&
                            (selectedDeadline.isEmpty || p.deadline == selectedDeadline.get) &&
                            (selectedRevisorId == Uid.zero || p.revisorId.read == selectedRevisorId)
                        )
                    }
                }
            )
          }
        ),
        button(
          idAttr := "add-project-button",
          "Projekt hinzufügen",
          onClick --> { e =>
            {
              toggleDisplay.update(t => "")
            }
          }
        ),
        div(
          idAttr := "form-overlay",
          display <-- toggleDisplay,
          AddProjectFormView()
        )
      )
    )
  }
}
