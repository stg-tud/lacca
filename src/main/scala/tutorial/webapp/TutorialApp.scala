package tutorial.webapp

import org.scalajs.dom
import org.scalajs.dom.document
import org.scalajs.dom.html.{Anchor, Button, Div, Input}
import scala.scalajs.js
import scala.scalajs.js.annotation.JSExportTopLevel
import org.scalajs.dom.raw.Event

object TutorialApp {
  def main(args: Array[String]): Unit = {
    document.addEventListener("DOMContentLoaded", { (e: dom.Event) =>
      setupUI()
      setupDragAndDrop()
    })
  }

  def setupUI(): Unit = {
    // Create navigation bar
    val navBar = document.createElement("div").asInstanceOf[Div]
    navBar.id = "nav-bar"

    val links = List("Kanzleiboard", "Kalkulationen", "Angebote", "Rechnungen", "Controlling", "Zeiten", "Einstellungen")

    for (linkText <- links) {
      val link = document.createElement("a").asInstanceOf[Anchor]
      link.href = s"#${linkText.toLowerCase}"
      link.textContent = linkText
      link.setAttribute("class", "nav-link")
      navBar.appendChild(link)
    }

    document.body.appendChild(navBar)

    // Create Kanban board
    val kanbanBoard = document.createElement("div").asInstanceOf[Div]
    kanbanBoard.id = "kanban-board"

    val columns = List("Neu", "Geplant", "In Arbeit", "Abrechenbar", "Abgeschlossen")

    for (columnTitle <- columns) {
      val column = document.createElement("div").asInstanceOf[Div]
      column.setAttribute("class", "kanban-column")

      val columnHeader = document.createElement("h3")
      columnHeader.setAttribute("class", "kanban-column-header")
      columnHeader.textContent = columnTitle

      val columnContent = document.createElement("div").asInstanceOf[Div]
      columnContent.setAttribute("class", "kanban-column-content")
      columnContent.id = s"column-${columnTitle.toLowerCase.replace(" ", "-")}"

      column.appendChild(columnHeader)
      column.appendChild(columnContent)
      kanbanBoard.appendChild(column)
    }

    document.body.appendChild(kanbanBoard)

    // Create "Projekt hinzufügen" button
    val addButton = document.createElement("button").asInstanceOf[Button]
    addButton.textContent = "Projekt hinzufügen"
    addButton.id = "add-project-button"
    addButton.addEventListener("click", { (e: dom.MouseEvent) =>
      openAddProjectForm()
    })

    document.body.appendChild(addButton)
  }

  def openAddProjectForm(): Unit = {
    // Create form
    val formOverlay = document.createElement("div").asInstanceOf[Div]
    formOverlay.id = "form-overlay"

    val form = document.createElement("div").asInstanceOf[Div]
    form.id = "add-project-form"

    val formTitle = document.createElement("h3")
    formTitle.textContent = "Neues Projekt hinzufügen"

    val input = document.createElement("input").asInstanceOf[Input]
    input.id = "project-name"
    input.placeholder = "Projektname eingeben"

    val submitButton = document.createElement("button").asInstanceOf[Button]
    submitButton.textContent = "Hinzufügen"
    submitButton.addEventListener("click", { (e: dom.MouseEvent) =>
      addProject()
      document.body.removeChild(formOverlay)
    })

    val cancelButton = document.createElement("button").asInstanceOf[Button]
    cancelButton.textContent = "Abbrechen"
    cancelButton.addEventListener("click", { (e: dom.MouseEvent) =>
      document.body.removeChild(formOverlay)
    })

    form.appendChild(formTitle)
    form.appendChild(input)
    form.appendChild(submitButton)
    form.appendChild(cancelButton)
    formOverlay.appendChild(form)
    document.body.appendChild(formOverlay)
  }

  def addProject(): Unit = {
    val projectName = document.getElementById("project-name").asInstanceOf[Input].value
    if (projectName.nonEmpty) {
      val newCard = document.createElement("div").asInstanceOf[Div]
      newCard.setAttribute("class", "kanban-card")
      newCard.textContent = projectName
      newCard.setAttribute("draggable", "true")

      val neuColumn = document.getElementById("column-neu").asInstanceOf[Div]
      neuColumn.appendChild(newCard)

      // Set initial data-x and data-y attributes for drag position
      newCard.setAttribute("data-x", "0")
      newCard.setAttribute("data-y", "0")

      // Re-initialize drag and drop for new card
      initializeDragAndDrop(newCard)
    }
  }

  def setupDragAndDrop(): Unit = {
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
        // Clear the inline styles and update data-x and data-y to be 0
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
        // Reset the position attributes for the draggable element
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
        // Clear the inline styles and update data-x and data-y to be 0
        target.style.transform = "translate(0, 0)"
        target.setAttribute("data-x", "0")
        target.setAttribute("data-y", "0")
      }
    ))
  }
}
