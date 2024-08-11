package kanban

import org.scalajs.dom
import org.scalajs.dom.document

object MainApp {
  def main(args: Array[String]): Unit = {
    document.addEventListener("DOMContentLoaded", { (e: dom.Event) =>
      UI.setupUI()
      DragAndDrop.setupDragAndDrop()
    })
  }
}
