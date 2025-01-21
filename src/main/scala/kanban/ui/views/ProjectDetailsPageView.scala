// package kanban.ui.views

// import com.raquo.laminar.api.L.*
// import kanban.domain.models.User.*
// import kanban.domain.models.{User, UserId}
// import kanban.service.UserService.*
// import scala.concurrent.ExecutionContext.Implicits.global
// import scala.scalajs.js.Date
// import scala.util.{Failure, Success}
// import kanban.ui.components.NavBar
// import kanban.domain.models.ProjectStatus
// import kanban.domain.models.Project

// object ProjectDetailsPageView {
//    //laminar reactive variables that store the state of the UI

//    //other variables

//    //get data from the database
//    val revisors = List(
//         User(Some(1), "User 1", 22, "user1@gmail.com"),
//         User(Some(2), "User 2", 23, "user2@gmail.com"),
//         User(Some(3), "User 3", 24, "user3@gmamil.com")
//     )
//     val revisorsListVar: Var[List[User]] = Var(revisors)

//     val statusValues: List[String] = ProjectStatus.values.map(_.toString).toList


//    //Return the HTML div element that renders this view
//    def apply(projectDetailsPageSignal: Signal[ProjectDetailsPage]): HtmlElement = {
//     val isTimeTrackingSidebarVisible = Var(false)
//     val timeInFormVar = Var(0.0)
//     val editedStatusVar = Var("")
//     val editedRevisorVar: Var[UserId] = Var(None)
//     val editedDeadlineVar = Var[Option[Date]](None)

//     div(
//         cls := "project-details",
//         NavBar(),
//         child <-- projectDetailsPageSignal.map{project => 
//             editedStatusVar.set(project.status.toString)
//             editedRevisorVar.set(project.revisorId)
//             editedDeadlineVar.set(project.deadline)

//             div(
//                 h2(s"Project: ${project.name}"),
//                 div(
//                   cls := "project-detail",
//                   span(cls := "project-detail-label", "Status: "),
//                   select(
//                     statusValues.map(status =>
//                       option(
//                         value := status,
//                         status,
//                         selected := (status == project.status.toString)
//                       )
//                     ),
//                     onChange.mapToValue --> editedStatusVar.set
//                   )
//                 ),
//                 div(
//                   cls := "project-detail",
//                   span(cls := "project-detail-label", "Bearbeiter: "),
//                   select(
//                     children <-- revisorsListVar.signal.map { revisors =>
//                       revisors.map { revisor =>
//                         option(
//                           value := revisor.id.toString,
//                           revisor.name,
//                           selected := (revisor.id == project.revisorId)
//                         )
//                       }
//                     },
//                     onChange.mapToValue --> { value =>
//                       editedRevisorVar.set(value.toIntOption)
//                     }
//                   )
//                 ),
//                 div(
//                   cls := "project-detail",
//                   span(cls := "project-detail-label", "Fälligkeitsdatum: "),
//                   input(
//                     typ := "date",
//                     value := project.deadline
//                       .map(_.toISOString().slice(0, 10))
//                       .getOrElse(""), // Format date as "YYYY-MM-DD"
//                     onInput.mapToValue --> { dateStr =>
//                       if (dateStr.nonEmpty) {
//                         editedDeadlineVar.set(Some(new Date(dateStr)))
//                       } else {
//                         editedDeadlineVar.set(None)
//                       }
//                     }
//                   )
//                 ),
//                 div(
//                   cls := "project-detail",
//                   span(
//                     cls := "project-detail-label",
//                     "Gesamte erfasste Zeit: "
//                   ),
//                   span(
//                     child.text <-- Var(project.timeTracked).signal
//                       .map(timeInMinutes => formatTime(timeInMinutes))
//                   )
//                 ),
//                 button(
//                   cls := "time-tracking-button",
//                   "Zeiterfassung",
//                   onClick --> { _ => isTimeTrackingSidebarVisible.set(true) }
//                 ),

//                 child.maybe <-- isTimeTrackingSidebarVisible.signal.map {
//                   case true =>
//                     Some(
//                       renderTimeTrackingSidebar(
//                         timeInFormVar,
//                         isTimeTrackingSidebarVisible,
//                         project
//                       )
//                     )
//                   case false => None
//                 },
//                 button(
//                   cls := "save-button",
//                   "Speichern",
//                   onClick.map(_ =>
//                     val updatedProject = project.copy(
//                       revisorId = editedRevisorVar.now(),
//                       status = ProjectStatus.valueOf(editedStatusVar.now()),
//                       deadline = editedDeadlineVar.now()
//                     )
//                     // Go back to Kanban view after saving
//                     Router.pushState(KanbanBoardPage)
//                     ProjectCommands.update(project.id, updatedProject)
//                   ) --> projectCommandBus
//                 ),
//                 button(
//                   "Zurück",
//                   onClick --> { _ => Router.pushState(KanbanBoardPage) }
//                 )
//             )
//         }
//     )
//    }
// }
