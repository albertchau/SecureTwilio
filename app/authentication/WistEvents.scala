package authentication

import play.api.mvc.{ Controller, Session, RequestHeader }
import securesocial.core.{EventListener,Event}

/**
 * Helper object to fire events
 */
object WistEvents {

  def doFire[U](list: List[EventListener[U]], event: Event[U],
                request: RequestHeader, session: Session): Session =
  {
    if (list.isEmpty) {
      session
    } else {
      val newSession = list.head.onEvent(event, request, session)
      doFire(list.tail, event, request, newSession.getOrElse(session))
    }
  }

  def fire[U](event: Event[U])(implicit request: RequestHeader, env: WistRuntimeEnvironment[U]): Option[Session] = {
    val result = doFire(env.eventListeners, event, request, request.session)
    if (result == request.session) None else Some(result)
  }
}
