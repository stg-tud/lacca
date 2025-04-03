package kanban.routing

import com.raquo.laminar.api.L.*
import com.raquo.waypoint
import kanban.routing.Pages.Page
import org.getshaka.nativeconverter.NativeConverter

object Router
    extends waypoint.Router[Page](
      routes = routes,
      getPageTitle = _.title,
      serializePage = page =>  page.toJson,
      deserializePage = pageStr =>
        NativeConverter[Page].fromJson(pageStr),
      routeFallback = _ => Pages.KanbanBoardPage
    )(
      popStateEvents = windowEvents(
        _.onPopState
      ),
      owner = unsafeWindowOwner
    )
