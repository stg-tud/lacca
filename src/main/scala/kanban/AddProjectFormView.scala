package kanban

import org.scalajs.dom
import org.scalajs.dom.document
import com.raquo.laminar.api.L.{*, given}
import kanban.KanbanBoardPageView.*
import kanban.models.*
import com.raquo.laminar.api.features.unitArrows

object AddProjectFormView {
  val projectName = Var("")
  val projectStatus = Var(ProjectStatus.Neu.toString())
  val projectStatusValues: List[String] =
    ProjectStatus.values.map(_.toString).toList

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
        button(
          typ := "submit",
          idAttr := "submit-button",
          "Hinzufügen",
          onClick --> { e =>
            {
              toggleDisplay.update(t => "none")
              KanbanBoardPageView.addNewProject(
                Project(
                  projectName.now(),
                  ProjectStatus.valueOf(projectStatus.now())
                )
              )
            }
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
