package kanban.service

import kanban.domain.models.{Project, ProjectId, ProjectJsObject, ProjectStatus}
import kanban.persistence.DexieDB.dexieDB
import org.scalablytyped.runtime.StringDictionary
import typings.dexie.mod.{Dexie, Table, UpdateSpec, liveQuery}
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
    private val projectsTable: Table[ProjectJsObject, Int, ProjectJsObject] =
        dexieDB.table("projects")

    val projectsObservable = liveQuery(() => getAllProjects())

    def createProject(project: Project): Future[Any] = {
        println(s"createProject called!!")
        projectsTable.add(project.toJsObject).toFuture
    }
    
    def getAllProjects(): Future[Seq[Project]] = {
        println(s"getAllProjects called!!")
        projectsTable.toArray().toFuture.map { projectsJsArray =>
            projectsJsArray.map(fromJsObject(_)).toSeq
        }
    }

    def getProjectById(project: ProjectId): Future[Project] = {
        projectsTable.get(project.getOrElse(0)).toFuture.map { projectJsObject =>
            if (projectJsObject.isEmpty) {
                return Future.failed(new Exception("Project not found!!"))
            } else {
                fromJsObject(projectJsObject.get)
            }
        }
    }

    getProjectById(Option(2)).onComplete{
        case Success(value) => println(s"getProjectById completed successfully!!")
        case Failure(exception) => println(s"getProjectById failed!! Exception: $exception")
    }

    // create method to delete project
    def deleteProject(projectId: ProjectId): Future[Unit] = {
        projectsTable.delete(projectId.getOrElse {
            println(s"projectId is not defined!!")
            0
        }).toFuture
    }

    def updateProject(projectId: ProjectId, project: Project): Future[Unit] = {
        println(s"updateProject called!!")
        projectsTable.update(projectId.getOrElse(0), js.Dynamic.literal(
            "name" -> project.name,
            "status" -> project.status.toString,
            "revisorId" -> project.revisorId.orUndefined,
            "deadline" -> project.deadline.orUndefined
        ).asInstanceOf[UpdateSpec[ProjectJsObject]]).toFuture.map(_ => ())
    }

    def updateProjectStatusById(
        projectId: ProjectId, 
        status: ProjectStatus): Future[Unit] = {
            println(s"updateProjectStatusById service method called!!")
            projectsTable.update(projectId.getOrElse(0), js.Dynamic.literal(
            "status" -> status.toString
        ).asInstanceOf[UpdateSpec[ProjectJsObject]]).toFuture.map(_ => ())
    }

    def fromJsObject(projectJsObject: ProjectJsObject): Project = {
        Project(
            id = projectJsObject.id.toOption,
            name = projectJsObject.name,
            status = ProjectStatus.valueOf(projectJsObject.status),
            revisorId = projectJsObject.revisorId.toOption,
            deadline = projectJsObject.deadline.toOption,
            timeTracked = projectJsObject.timeTracked
        )
    }
}
