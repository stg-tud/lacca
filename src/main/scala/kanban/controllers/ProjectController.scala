package kanban.controllers

import com.raquo.airstream.ownership.ManualOwner
import com.raquo.laminar.api.L.{*, given}
import kanban.domain.events.ProjectEvent
import kanban.domain.models.{Project, ProjectStatus}
import kanban.routing.Pages.ProjectDetailsPage
import kanban.routing.Router
import kanban.service.ProjectService.*
import kanban.service.{ProjectService, TrysteroService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

object ProjectController {
  val projects: Var[List[Project]] = Var(List.empty)

  val projectEventBus: EventBus[ProjectEvent] = new EventBus[ProjectEvent]

  projectsObservable.subscribe(
    next = (queryResultFuture) =>
      queryResultFuture.onComplete {
        case Success(projectsSeq) => {
          projects.set(projectsSeq.toList)
          println(s"Projects changed: ${projectsSeq.toList}")
          TrysteroService.sendMessage(
            s"Projects changed: ${projectsSeq.toList}"
          )
        }
        case Failure(exception) =>
          println(s"Error observing projects: $exception")
      },
    error = (error) => println(s"Error observing projects: $error")
    // complete = ? (not needed)
  )

  projectEventBus.events.foreach {
    case ProjectEvent.Added(project) =>
      println("create project event received")
      createProject(project).onComplete {
        case Success(_) =>
          println(s"Project with id: ${project.id.toString} added successfully!")
        case Failure(exception) =>
          println(
            s"Failed to add project with id: ${project.id.toString}. Exception: $exception"
          )
      }
    case ProjectEvent.Deleted(id) =>
      println("delete project event received")
      deleteProject(id).onComplete {
        case Success(_) =>
          println(s"Project with id: ${id.toString} deleted successfully!")
        case Failure(exception) =>
          println(
            s"Failed to delete project with id: ${id.toString}. Exception: $exception"
          )
      }
    case ProjectEvent.Updated(id, updatedProject) =>
      println("update project event received")
      updateProject(id, updatedProject).onComplete {
        case Success(_) =>
          println(s"Project with id: ${id.toString} updated successfully!")
        case Failure(exception) =>
          println(
            s"Failed to update project with id: ${id.toString}. Exception: $exception"
          )
      }
    case ProjectEvent.StatusModified(id, newStatus) =>
      println(
        s"Status modification event received for project with id: ${id.toString}"
      )
      updateProjectStatusById(id, newStatus).onComplete {
        case Success(_) =>
          println(s"Project with id: ${id.toString} status updated successfully!")
        case Failure(exception) =>
          println(
            s"Failed to update project with id: ${id.toString} status. Exception: $exception"
          )
      }

    case ProjectEvent.ClickedOn(projectId) =>
      println(s"Clicked on project with id:}")
      Router.pushState(ProjectDetailsPage(projectId))
  }

  def loadProjects(): Unit = {
    println("load projects called!")
    getAllProjects().onComplete {
      case Success(data)      => projects.set(data.toList)
      case Failure(exception) => println(s"Failed to load projects: $exception")
    }
  }

  given ManualOwner()
}
