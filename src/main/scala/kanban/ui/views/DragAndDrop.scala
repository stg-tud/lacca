package kanban.ui.views

import scala.scalajs.js
import kanban.domain.models.{ProjectId, ProjectStatus}
import org.scalajs.dom.html.Div
import kanban.controllers.ProjectController.projectEventBus
import kanban.domain.events.ProjectEvent.Updated
import kanban.domain.events.ProjectEvent.StatusModified
import rdts.base.Uid

object DragAndDrop {
  var isDragging: Boolean = false

  def setupDragAndDrop(): Unit = {
    js.Dynamic.global
      .interact(".kanban-card")
      .draggable(
        js.Dynamic.literal(
          inertia = true,
          autoScroll = true,
          onstart = (event: js.Dynamic) => {
            isDragging = true
          },
          onmove = (event: js.Dynamic) => {
            val target = event.target.asInstanceOf[Div]
            val x = (js.Dynamic.global
              .parseFloat(target.getAttribute("data-x"))
              .asInstanceOf[Double] + event.dx.asInstanceOf[Double]).toDouble
            val y = (js.Dynamic.global
              .parseFloat(target.getAttribute("data-y"))
              .asInstanceOf[Double] + event.dy.asInstanceOf[Double]).toDouble
            target.style.transform = s"translate(${x}px, ${y}px)"
            target.setAttribute("data-x", x.toString)
            target.setAttribute("data-y", y.toString)
          },
          onend = (event: js.Dynamic) => {
            val target = event.target.asInstanceOf[Div]
            target.style.transform = "translate(0, 0)"
            target.setAttribute("data-x", "0")
            target.setAttribute("data-y", "0")
            js.timers.setTimeout(50) {
              isDragging = false
            }
          }
        )
      )

    js.Dynamic.global
      .interact(".kanban-column-content")
      .dropzone(
        js.Dynamic.literal(
          accept = ".kanban-card",
          overlap = 0.5,
          ondrop = (event: js.Dynamic) => {
            print("dropped")
            val draggableElement = event.relatedTarget.asInstanceOf[Div]
            val dropzoneElement = event.target.asInstanceOf[Div]
            val idAttr = dropzoneElement.id.replace("column-", "")
            println(s"Dropzone Status: $idAttr")
            val newStatus = ProjectStatus.valueOf(idAttr)
            val projectName = draggableElement.getAttribute("data-name")
            val projectId: ProjectId =
              Uid.predefined(draggableElement.getAttribute("data-project-id"))

            //TODO: check if the printed projectId is correct
            println(s"projectId when dropped: $projectId")
            projectEventBus.emit(StatusModified(projectId, newStatus))
            

            draggableElement.setAttribute("data-x", "0")
            draggableElement.setAttribute("data-y", "0")
            draggableElement.style.transform = "translate(0, 0)"
          }
        )
      )
  }

  def initializeDragAndDrop(card: Div): Unit = {
    js.Dynamic.global
      .interact(card)
      .draggable(
        js.Dynamic.literal(
          inertia = true,
          autoScroll = true,
          onmove = (event: js.Dynamic) => {
            val target = event.target.asInstanceOf[Div]
            val x = (js.Dynamic.global
              .parseFloat(target.getAttribute("data-x"))
              .asInstanceOf[Double] + event.dx.asInstanceOf[Double]).toDouble
            val y = (js.Dynamic.global
              .parseFloat(target.getAttribute("data-y"))
              .asInstanceOf[Double] + event.dy.asInstanceOf[Double]).toDouble
            target.style.transform = s"translate(${x}px, ${y}px)"
            target.setAttribute("data-x", x.toString)
            target.setAttribute("data-y", y.toString)
          },
          onend = (event: js.Dynamic) => {
            val target = event.target.asInstanceOf[Div]
            target.style.transform = "translate(0, 0)"
            target.setAttribute("data-x", "0")
            target.setAttribute("data-y", "0")
          }
        )
      )
  }
}
