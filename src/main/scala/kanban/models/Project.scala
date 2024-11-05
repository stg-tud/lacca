package kanban.models

import scala.scalajs.js.Date

enum ProjectStatus:
    case Neu, Geplant, InArbeit, Abrechenbar, Abgeschlossen

enum Revisors:
    case Manas, Jakob, Julian, Bach

case class Project(
    val name: String,
    val status: ProjectStatus,
    val revisor: Revisors,
    val deadline: Option[Date],
    val timeTracked: Double
    )
