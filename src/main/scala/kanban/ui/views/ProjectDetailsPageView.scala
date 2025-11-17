package kanban.ui.views

import com.raquo.laminar.api.L.*
import kanban.controllers.ProjectController.projectEventBus
import kanban.controllers.{ProjectController, UserController}
import kanban.domain.events.ProjectEvent.Updated
import kanban.domain.models.User.*
import kanban.domain.models.{Project, ProjectStatus, User, UserId, UserPermission, Permission}
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
import kanban.auth.ProjectUcanService
import ucan.Base32

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

    // Signal with User objects which in the replica table
    val replicaUsersSignal: Signal[Seq[User]] = 
      EventStream.fromFuture(
        Replica.replicaIdTable.toArray().toFuture.flatMap { replicas =>
          getAllUsers().map { users =>
            replicas.toSeq.flatMap { r =>
              val id = r.userId.stripPrefix("🪪")
              users.find(_.id.delegate == id)
            }.distinctBy(_.id.delegate)
          }
        }
      ).toSignal(Seq.empty)

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

                  // Button to show all the users with their permissions
                  button(
                    cls := "toggle-users-button",
                    "Zugelassene Benutzer anzeigen",
                    onClick --> { _ => showPermittedUsersVar.update(!_)}
                  ),

                  child <-- showPermittedUsersVar.signal.combineWith(UserController.users.signal).map {
                    case (true, users) =>
                      val userSet: Set[UserPermission] = project.permittedUsers.map(_.value).getOrElse(Set.empty)
                      val userMap: Map[UserId, User] = users.map(u => u.id -> u).toMap                      
                      ul(
                        for (perm <- userSet.toList.sortBy(_.userId.delegate)) yield {
                          val userName = userMap.get(perm.userId).map(_.name.read).getOrElse("Unbekannt")
                          li(s"👤 $userName: ${perm.permission}")
                        }
                      )
                    case (false, _) => emptyNode
                  },

                  // Render the users with permission dropdowns
                  child <-- replicaUsersSignal.map { users =>
                    val currentPermissions: Set[UserPermission] = 
                      project.permittedUsers.map(_.value).getOrElse(Set.empty)

                    val permissionsByUserId: Map[UserId, String] =
                      currentPermissions.map(p => p.userId -> p.permission.toString).toMap

                    div(
                      h3("Benutzerberechtigungen"),
                      ul(
                        users.map { user =>
                          val currentPerm: String = permissionsByUserId.getOrElse(user.id, "None")
                          li(
                            span(user.name.read + ": "),
                            select(
                              option(value := "None", "None", selected := (currentPerm == "None")),
                              option(value := "Read", "Read", selected := (currentPerm == "Read")),
                              option(value := "Write", "Write", selected := (currentPerm == "Write")),
                              onChange.mapToValue --> { newPermission =>
                                val updatedSet: Set[UserPermission] = {
                                  val filtered = currentPermissions.filterNot(_.userId == user.id)
                                  if newPermission != "None" then
                                    filtered + UserPermission(user.id, Permission.valueOf(newPermission))
                                  else
                                    filtered
                                }

                                val updatedCRDT = project.permittedUsers match {
                                  case Some(crdt) => crdt.write(updatedSet)
                                  case None       => LastWriterWins(CausalTime.now(), updatedSet)
                                }

                                val updatedProject = project.copy(permittedUsers = Some(updatedCRDT))
                                projectEventBus.emit(Updated(project.id, updatedProject))

                                // Delegate permission via UCAN
                                delegatePermissionToUser(project.id, user, newPermission)
                              }
                            )
                          )
                        }
                      )
                    )
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

  // --- Minimal UCAN delegation with only one token for one user's public key ---
  private def delegatePermissionToUser(
    projectId: Uid,
    user: User,
    permission: String
    ): Unit = {
      findUserPublicKey(user.id).foreach {
        case Some(pubKeyStr) =>
          val audienceDid = makeDidFromPublicKey(pubKeyStr)
          ProjectUcanService
          .delegateToDid(projectId, audienceDid, Seq(permission))
          .onComplete {
            case Success(token) =>
              println(
                s"UCAN token created for ${user.name.read} ($permission): $token"
              )
            case Failure(ex) =>
              println(
                s"Failed to create UCAN token for ${user.name.read}: ${ex.getMessage}"
              )
          }
        case None =>
          println(s"No replica entry found for user ${user.id.delegate}")
      }
    }

  /** Finds and returns a user's public key from the replicaIdTable */
  private def findUserPublicKey(userId: UserId)
      : scala.concurrent.Future[Option[String]] = {
    Replica.replicaIdTable.toArray().toFuture.map { entries =>
      entries.find(e => e.userId.stripPrefix("🪪") == userId.delegate) match {
        case Some(matched) =>
          println(s"Found public key for user ${userId.delegate}: ${matched.publicKey}")
          Some(matched.publicKey)
        case None =>
          println(s"No public key found for user ${userId.delegate}")
          None
      }
    }
  }

  /** Converts a Base32-encoded Ed25519 public key into a did:key string */
  private def makeDidFromPublicKey(pubKeyStr: String): String = {
    val pubKeyBytes: Array[Byte] = Base32.decode(pubKeyStr)
    val prefix: Array[Byte] = Array(0xED.toByte, 0x01.toByte) // Ed25519 multicodec prefix
    val combined: Array[Byte] = prefix ++ pubKeyBytes
    val base58Encoded: String = ucan.Base58.encode(combined)
    s"did:key:z$base58Encoded"
  }
}
