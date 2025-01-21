package kanban

import com.raquo.laminar.api.L.{*, given}
import kanban.routing.currentView
import org.scalajs.dom
import org.scalajs.dom.document

object main {
    def main(args: Array[String]): Unit = {
        lazy val rootDivElement = dom.document.getElementById("root")

        //following element inserts the current page's content to root element
        lazy val appElement = {
            div(
                cls := "pageContent",
                child <-- currentView
            )
        }
        render(rootDivElement, appElement)
    }
}
