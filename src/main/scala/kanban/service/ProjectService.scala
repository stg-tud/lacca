package kanban.service

import kanban.domain.models.{Project, ProjectId, ProjectJsObject, ProjectStatus}
import kanban.persistence.DexieDB.dexieDB
import org.getshaka.nativeconverter.NativeConverter
import org.scalablytyped.runtime.StringDictionary
import rdts.base.Uid
import rdts.datatypes.LastWriterWins
import rdts.time.CausalTime
import typings.dexie.mod.{Dexie, Table, UpdateSpec, liveQuery}
// import typings.dexieObservable.{liveQuery, LiveQueryResult}

import kanban.service.UserService.getAllUsers
import typings.std.stdStrings.live

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.Date
import scala.scalajs.js.JSConverters.JSRichOption
import scala.util.{Failure, Success}

object ProjectService {
  private val projectsTable: Table[js.Any, String, js.Any] =
    dexieDB.table("projects")

  val projectsObservable = liveQuery(() => getAllProjects())

  def createProject(project: Project): Future[Any] = {
    println(s"createProject called!!")
    projectsTable.put(project.toNative).toFuture
  }

  def getAllProjects(): Future[Seq[Project]] = {
    println(s"getAllProjects called!!")
    projectsTable.toArray().toFuture.map { projectsJsArray =>
      projectsJsArray
        .map(entry => NativeConverter[Project].fromNative(entry))
        .toSeq
    }
  }

  def getProjectById(projectId: ProjectId): Future[Project] = {
    projectsTable.get(projectId.delegate).toFuture.map { projectJsObject =>
      if (projectJsObject.isEmpty) {
        throw new Exception(s"Project ${projectId.show} not found!!")
      } else {
        NativeConverter[Project].fromNative(projectJsObject.get)
      }
    }
  }

  def deleteProject(projectId: ProjectId): Future[Unit] = {
    projectsTable
      .delete(projectId.toString)
      .toFuture
  }

  def updateProject(projectId: ProjectId, project: Project): Future[String] = {
    println(s"updateProject called!!")
    projectsTable
      .put(project.toNative)
      .toFuture
  }

  def updateProjectStatusById(
      projectId: ProjectId,
      status: ProjectStatus
  ): Future[Unit] = {
    println(s"updateProjectStatusById service method called!!")
    val project =
      projectsTable.get(projectId.delegate).toFuture.map { projectJsObject =>
        if (projectJsObject.isEmpty) {
          throw (new Exception("Project not found!!"))
        } else {
          NativeConverter[Project].fromNative(projectJsObject.get)
        }
      }
    println(s"id: $projectId, new status: ${status.toString}")
    project.onComplete {
      case Success(p) => println(s"modified project: ${p.toString}")
      case _          => println("failed to fetch project")
    }
    project.map(p =>
      updateProject(p.id, p.copy(status = p.status.write(status)))
    )
  }
}
