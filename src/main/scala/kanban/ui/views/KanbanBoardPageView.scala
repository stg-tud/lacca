package kanban.ui.views

import com.raquo.laminar.api.L.{*, given}
import kanban.controllers.ProjectController
import kanban.domain.models.{Project, ProjectStatus, User}
import kanban.ui.components.{KanbanColumn, NavBar}
import org.scalajs.dom.HTMLElement
//import kanban.ui.views.AddProjectFormView

import scala.scalajs.js.Date
import kanban.controllers.ProjectController.projectEventBus
import kanban.ui.views.DragAndDrop.setupDragAndDrop

object KanbanBoardPageView {


    //dummy list of projects
//    val projects = List(
//        Project(Some(1), "Project 1", ProjectStatus.Neu, Some(1), Some(new Date()), timeTracked = 0),
//        Project(Some(2), "Project 2", ProjectStatus.Geplant, Some(2), Some(new Date()), timeTracked = 0),
//        Project(Some(3), "Project 3", ProjectStatus.InArbeit, Some(3), Some(new Date()), timeTracked = 0),
//        Project(Some(4), "Project 4", ProjectStatus.Neu, Some(1), Some(new Date()), timeTracked = 0),
//    )
//    val projectsVar: Var[List[Project]] = Var(projects)

    val revisors = List(
        User(Some(1), "User 1", 22, "user1@gmail.com"),
        User(Some(2), "User 2", 23, "user2@gmail.com"),
        User(Some(3), "User 3", 24, "user3@gmamil.com")
    )
    val revisorsListVar: Var[List[User]] = Var(revisors)

    val selectedDeadlineVar = Var(Option.empty[Date])
    val selectedRevisorIdVar: Var[Int] = Var(0)
    val projectStatusValues: List[String] = ProjectStatus.values.map(_.toString).toList
    val toggleDisplay: Var[String] = Var("none")

    def apply(): HtmlElement = {
        //ProjectController.loadProjects() ---- no more needed, remove later
        setupDragAndDrop()
        div(
            NavBar(),
            div(
                idAttr := "kanbanboard-container",
                //date filter
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

                //Revisor Filter
                select(
                    idAttr := "revisor",
                    option(
                        value := "",
                        selected := true,
                        hidden := true,
                        disabled := true,
                        "Choose revisor"
                    ),
                    //TODO: get users from the db
                    children <-- revisorsListVar.signal.map { users =>
                        users.map { user =>
                            option(value := user.id.getOrElse(0).toString, user.name)
                        }
                    },
                    onChange.mapToValue --> { value =>
                        selectedRevisorIdVar.set(
                            value.toInt
                        )
                    }
                ),

                //kanban-board
                div(
                    idAttr := "kanban-board",
                    projectStatusValues.map { status =>
                        KanbanColumn(
                            title = status,
                            projects = ProjectController.projects.signal.combineWith(
                                selectedRevisorIdVar.signal,
                                selectedDeadlineVar.signal
                            ).map {
                                (
                                    projectsList: List[Project],
                                    selectedRevisorId: Int,
                                    selectedDeadline: Option[Date]
                                ) => {
                                    projectsList.filter(p =>
                                        (p.status.toString == status) &&
                                            (selectedRevisorId == 0 || p.revisorId.contains(selectedRevisorId)) &&
                                            (selectedDeadline.isEmpty || p.deadline.contains(selectedDeadline.get))
                                    )
                                }
                            }
                        )
                    }
                ),
                button(
                    idAttr := "add-project-button",
                    "projekt hinzufügen",
                    onClick --> { e => {
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
