package kanban.ui.views

import com.raquo.laminar.api.L.*
import kanban.controllers.ProjectController.projectEventBus
import kanban.controllers.{ProjectController, UserController}
import kanban.domain.events.ProjectEvent.Updated
import kanban.domain.models.User.*
import kanban.domain.models.{Project, ProjectStatus, User, UserId}
import kanban.routing.Pages.{KanbanBoardPage, ProjectDetailsPage}
import kanban.routing.Router
import kanban.service.UserService.*
import kanban.sync.Replica
import kanban.ui.components.NavBar
import rdts.datatypes.LastWriterWins
import rdts.time.CausalTime

import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js.Date
import scala.util.{Failure, Success}
import rdts.base.Uid

object ProjectDetailsPageView {
  val statusValues: List[String] = ProjectStatus.values.map(_.toString).toList

  def apply(
      projectDetailsPageSignal: Signal[ProjectDetailsPage]
  ): HtmlElement = {
    val isTimeTrackingSidebarVisible = Var(false)
    val timeInFormVar = Var(0.0)
    val editedStatusVar = Var("")
    val editedRevisorIdVar: Var[UserId] = Var(Uid.zero)
    val editedDeadlineVar = Var[Option[Date]](None)
    val showPermittedUsersVar = Var(false)
    val editedPermittedUserIdVar: Var[UserId] = Var(Uid.zero)

    div(
      NavBar(),
      div(
        idAttr := "project-detail-container",
        child <-- projectDetailsPageSignal
          .combineWith(ProjectController.projects.signal)
          .map { case (ProjectDetailsPage(projectId), projects) =>
            // Add proper error handling using Option.fold
            projects
              .find(_.id == projectId)
              .fold(
                // If no project is found, display an error message
                div(
                  cls := "project-not-found",
                  h2("Project Not Found"),
                  p(s"No project found with ID: ${projectId.delegate}"),
                  button(
                    cls := "back-button",
                    "Back to Kanban Board",
                    onClick --> { _ => Router.pushState(KanbanBoardPage) }
                  )
                )
              ) { project =>
                // If project is found, proceed with the normal view
                editedRevisorIdVar.set(project.revisorId.read)
                editedStatusVar.set(project.status.read.toString)
                editedDeadlineVar.set(project.deadline.value)

                val currentStatusStr = project.status.read.toString

                div(
                  cls := "project-details",
                  h2(s"Project: ${project.name.read}"),

                  // Editable status dropdown
                  div(
                    cls := "project-detail",
                    span(cls := "project-detail-label", "Status: "),
                    select(
                      statusValues.map(status =>
                        option(
                          value := status,
                          status,
                          selected := (status == currentStatusStr)
                        )
                      ),
                      onChange.mapToValue --> editedStatusVar.set
                    )
                  ),

                  // Editable revisor dropdown
                  div(
                    cls := "project-detail",
                    span(cls := "project-detail-label", "Bearbeiter: "),
                    select(
                      children <-- UserController.users.signal.map { users =>
                        users.map { user =>
                          option(
                            value := user.id.delegate,
                            user.name.read,
                            selected := (user.id == project.revisorId.read)
                          )
                        }
                      },
                      onChange.mapToValue --> { userId =>
                        if (userId.nonEmpty) {
                          editedRevisorIdVar.set(
                            Uid.predefined(userId)
                          ) // Set deadline as a Date object
                        } else {
                          println(s"userId is empty!!")
                        }
                      }
                    )
                  ),

                  // Editable deadline input (date picker)
                  div(
                    cls := "project-detail",
                    span(cls := "project-detail-label", "Fälligkeitsdatum: "),
                    input(
                      typ := "date",
                      value := project.deadline.value
                        .map(_.toISOString().slice(0, 10))
                        .getOrElse(""), // Format date as "YYYY-MM-DD"
                      onInput.mapToValue --> { dateStr =>
                        if (dateStr.nonEmpty) {
                          editedDeadlineVar.set(Some(new Date(dateStr)))
                        } else {
                          editedDeadlineVar.set(None)
                        }
                      }
                    )
                  ),

                  button(
                    cls := "toggle-users-button",
                    "Zugelassene Benutzer anzeigen",
                    onClick --> { _ => showPermittedUsersVar.update(!_)}
                  ),

                  // List of all users with permission level select
                  child <-- UserController.users.signal.map { users =>
                    val currentPermittedIds: Set[UserId] = project.listPermittedUsers match {
                      case Some(lwwSet) => lwwSet.read
                      case None         => Set.empty
                    }
                    
                    div(
                      h3("Benutzerberechtigungen"),
                      ul(
                        users.map { user =>
                          val currentPermission: String = 
                            if (currentPermittedIds.contains(user.id)) "Write" else "None"
                          li(
                            span(user.name.read + ": "),
                            select(
                              option(value := "None", "None", selected := (currentPermission == "None")),
                              option(value := "Write", "Write", selected := (currentPermission == "Write")),
                              onChange.mapToValue --> { newPermission =>
                                val currentSet: Set[UserId] = project.listPermittedUsers.map(_.read).getOrElse(Set.empty)
                                val newSet = 
                                  if (newPermission == "Write") currentSet + user.id
                                  else currentSet - user.id
                                
                                val updatedCRDT = project.listPermittedUsers match {
                                  case Some(lww) => lww.write(newSet)
                                  case None => LastWriterWins(CausalTime.now(), newSet)
                                }
                                
                                val updatedProject = project.copy(listPermittedUsers = Some(updatedCRDT))
                                projectEventBus.emit(Updated(project.id, updatedProject))
                              }
                            )
                          )
                        }
                      )
                    )
                  },
                  // List of all users with permission level select

                  child.maybe <-- showPermittedUsersVar.signal.map {
                    case true =>
                      Some(
                        div(
                          cls := "permitted-users-list",
                          span("Zugelassene Benutzer: "),
                          child <-- UserController.users.signal.map { users =>
                            val permittedUserIds: Set[UserId] =
                              project.listPermittedUsers
                              .map(_.read)
                              .getOrElse(Set.empty)
                            
                            val permittedUsers = users.filter(user => permittedUserIds.contains(user.id))
                            if permittedUsers.isEmpty then
                              div("Keine zugelassenen Benutzer")
                            else
                              ul(
                                permittedUsers.map(user =>
                                  li(user.name.read)
                                )
                              )
                          }
                        )
                      )
                    case false => None
                  },

                  // Function to display the total time tracked in "hour:minute" format
                  div(
                    cls := "project-detail",
                    span(
                      cls := "project-detail-label",
                      "Gesamte erfasste Zeit: "
                    ),
                    span(
                      child.text <-- Var(project.timeTracked).signal
                        .map(timeInMinutes => formatTime(timeInMinutes.value))
                    )
                  ),

                  // Button to open the time tracking sidebar
                  button(
                    cls := "time-tracking-button",
                    "Zeiterfassung",
                    onClick --> { _ => isTimeTrackingSidebarVisible.set(true) }
                  ),

                  // Conditionally render the sidebar
                  child.maybe <-- isTimeTrackingSidebarVisible.signal.map {
                    case true =>
                      Some(
                        renderTimeTrackingSidebar(
                          timeInFormVar,
                          isTimeTrackingSidebarVisible,
                          project
                        )
                      )
                    case false => None
                  },
                  button(
                    cls := "save-button",
                    "Speichern",
                    onClick --> { _ =>
                      val updatedProject = project.copy(
                        status = project.status
                          .write(ProjectStatus.valueOf(editedStatusVar.now())),
                        revisorId =
                          project.revisorId.write(editedRevisorIdVar.now()),
                        deadline =
                          project.deadline.write(editedDeadlineVar.now())
                      )
                      projectEventBus.emit(Updated(project.id, updatedProject))
                      Router.pushState(KanbanBoardPage)
                    }
                  )
                )
              }
          }
      )
    )
  }

  // Function to render the time tracking form
  private def renderTimeTrackingSidebar(
      timeInFormVar: Var[Double],
      isTimeTrackingSidebarVisible: Var[Boolean],
      project: Project
  ): HtmlElement = {
    val startTimeVar = Var("")
    val endTimeVar = Var("")
    val formValidVar = Var(false)
    val dateVar = Var("")
    val errorMessageVar = Var("")

    def validateForm(): Unit = {
      val isStartTimeFilled = startTimeVar.now().nonEmpty
      val isEndTimeFilled = endTimeVar.now().nonEmpty
      val isDateFilled = Option(dateVar.now()).exists(_.nonEmpty)

      // Lexicographically compare time strings
      val isValidTime = for {
        startTime <- Option(startTimeVar.now())
          .filter(_.matches("^(?:[01]\\d|2[0-3]):[0-5]\\d$"))
        endTime <- Option(endTimeVar.now())
          .filter(_.matches("^(?:[01]\\d|2[0-3]):[0-5]\\d$"))
      } yield startTime < endTime // Lexicographical comparison for "HH:mm" format

      // If fields are missing or invalid, show an error message
      if (!isStartTimeFilled || !isEndTimeFilled || !isDateFilled) {
        errorMessageVar.set("Bitte füllen Sie alle Felder aus.")
      } else if (!isValidTime.getOrElse(true)) {
        errorMessageVar.set("Die Startzeit muss vor der Endzeit liegen.")
      } else {
        errorMessageVar.set("") // No errors
      }

      formValidVar.set(
        isStartTimeFilled && isEndTimeFilled && isDateFilled && isValidTime
          .getOrElse(false)
      )
    }

    div(
      cls := "time-tracking-sidebar",
      div(
        cls := "sidebar-header",
        h3("Zeiterfassung"),
        button(
          cls := "close-button",
          "✕",
          onClick --> { _ => isTimeTrackingSidebarVisible.set(false) }
        )
      ),
      div(
        cls := "sidebar-content",
        div(
          cls := "form-field",
          span("Datum: "),
          input(
            typ := "date",
            onInput.mapToValue --> { value =>
              dateVar.set(value)
              validateForm() // Re-validate when date changes
            }
          ) // Date input field
        ),
        div(
          cls := "form-field",
          span("Beginn: "),
          input(
            typ := "time",
            onInput.mapToValue --> { value =>
              startTimeVar.set(value)
              validateForm()
            },
            styleAttr := "margin-left: 1em; width: 6em;"
          )
        ),
        div(
          cls := "form-field",
          span("Ende: "),
          input(
            typ := "time",
            onInput.mapToValue --> { value =>
              endTimeVar.set(value)
              validateForm()
            },
            styleAttr := "margin-left: 1em; width: 6em;"
          )
        ),

        // Show error message if form is invalid
        child <-- errorMessageVar.signal.map {
          case ""      => emptyNode
          case message => div(cls := "error-message", message)
        },
        button(
          cls := "submit-button",
          "Speichern",
          disabled <-- formValidVar.signal.map(!_),
          onClick --> { _ =>
            // Update the project list reactively
            Replica.id.now() match // check if the replicaId is known
              case Some(repId) =>
                val addedTime =
                  calculateDuration(startTimeVar.now(), endTimeVar.now())
                val updatedProject =
                  project.copy(
                    timeTracked =
                      project.timeTracked.add(addedTime)(using repId)
                  )
                // project.copy(timeTracked = project.timeTracked + addedTime)
                // Close the sidebar
                isTimeTrackingSidebarVisible.set(false)
                projectEventBus.emit(Updated(project.id, updatedProject))
              case None => println("Replica Id not set!")
          }
        )
      )
    )
  }

  // Helper function to calculate duration (in minutes) between two times
  private def calculateDuration(startTime: String, endTime: String): Int = {
    val startParts = startTime.split(":").map(_.toInt)
    val endParts = endTime.split(":").map(_.toInt)

    val startMinutes =
      startParts(0) * 60 + startParts(1) // Convert start time to total minutes
    val endMinutes =
      endParts(0) * 60 + endParts(1) // Convert end time to total minutes

    // If the end time is earlier than the start time, it means the end time is on the next day
    val durationInMinutes = if (endMinutes < startMinutes) {
      (endMinutes + 24 * 60) - startMinutes // Add 24 hours worth of minutes to end time
    } else {
      endMinutes - startMinutes
    }

    // Return the duration in minutes
    durationInMinutes
  }

  private def formatTime(durationInMinutes: Double): String = {
    val hours = (durationInMinutes / 60).toInt
    val minutes = (durationInMinutes % 60).toInt
    f"$hours%02d:$minutes%02d" // Format as "hh:mm"
  }
}
