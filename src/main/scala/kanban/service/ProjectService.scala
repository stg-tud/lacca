package kanban.service

import kanban.dexie.DexieDB.dexieDB
import kanban.models.{Project, ProjectJsObject, ProjectStatus}
import org.scalablytyped.runtime.StringDictionary
import typings.dexie.mod.{Dexie, Table}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.scalajs.js

object ProjectService {
  private val projectsTable: Table[ProjectJsObject, String, ProjectJsObject] = dexieDB.table("projects")

  def createProject(project: Project): Future[Any] = {
    println(s"createProject called!!")
    projectsTable.add(project.toJsObject).toFuture
  }

  def getAllProjects(): Future[Seq[Project]] = {
    projectsTable.toArray().toFuture.map { projectsJsArray =>
      projectsJsArray.map { projectJsObject =>
        Project(
          id = projectJsObject.id,
          name = projectJsObject.name,
          status = ProjectStatus.valueOf(projectJsObject.status),
          revisorId = projectJsObject.revisorId,
          deadline = Option(projectJsObject.deadline)
        )
      }.toSeq
    }
  }
}
