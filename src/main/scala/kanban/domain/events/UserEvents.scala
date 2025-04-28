package kanban.domain.events

import kanban.domain.models.{User, UserId}

sealed trait UserEvent

object UserEvent {
  case class Added(user: User) extends UserEvent
  case class Deleted(userId: UserId) extends UserEvent
  case class Updated(userId: UserId, user: User) extends UserEvent
}
