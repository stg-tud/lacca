package kanban.controllers

import com.raquo.airstream.ownership.ManualOwner
import com.raquo.laminar.api.L.{*, given}
import kanban.domain.events.ProjectEvent
import kanban.domain.models.{Project, ProjectStatus}
import kanban.routing.Pages.ProjectDetailsPage
import kanban.routing.Router
import kanban.service.ProjectService.*
import kanban.service.{ProjectService, TrysteroService}
import rdts.base.Lattice

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

object ProjectController {
  val projects: Var[List[Project]] = Var(List.empty)

  val projectEventBus: EventBus[ProjectEvent] = new EventBus[ProjectEvent]

  // Synchronization
  // send all projects when a new device joins
  TrysteroService.room.onPeerJoin(peerId =>
    projects
      .now()
      .foreach(project =>
        TrysteroService.sendProjectUpdate(project, List(peerId))
      )
  )

  // listen for updates from other peers
  TrysteroService.receiveProjectUpdate((newProject: Project) =>
    val projectId = newProject.id
    val oldProject: Future[Project] = ProjectService.getProjectById(projectId)
    val oldPlusMerged = oldProject.map { old => // old value and merged value
      (old, Lattice[Project].merge(old, newProject))
    }
    oldPlusMerged.onComplete {
      case Success((old, m)) =>
        if old <= m then
          ProjectService
            .updateProject(projectId, m) // check if merge inflates
      case Failure(_) =>
        ProjectService.updateProject(
          projectId,
          newProject
        ) // just apply remote update
    }
  )

  projectsObservable.subscribe(
    next = (queryResultFuture) =>
      queryResultFuture.onComplete {
        case Success(projectsSeq) => {
          projects.set(projectsSeq.toList)
        }
        case Failure(exception) =>
          println(s"Error observing projects: $exception")
      },
    error = (error) => println(s"Error observing projects: $error")
    // complete = ? (not needed)
  )

  projectEventBus.events.foreach {
    case ProjectEvent.Added(project) =>
      println(s"create project event received for $project")
      createProject(project)
      TrysteroService.sendProjectUpdate(project)
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
      TrysteroService.sendProjectUpdate(updatedProject)
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
      getProjectById(id).onComplete {
        case Success(p) =>
          TrysteroService.sendProjectUpdate(
            p.copy(status = p.status.write(newStatus))
          )
        case Failure(exception) => throw exception
      }
      updateProjectStatusById(id, newStatus).onComplete {
        case Success(_) =>
          println(
            s"Project with id: ${id.toString} status updated successfully!"
          )
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
