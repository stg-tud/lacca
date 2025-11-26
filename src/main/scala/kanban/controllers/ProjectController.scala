package kanban.controllers

import com.raquo.airstream.ownership.ManualOwner
import com.raquo.laminar.api.L.{*, given}
import kanban.controllers.UserController.users
import kanban.domain.events.ProjectEvent
import kanban.domain.models.{Project, ProjectStatus}
import kanban.routing.Pages.ProjectDetailsPage
import kanban.routing.Router
import kanban.service.ProjectService.*
import kanban.service.ProjectService
import kanban.sync.ProjectSync.{receiveProjectUpdate, sendProjectUpdate}
import kanban.sync.TrysteroSetup
import kanban.sync.UserSync.sendUserUpdate
import rdts.base.Lattice
import rdts.datatypes.LastWriterWins
import rdts.time.CausalTime

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}
import kanban.sync.Replica
import ucan.Base32
import kanban.sync.ReplicaSync.{sendReplicaInfo, receiveReplicaInfo}
import kanban.sync.Replica.replicaIdTable
import kanban.sync.Replica.replicaDBEntry
import scala.scalajs.js
import kanban.ui.views.GlobalState
import kanban.sync.TokenSync

object ProjectController {
  val projects: Var[List[Project]] = Var(List.empty)

  val projectEventBus: EventBus[ProjectEvent] = new EventBus[ProjectEvent]

  // Temporary in-memory storage for pending replicas
  val pendingReplicas: Var[Map[String, String]] = Var(Map.empty)

  // Synchronization
  // send all projects when a new device joins
  // TODO: Temporary fix for sending project and user updates
  //  find a better way to do this!
  TrysteroSetup.room.onPeerJoin(peerId =>
    projects
      .now()
      .foreach(project => sendProjectUpdate(project, List(peerId)))
    users
      .now()
      .foreach(user => sendUserUpdate(user, List(peerId)))
    // send replica public key
    Replica.id.signal.combineWith(Replica.keyMaterial.signal).foreach {
      case (Some(replicaId), Some(km)) =>
        val pk = Base32.encode(km.publicKey)
        GlobalState.userIdVar.now() match
          case Some(uid) =>
            sendReplicaInfo(uid, replicaId.show, pk, List(peerId))
            println(s"[ProjectController] Sent replica info for userId=$uid")
          case None =>
            println("[ProjectController] Cannot send replica info: userId not ready")
        println(s"[ProjectController] Sent success")
      case _ => // not ready yet
    }
  )

  // Listen for replica info from other peers
  // TODO: Refactor this in another place
  receiveReplicaInfo { (userId, replicaId, publicKey, peerId) =>
    println(s"[ProjectController] Received success")
    Option(publicKey).foreach { pk =>
      // Check if this public key already exists in DB or pending
      val PublicKeys = replicaIdTable.toArray().toFuture.map { entries =>
        entries.exists(_.publicKey == pk) || pendingReplicas.now().values.toSet.contains(pk)
      }
      PublicKeys.foreach { alreadyStoredpk =>
        if !alreadyStoredpk then
          // Update the pendingReplicas map
          pendingReplicas.update(old => old + (replicaId -> pk))
          // Assign next free slot and store in DB
          replicaIdTable.toArray().toFuture.foreach { entries =>
            val usedSlots = entries.map(_.slot).toSet
            val newSlot   = Iterator.from(0).find(s => !usedSlots.contains(s)).get
            val entry = js.Dynamic.literal(
              slot = newSlot,
              localUid = replicaId,
              publicKey = pk,
              userId = userId
              ).asInstanceOf[replicaDBEntry]

            replicaIdTable.add(entry).toFuture.onComplete {
              case Success(_) =>
                println(s"[ProjectController] Stored replica info at slot=$newSlot with publicKey=$pk for user $userId")
                pendingReplicas.update(_ - replicaId)
              case Failure(ex) =>
                println(s"[ProjectController] Failed to store replica info: $ex")
            }
          }
        else
          println(s"[ProjectController] Public key $pk already stored, skipping redundant write")
      }
    }
  }

  // --- Minimal Token receive callback, only the message ---
  TokenSync.receiveToken { (projectId, userId, token) =>
    println(s"[ProjectController] Received UCAN token for project $projectId from user $userId: $token")
  }

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
