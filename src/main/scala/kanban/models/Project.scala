package kanban.models

enum ProjectStatus:
    case Neu, Geplant, InArbeit, Abrechenbar, Abgeschlossen

enum Revisors:
    case Manas, Jakob, Julian, Bach

case class Project(var name: String, var status: ProjectStatus, var revisor: Revisors)
