package kanban.ui.views

import com.raquo.laminar.api.L.{*, given}
import kanban.controllers.{ProjectController, UserController}
import kanban.domain.models.{Project, ProjectStatus, UserId}
import kanban.sync.Replica
import kanban.ui.components.{KanbanColumn, NavBar}
import kanban.ui.views.DragAndDrop.setupDragAndDrop
import rdts.base.Uid

import scala.scalajs.js.Date
import scala.scalajs.js
import kanban.service.UcanTokenStore
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import kanban.utils.UserKeyUtils.*
import kanban.sync.Replica.owner

object KanbanBoardPageView {

  println(s"current replicaId: ${Replica.id.now()}")
  val selectedDeadlineVar: Var[Option[Date]] = Var(Option.empty[Date])
  val selectedRevisorIdVar: Var[UserId] = Var(Uid.zero)
  val projectStatusValues: List[String] =
    ProjectStatus.values.map(_.toString).toList
  val toggleDisplay: Var[String] = Var("none")
  val permittedProjectIdsVar: Var[Set[String]] = Var(Set.empty)

  def apply(): HtmlElement = {
    setupDragAndDrop()
    getAllPermittedProjects() // Print out all the permitted projects for this user
    div(
      NavBar(),
      div(
        idAttr := "kanbanboard-container",
        div(
          // date filter
          idAttr := "filter-container",
          input(
            typ := "date",
            onInput.mapToValue --> { dateStr =>
              if (dateStr.nonEmpty) {
                selectedDeadlineVar.set(
                  Some(new Date(dateStr))
                )
              } else {
                selectedDeadlineVar.set(None)
              }
            }
          ),
          // Revisor Filter
          select(
            idAttr := "revisor",
            option(
              value := "",
              selected := true,
              hidden := true,
              disabled := true,
              "Bearbeiter"
            ),
            option(
              value := Uid.zero.delegate, // Value for "All Revisors"
              "Bearbeiter" // Label for the "All Revisors" option
            ),
            children <-- UserController.users.signal.map { users =>
              users.map { user =>
                option(value := user.id.delegate, user.name.read)
              }
            },
            onChange.mapToValue --> { value =>
              selectedRevisorIdVar.set(Uid.predefined(value))
            }
          )
        ),

        // kanban-board
        div(
          idAttr := "kanban-board",
          projectStatusValues.map { status =>
            KanbanColumn(
              title = status,
              projects = ProjectController.projects.signal
                .combineWith(
                  selectedRevisorIdVar.signal,
                  selectedDeadlineVar.signal,
                  permittedProjectIdsVar.signal
                )
                .map {
                  (
                      projectsList: List[Project],
                      selectedRevisorId: Uid,
                      selectedDeadline: Option[Date],
                      permittedProjectIds
                  ) =>
                    {
                      projectsList
                        .filter(p =>
                          p.status.value.toString == status &&
                            (selectedDeadline.isEmpty || p.deadline == selectedDeadline.get) &&
                            (selectedRevisorId == Uid.zero || p.revisorId.read == selectedRevisorId) &&
                            permittedProjectIds.contains(p.id.delegate)
                        )
                    }
                }
            )
          }
        ),
        button(
          idAttr := "add-project-button",
          "Projekt hinzufügen",
          onClick --> { e =>
            {
              toggleDisplay.update(t => "")
            }
          }
        ),
        div(
          idAttr := "form-overlay",
          display <-- toggleDisplay,
          AddProjectFormView()
        )
      )
    )
  }

  private def getAllPermittedProjects(): Unit = {
    // Get all visible projects
    val visibleProjectsSignal: Signal[List[Project]] =
      ProjectController.projects.signal
        .combineWith(selectedRevisorIdVar.signal, selectedDeadlineVar.signal)
        .map { case (projectsList, selectedRevisorId, selectedDeadline) =>
          projectsList.filter { p =>
            (selectedDeadline.isEmpty || p.deadline == selectedDeadline.get) &&
              (selectedRevisorId == Uid.zero || p.revisorId.read == selectedRevisorId)
          }
        }

    // Combine visible projects with current user
    (visibleProjectsSignal.combineWith(GlobalState.userIdVar.signal)).foreach {
      case (visibleProjects, maybeUserIdOpt) =>
        maybeUserIdOpt.foreach { currentUserId =>
          val visibleProjectIds = visibleProjects.map(_.id.delegate).toSet

          UcanTokenStore.listAll().foreach { tokens =>
            val userTokensFut = Future.sequence {
              tokens.map(token => lookupUserIdByDid(token.aud).map(_.filter(_ == currentUserId).map(_ => token)))
            }

            userTokensFut.foreach { userTokens =>
              val capsWithTokens = userTokens.flatten.flatMap { token =>
                token.capKeys.toOption.getOrElse(js.Array()).toSeq.map(parseCapability).map {
                  case (projectId, permission) => (projectId, permission, token)
                }
              }

              val visibleCaps = capsWithTokens.filter { case (projectId, _, _) =>
                visibleProjectIds.contains(projectId)
              }

              // Only newest token per project with permission not "None"
              val newestAllowedProjects = visibleCaps
                .groupBy(_._1)
                .collect {
                  case (projectId, caps) =>
                    val newest = caps.maxBy(_._3.createdAt)
                    if newest._2 != "None" then
                      println(
                        s"""
                          |aud       : ${newest._3.aud}
                          |userId    : $currentUserId
                          |projectId : ${newest._1}
                          |permission: ${newest._2}
                          |createdAt : ${newest._3.createdAt}
                          |--------------------------------
                          |""".stripMargin
                      )
                      Some(newest._1)
                    else None
                }.flatten.toSet

              permittedProjectIdsVar.set(newestAllowedProjects)
            }
          }
        }
    }(using owner)
  }
}
