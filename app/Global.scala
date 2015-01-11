/**
 * Copyright 2014 Jorge Aliss (jaliss at gmail dot com) - twitter: @jaliss
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

import java.lang.reflect.Constructor

import authentication.WistRuntimeEnvironment
import models.BasicUser
import play.api._
import securesocial.core.authenticator.{HttpHeaderAuthenticatorBuilder, CookieAuthenticatorBuilder}
import securesocial.core.services.AuthenticatorService
import service.{SlickAuthenticatorStore, MyEventListener, SlickTwilioUserService}


object Global extends GlobalSettings {

  /**
   * The runtime environment for this sample app.
   */

  object MyRuntimeEnvironment extends WistRuntimeEnvironment.Default[BasicUser] {
    //    override lazy val routes = new CustomRoutesService()
    override lazy val eventListeners = List(new MyEventListener())
    override lazy val userService: SlickTwilioUserService = new SlickTwilioUserService()

    override lazy val authenticatorService: AuthenticatorService[BasicUser] = new AuthenticatorService[BasicUser](
      new CookieAuthenticatorBuilder[BasicUser](new SlickAuthenticatorStore, idGenerator),
      new HttpHeaderAuthenticatorBuilder[BasicUser](new SlickAuthenticatorStore, idGenerator)
    )
    //    override lazy val providers = ListMap (
    //      // oauth 2 client providers
    //      include(new FacebookProvider(routes, cacheService, oauth2ClientFor(FacebookProvider.Facebook))),
    //      include(new FoursquareProvider(routes, cacheService, oauth2ClientFor(FoursquareProvider.Foursquare))),
    //      include(new GoogleProvider(routes, cacheService, oauth2ClientFor(GoogleProvider.Google))),
    //      include(new InstagramProvider(routes, cacheService, oauth2ClientFor(InstagramProvider.Instagram))),
    //      // oauth 1 client providers
    //      include(new TwitterProvider(routes, cacheService, oauth1ClientFor(TwitterProvider.Twitter))),
    //      // username password
    //      include(new UsernamePasswordProvider[BasicUser](userService, avatarService, viewTemplates, passwordHashers))
    //      )
  }

  /**
   * An implementation that checks if the controller expects a RuntimeEnvironment and
   * passes the instance to it if required.
   *
   * This can be replaced by any DI framework to inject it differently.
   *
   * @param controllerClass
   * @tparam A
   * @return
   */
  override def getControllerInstance[A](controllerClass: Class[A]): A = {
    val instance = controllerClass.getConstructors.find { c =>
      val params = c.getParameterTypes
      params.length == 1 && params(0) == classOf[WistRuntimeEnvironment[BasicUser]]
    }.map {
      _.asInstanceOf[Constructor[A]].newInstance(MyRuntimeEnvironment)
    }
    instance.getOrElse(super.getControllerInstance(controllerClass))
  }
}
