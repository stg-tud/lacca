package kanban.controllers

import com.raquo.airstream.ownership.ManualOwner
import com.raquo.laminar.api.L.{*, given}
import kanban.domain.events.ProjectEvent
import kanban.domain.models.{Project, ProjectStatus}
import kanban.routing.Pages.ProjectDetailsPage
import kanban.routing.Router
import kanban.service.ProjectService.*
import kanban.service.ProjectService
import kanban.sync.ProjectSync.{receiveProjectUpdate, sendProjectUpdate}
import kanban.sync.TrysteroSetup
import rdts.base.Lattice
import rdts.datatypes.LastWriterWins
import rdts.time.CausalTime

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

object ProjectController {
  val projects: Var[List[Project]] = Var(List.empty)

  val projectEventBus: EventBus[ProjectEvent] = new EventBus[ProjectEvent]

  // Synchronization
  // send all projects when a new device joins
  // TODO: Also send updates for users
  TrysteroSetup.room.onPeerJoin(peerId =>
    projects
      .now()
      .foreach(project => sendProjectUpdate(project, List(peerId)))
  )

  // listen for updates from other peers
  receiveProjectUpdate((newProject: Project) =>
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
          projects.set(
            projectsSeq.filter(p => !p.deleted.exists(_.value)).toList
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
      println(s"create project event received for $project")
      createProject(project)
      sendProjectUpdate(project)
    case ProjectEvent.Deleted(id) =>
      println("delete project event received")
      getProjectById(id).onComplete {
        case Success(project) =>
          val updated = project.copy(deleted = Some(LastWriterWins(CausalTime.now(), true)))
          updateProject(id, updated).onComplete {
            case Success(_) =>
              println(s"Project with id: ${id.toString} marked as deleted successfully!")
              sendProjectUpdate(updated)
              // Immediately reflect deletion in the UI
              val updatedList = projects.now().filterNot(_.id == id)
              projects.set(updatedList)           
            case Failure(exception) =>
              println(s"Failed to mark project as deleted: $exception")
          }          
        case Failure(exception) =>
          println(s"Failed to fetch project for deletion: $exception")
      }
      // This code deletely the project locally without the deleted tag
      /*
      deleteProject(id).onComplete {
        case Success(_) =>
          println(s"Project with id: ${id.toString} deleted successfully!")
        case Failure(exception) =>
          println(
            s"Failed to delete project with id: ${id.toString}. Exception: $exception"
          )
      }
      */
    case ProjectEvent.Updated(id, updatedProject) =>
      println("update project event received")
      sendProjectUpdate(updatedProject)
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
          sendProjectUpdate(
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
      println(s"Clicked on project with id: ${projectId.toString}")
      Router.pushState(ProjectDetailsPage(projectId))
  }

  def loadProjects(): Unit = {
    println("load projects called!")
    getAllProjects().onComplete {
      case Success(data)      =>
        projects.set(
          data.filter(p => !p.deleted.exists(_.value)).toList
        )
      case Failure(exception) => println(s"Failed to load projects: $exception")
    }
  }

  given ManualOwner()
}
