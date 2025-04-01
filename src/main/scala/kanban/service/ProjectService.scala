package kanban.service

import kanban.domain.models.{Project, ProjectId, ProjectJsObject, ProjectStatus}
import kanban.persistence.DexieDB.dexieDB
import org.scalablytyped.runtime.StringDictionary
import rdts.base.Uid
import rdts.datatypes.LastWriterWins
import rdts.time.CausalTime
import typings.dexie.mod.{Dexie, Table, UpdateSpec, liveQuery}
import kanban.domain.models.Project.fromJsObject
// import typings.dexieObservable.{liveQuery, LiveQueryResult}

import scala.scalajs.js.JSConverters.JSRichOption
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.Date
import scala.util.{Failure, Success}
import typings.std.stdStrings.live
import kanban.service.UserService.getAllUsers

object ProjectService {
  private val projectsTable: Table[ProjectJsObject, String, ProjectJsObject] =
    dexieDB.table("projects")

  val projectsObservable = liveQuery(() => getAllProjects())

  def createProject(project: Project): Future[Any] = {
    println(s"createProject called!!")
    projectsTable.add(Project.toJsObject(project)).toFuture
  }

  def getAllProjects(): Future[Seq[Project]] = {
    println(s"getAllProjects called!!")
    projectsTable.toArray().toFuture.map { projectsJsArray =>
      projectsJsArray.map(Project.fromJsObject(_)).toSeq
    }
  }

  def getProjectById(projectId: ProjectId): Future[Project] = {
    projectsTable.get(projectId.toString).toFuture.map { projectJsObject =>
      if (projectJsObject.isEmpty) {
        return Future.failed(new Exception("Project not found!!"))
      } else {
        fromJsObject(projectJsObject.get)
      }
    }
  }
  
  def deleteProject(projectId: ProjectId): Future[Unit] = {
    projectsTable
      .delete(projectId.toString)
      .toFuture
  }

  def updateProject(projectId: ProjectId, project: Project): Future[Unit] = {
    println(s"updateProject called!!")
    projectsTable
      .update(
        projectId.toString,
        Project.toJsObject(project).asInstanceOf[UpdateSpec[ProjectJsObject]]
//        js.Dynamic
//          .literal(
//            "name" -> project.name,
//            "status" -> project.status.toString,
//            "revisorId" -> project.revisorId.orUndefined,
//            "deadline" -> project.deadline.orUndefined
//          )
//          .asInstanceOf[UpdateSpec[ProjectJsObject]]
      )
      .toFuture
      .map(_ => ())
  }

  def updateProjectStatusById(
      projectId: ProjectId,
      status: ProjectStatus
  ): Future[Unit] = {
    println(s"updateProjectStatusById service method called!!")
    projectsTable
      .update(
        projectId.toString,
        js.Dynamic
          .literal(
            "status" -> status.toString
          )
          .asInstanceOf[UpdateSpec[ProjectJsObject]]
      )
      .toFuture
      .map(_ => ())
  }
}
