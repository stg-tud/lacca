package kanban.models

import scala.scalajs.js.Date

type ProjectId = String

enum ProjectStatus:
    case Neu, Geplant, InArbeit, Abrechenbar, Abgeschlossen

enum Revisors:
    case Manas, Jakob, Julian, Bach

case class Project(id: ProjectId,name: String,status: ProjectStatus, revisor: Revisors, deadline: Option[Date], timeTracked: Double = 0)
