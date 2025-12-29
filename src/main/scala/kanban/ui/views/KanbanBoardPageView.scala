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

  def apply(): HtmlElement = {
    setupDragAndDrop()
    logAllUcanTokens()
    div(
      NavBar(),
      // Print out now only for debug
      child <-- Replica.id.signal.map {
        case Some(rid) =>
          println(s"[KanbanBoard] replicaId = ${rid.toString}")
          println(s"[KanbanBoard] current userId = ${kanban.ui.views.GlobalState.userIdVar.now().getOrElse("None")}")
          emptyNode
        case None =>
          println("[KanbanBoard] ReplicaId not set yet")
          emptyNode
      },
      // Print out now only for debug
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
                  selectedDeadlineVar.signal
                )
                .map {
                  (
                      projectsList: List[Project],
                      selectedRevisorId: Uid,
                      selectedDeadline: Option[Date]
                  ) =>
                    {
                      projectsList
                        .filter(p =>
                          p.status.value.toString == status &&
                            (selectedDeadline.isEmpty || p.deadline == selectedDeadline.get) &&
                            (selectedRevisorId == Uid.zero || p.revisorId.read == selectedRevisorId)
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

  // TODO: list only the tokens from the projects on the screen right now
  private def logAllUcanTokens(): Unit = {
    val visibleProjectsSignal: Signal[List[Project]] =
      ProjectController.projects.signal
        .combineWith(selectedRevisorIdVar.signal, selectedDeadlineVar.signal)
        .map { case (projectsList, selectedRevisorId, selectedDeadline) =>
          projectsList.filter { p =>
            (selectedDeadline.isEmpty || p.deadline == selectedDeadline.get) &&
              (selectedRevisorId == Uid.zero || p.revisorId.read == selectedRevisorId)
          }
        }

    (visibleProjectsSignal.combineWith(GlobalState.userIdVar.signal)).foreach {
      case (visibleProjects, maybeUserIdOpt) =>
        maybeUserIdOpt.foreach { currentUserId =>
          val visibleProjectIds = visibleProjects.map(_.id.delegate).toSet

          UcanTokenStore.listAll().foreach { tokens =>
            println("[KanbanBoard]========== UCAN TOKENS FOR CURRENT USER (VISIBLE PROJECTS, NEWEST ONLY) ==========")

            // Lookup userId for each token and filter by current user
            val tokensForCurrentUserFut = Future.sequence {
              tokens.map(token => 
                lookupUserIdByDid(token.aud).map(maybeUserId => (token, maybeUserId))
              )
            }

            tokensForCurrentUserFut.foreach { tokensWithUserId =>
              // Keep only tokens for current user
              val userTokens = tokensWithUserId.collect {
                case (token, Some(userId)) if userId == currentUserId => token
              }

              // Flatten token capabilities to (projectId, permission, token)
              val capsWithTokens = userTokens.flatMap { token =>
                token.capKeys.toOption.getOrElse(js.Array()).toSeq.map(parseCapability).map {
                  case (projectId, permission) => (projectId, permission, token)
                }
              }

              // Filter only visible projects
              val visibleCaps = capsWithTokens.filter { case (projectId, _, _) =>
                visibleProjectIds.contains(projectId)
              }

              // Group by projectId and pick the newest token
              val newestTokenPerProject = visibleCaps
                .groupBy(_._1) // group by projectId
                .map { case (projectId, caps) =>
                  caps.maxBy(_._3.createdAt) // pick the one with latest createdAt
                }

              newestTokenPerProject.foreach { case (projectId, permission, token) =>
                println(
                  s"""
                    |aud       : ${token.aud}
                    |userId    : $currentUserId
                    |projectId : $projectId
                    |permission: $permission
                    |createdAt : ${token.createdAt}
                    |--------------------------------
                    |""".stripMargin
                )
              }
            }
          }
        }
    }(using owner)
  }
}
