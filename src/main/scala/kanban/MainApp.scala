package kanban

import com.raquo.laminar.api.L.{*, given}
import org.scalajs.dom
import org.scalajs.dom.document

object MainApp {
  def main(args: Array[String]): Unit = {
    //this element is the container for rendering views based on router state
    lazy val rootDivElement = dom.document.getElementById("root")

    //following element inserts the current page's content to root element
    lazy val appElement = {
      div(
        cls := "pageContent",
        child <-- views
      )
    }
    render(rootDivElement, appElement)
  }
}
