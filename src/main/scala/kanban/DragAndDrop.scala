package kanban

import org.scalajs.dom.html.Div
import scala.scalajs.js
import com.raquo.laminar.api.L.Var
import kanban.models.Project // Adjust the path if necessary

object DragAndDrop {
  def setupDragAndDrop(updateProjectStatus: (String, String) => Unit, projectList: Var[List[Project]]): Unit = {
    js.Dynamic.global.interact(".kanban-card").draggable(js.Dynamic.literal(
      inertia = true,
      autoScroll = true,
      onmove = (event: js.Dynamic) => {
        val target = event.target.asInstanceOf[Div]
        val x = (js.Dynamic.global.parseFloat(target.getAttribute("data-x")).asInstanceOf[Double] + event.dx.asInstanceOf[Double]).toDouble
        val y = (js.Dynamic.global.parseFloat(target.getAttribute("data-y")).asInstanceOf[Double] + event.dy.asInstanceOf[Double]).toDouble
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
    ))

    js.Dynamic.global.interact(".kanban-column-content").dropzone(js.Dynamic.literal(
      accept = ".kanban-card",
      overlap = 0.5,
      ondrop = (event: js.Dynamic) => {
        val draggableElement = event.relatedTarget.asInstanceOf[Div]
        val dropzoneElement = event.target.asInstanceOf[Div]
        dropzoneElement.appendChild(draggableElement)
        // Get the id attribute of dropzoneElement and remove the "column-" prefix
        val idAttr = dropzoneElement.id.replace("column-", "")
        println(s"Dropzone Status: $idAttr")
        val newStatus = idAttr
        val projectName = draggableElement.getAttribute("data-name")
        
        //right now it does not work so I comment it out
        //updateProjectStatus(projectName, newStatus)

        // Print the updated project list after the update
        println("Project list after drag-and-drop:")
        projectList.now().foreach { project =>
          println(s"Project Name: ${project.name}, Status: ${project.status}, Revisor: ${project.revisor}, Deadline: ${project.deadline}")
        }

        draggableElement.setAttribute("data-x", "0")
        draggableElement.setAttribute("data-y", "0")
        draggableElement.style.transform = "translate(0, 0)"
      }
    ))
  }

  def initializeDragAndDrop(card: Div): Unit = {
    js.Dynamic.global.interact(card).draggable(js.Dynamic.literal(
      inertia = true,
      autoScroll = true,
      onmove = (event: js.Dynamic) => {
        val target = event.target.asInstanceOf[Div]
        val x = (js.Dynamic.global.parseFloat(target.getAttribute("data-x")).asInstanceOf[Double] + event.dx.asInstanceOf[Double]).toDouble
        val y = (js.Dynamic.global.parseFloat(target.getAttribute("data-y")).asInstanceOf[Double] + event.dy.asInstanceOf[Double]).toDouble
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
    ))
  }
}
