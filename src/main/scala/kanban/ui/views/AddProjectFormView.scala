package kanban.ui.views

import com.raquo.laminar.api.L.{*, given}
import kanban.domain.models.{Project, ProjectStatus, User}
import KanbanBoardPageView.*
import kanban.service.UserService.getAllUsers
import org.scalajs.dom

import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js.Date
import scala.util.{Failure, Success}
import kanban.controllers.ProjectController.projectEventBus
import kanban.domain.events.ProjectEvent.Added


object AddProjectFormView {
    //laminar reactive variables that store the state of the UI
    val projectName = Var("")
    val projectDeadline = Var(Option.empty[Date])
    val projectRevisorId = Var(Option.empty[Int])
    val projectStatus = Var(ProjectStatus.Neu.toString())
    val timeTracked = Var(0.0)

    //other variables
    val projectStatusValues: List[String] =
        ProjectStatus.values.map(_.toString).toList
    
    //get data from the database
    val revisors = List(
        User(Some(1), "User 1", 22, "user1@gmail.com"),
        User(Some(2), "User 2", 23, "user2@gmail.com"),
        User(Some(3), "User 3", 24, "user3@gmamil.com")
    )
    val revisorsListVar: Var[List[User]] = Var(revisors)

    //Render UI
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
                                        value := user.id.getOrElse(0).toString,
                                        user.name
                                    )
                            }
                        },
                        onChange.mapToValue --> { userId =>
                            if (userId.nonEmpty) {
                                projectRevisorId.set(
                                    Some(userId.toInt)
                                ) // Set deadline as a Date object
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
                    onClick --> { e =>
                        toggleDisplay.update(t => "none")
                        projectEventBus.emit(Added(
                            Project(
                                id = None,
                                name = projectName.now(),
                                status = ProjectStatus.valueOf(projectStatus.now()),
                                revisorId = projectRevisorId.now(),
                                deadline = projectDeadline.now(),
                                timeTracked = 0
                            )
                        ))
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
