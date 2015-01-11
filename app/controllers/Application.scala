/**
 * Copyright 2012 Jorge Aliss (jaliss at gmail dot com) - twitter: @jaliss
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package controllers

import authentication.{WistSecureSocial, WistRuntimeEnvironment}
import models.BasicUser
import securesocial.core._
//import service.DemoUser
import play.api.mvc.{ Action, RequestHeader }

class Application(override implicit val env: WistRuntimeEnvironment[BasicUser]) extends WistSecureSocial[BasicUser] {
  def index = SecuredAction { implicit request =>
    Ok(views.html.index(request.user.main))
  }

  def nonSecureIndex = Action { implicit request =>
    request.headers.toSimpleMap.foreach(println)
    Ok
  }

  // a sample action using an authorization implementation
  def onlyTwitter = SecuredAction(WithProvider("twitter")) { implicit request =>
    Ok("You can see this because you logged in using Twitter")
  }

  def linkResult = SecuredAction { implicit request =>
    Ok(views.html.linkResult(request.user))
  }

  /**
   * Sample use of SecureSocial.currentUser. Access the /current-user to test it
   */
  def currentUser = Action.async { implicit request =>
    import play.api.libs.concurrent.Execution.Implicits._
    WistSecureSocial.currentUser[BasicUser].map { maybeUser =>
      val userId = maybeUser.map(_.main.email).getOrElse("unknown")
      Ok(s"Your id is $userId")
    }
  }
}

// An Authorization implementation that only authorizes uses that logged in using twitter
case class WithProvider(provider: String) extends Authorization[BasicUser] {
  def isAuthorized(user: BasicUser, request: RequestHeader) = {
    user.main.providerId == provider
  }
}