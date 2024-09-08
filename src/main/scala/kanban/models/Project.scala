package kanban.models

enum ProjectStatus:
    case Neu, Geplant, InArbeit, Abrechenbar, Abgeschlossen

case class Project(var name: String, var status: ProjectStatus)
