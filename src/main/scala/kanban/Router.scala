package kanban

import kanban.Pages.Page
import kanban.routes
import com.raquo.laminar.api.L.*
import com.raquo.waypoint
import io.bullet.borer.*
import org.scalajs.dom
import java.nio.charset.StandardCharsets

object Router
    extends waypoint.Router[Page](
      routes = routes,
      getPageTitle = _.title,
      serializePage = page => Json.encode(page).toUtf8String,
      deserializePage = pageStr =>
        Json.decode(pageStr.getBytes(StandardCharsets.UTF_8)).to[Page].value,
      routeFallback = _ => Pages.LoginPage
    )(
      popStateEvents = windowEvents(
        _.onPopState
      ),
      owner = unsafeWindowOwner
    )
