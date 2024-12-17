package kanban

import com.raquo.laminar.api.L.*
import kanban.KanbanBoardPageView.projectCommandBus
import kanban.models.*
import kanban.Router.*
import kanban.Pages.*

import scala.scalajs.js.Date
import scala.scalajs.js

object ProjectDetailsPageView {
  val revisors: List[String] =
    List("Manas", "Jakob", "Julian", "Bach", "Bearbeiter")
  val statusValues: List[String] = ProjectStatus.values.map(_.toString).toList

  // Function to render the project details page
  def apply(projectDetailsPageSignal: Signal[ProjectDetailsPage]): HtmlElement = {
    // Temporary variables to hold the edited values
    val isTimeTrackingSidebarVisible = Var(false)
    val timeInFormVar = Var(0.0)
    val editedStatusVar = Var("")
    val editedRevisorVar = Var("")
    val editedDeadlineVar = Var[Option[Date]](None)

    div(
      cls := "project-details",
      child <-- projectDetailsPageSignal.combineWith(KanbanBoardPageView.projectsMap).map {
        case (ProjectDetailsPage(projectId), projectsMap) =>
          projectsMap.get(projectId) match {
            case Some(project) =>
              // Initialize editing variables with current project values
              editedStatusVar.set(project.status.toString)
              editedRevisorVar.set(project.revisor.toString)
              editedDeadlineVar.set(project.deadline)

              div(
                h2(s"Project: ${project.name}"),

                // Editable status dropdown
                div(
                  cls := "project-detail",
                  span(cls := "project-detail-label", "Status: "),
                  select(
                    statusValues.map(status =>
                      option(
                        value := status,
                        status,
                        selected := (status == project.status.toString)
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
                    revisors.map(revisor =>
                      option(
                        value := revisor,
                        revisor,
                        selected := (revisor == project.revisor.toString)
                      )
                    ),
                  onChange.mapToValue --> editedRevisorVar.set
                  )
                ),

                // Editable deadline input (date picker)
                div(
                  cls := "project-detail",
                  span(cls := "project-detail-label", "Fälligkeitsdatum: "),
                  input(
                    typ := "date",
                    value := project.deadline
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

                // Function to display the total time tracked in "hour:minute" format
                div(
                  cls := "project-detail",
                  span(cls := "project-detail-label", "Gesamte erfasste Zeit: "),
                  span(
                    child.text <-- Var(project.timeTracked).signal.map(timeInMinutes =>
                    formatTime(timeInMinutes)
                    )
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
                  onClick.map(_ =>
                    val updatedProject = project.copy(
                      status = ProjectStatus.valueOf(editedStatusVar.now()),
                      revisor = Revisors.valueOf(editedRevisorVar.now()),
                      deadline = editedDeadlineVar.now()
                    )
                    // Go back to Kanban view after saving
                    Router.pushState(KanbanBoardPage)

                    ProjectCommands.update(project.id, updatedProject)
                  ) --> projectCommandBus
                ),

                button(
                  "Zurück",
                  onClick --> { _ => Router.pushState(KanbanBoardPage) }
                )
              )
            case None =>
              div("Projekt nicht gefunden.")
          }
      }
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
        startTime <- Option(startTimeVar.now()).filter(_.matches("^(?:[01]\\d|2[0-3]):[0-5]\\d$"))
        endTime   <- Option(endTimeVar.now()).filter(_.matches("^(?:[01]\\d|2[0-3]):[0-5]\\d$"))
      } yield startTime < endTime // Lexicographical comparison for "HH:mm" format

      // If fields are missing or invalid, show an error message
      if (!isStartTimeFilled || !isEndTimeFilled || !isDateFilled) {
        errorMessageVar.set("Bitte füllen Sie alle Felder aus.")
      } else if (!isValidTime.getOrElse(true)) {
        errorMessageVar.set("Die Startzeit muss vor der Endzeit liegen.")
      } else {
        errorMessageVar.set("") // No errors
      }

      formValidVar.set(isStartTimeFilled && isEndTimeFilled && isDateFilled && isValidTime.getOrElse(false))
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
          case "" => emptyNode
          case message => div(cls := "error-message", message)
        },

        button(
          cls := "submit-button",
          "Speichern",
          disabled <-- formValidVar.signal.map(!_),
          onClick.map(_ =>
            val addedTime = calculateDuration(startTimeVar.now(), endTimeVar.now())
            // Update the project list reactively
            val updatedProject =
              project.copy(timeTracked = project.timeTracked + addedTime)

            // Close the sidebar
            isTimeTrackingSidebarVisible.set(false)

            ProjectCommands.update(project.id, updatedProject)
          ) --> projectCommandBus
        )
      )
    )
  }

  // Helper function to calculate duration (in minutes) between two times
  private def calculateDuration(startTime: String, endTime: String): Double = {
    val startParts = startTime.split(":").map(_.toInt)
    val endParts = endTime.split(":").map(_.toInt)

    val startMinutes = startParts(0) * 60 + startParts(1)  // Convert start time to total minutes
    val endMinutes = endParts(0) * 60 + endParts(1)      // Convert end time to total minutes

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
