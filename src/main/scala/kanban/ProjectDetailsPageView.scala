package kanban

import com.raquo.laminar.api.L.*
import kanban.KanbanBoardPageView.projectCommandBus
import kanban.models.*

import scala.scalajs.js.Date

object ProjectDetailsPageView {
  val revisors: List[String] =
    List("Manas", "Jakob", "Julian", "Bach", "Bearbeiter")
  val statusValues: List[String] = ProjectStatus.values.map(_.toString).toList

  // Function to render the project details page
  def apply(project: Project): HtmlElement = {
    // Temporary variables to hold the edited values
    val editedRevisorVar = Var(project.revisor.toString)
    val editedStatusVar = Var(project.status.toString)
    val editedDeadlineVar = Var(project.deadline)
    val isTimeTrackingSidebarVisible = Var(false)
    val timeInFormVar = Var(0.0)

    div(
      cls := "project-details",
      h2(project.name),

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

      // Display total time tracked for this project
      div(
        cls := "project-detail",
        span(cls := "project-detail-label", "Total Time Tracked: "),
        span(
          child.text <-- Var(project.timeTracked).signal.map(time =>
            f"$time%.2f hours"
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

      // Save button to apply changes
      button(
        cls := "save-button",
        "Speichern",
        onClick.map(_ =>
          val updatedProject = project.copy(
            revisor = Revisors.valueOf(editedRevisorVar.now()),
            status = ProjectStatus.valueOf(editedStatusVar.now()),
            deadline = editedDeadlineVar.now()
          )
          // Go back to Kanban view after saving
          KanbanBoardPageView.showKanbanBoard.set(true)
          KanbanBoardPageView.selectedProjectVar.set(
            None
          ) // Clear selected project

          ProjectCommands.update(project.id, updatedProject)
        ) --> projectCommandBus
      ),

      // Back button to return to Kanban board
      button(
        cls := "back-button",
        "Zurück",
        onClick --> { _ =>
          // Set the view back to Kanban board
          KanbanBoardPageView.showKanbanBoard.set(true)
          KanbanBoardPageView.selectedProjectVar.set(
            None
          ) // Clear selected project
        }
      )
    )
  }

  // Helper function to format dates as "YYYY-MM-DD"
  private def formatDate(date: Option[Date]): String = {
    date.map(_.toLocaleDateString()).getOrElse("Keine Fälligkeitsdatum")
  }

  // Function to render the time tracking form
  private def renderTimeTrackingSidebar(
      timeInFormVar: Var[Double],
      isTimeTrackingSidebarVisible: Var[Boolean],
      project: Project
  ): HtmlElement = {
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
          input(typ := "date") // Date input field
        ),
        div(
          cls := "form-field",
          span("Stunden: "),
          input(
            typ := "number",
            minAttr := "0",
            stepAttr := "0.5",
            onInput.mapToValue --> { value =>
              timeInFormVar.set(
                value.toDouble
              ) // Store the entered time locally
            }
          ) // Hours input field
        ),
        button(
          cls := "submit-button",
          "Speichern",
          onClick.map(_ =>
            val addedTime = timeInFormVar.now()
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
}
