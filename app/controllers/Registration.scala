/**
 * Copyright 2012-2014 Jorge Aliss (jaliss at gmail dot com) - twitter: @jaliss
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
package controllers

import _root_.java.util.UUID

import models._
import org.joda.time.DateTime
import play.api.data.Forms._
import play.api.data._
import play.api.i18n.Messages
import play.api.libs.json.{JsString, JsValue, Writes, Json}
import play.api.mvc.Action
import securesocial.controllers.{RegistrationInfo, MailTokenBasedOperations}
import securesocial.core._
import securesocial.core.authenticator.{HttpHeaderAuthenticator, CookieAuthenticator}
import securesocial.core.providers.{MailToken, UsernamePasswordProvider}
import securesocial.core.providers.utils._
import securesocial.core.services.SaveMode
import service.{ SlickTwilioUserService}

import scala.concurrent.{Await, ExecutionContext, Future}

/**
 * A default Registration controller that uses the BasicProfile as the user type
 *
 * @param env the environment
 */
class Registration(override implicit val env: RuntimeEnvironment[AuthorizedProfile]) extends BaseRegistration[AuthorizedProfile]

/**
 * A trait that provides the means to handle user registration
 *
 * @tparam U the user type
 */
trait BaseRegistration[U] extends TwilioBasedOperations[U] {

  import securesocial.controllers.BaseRegistration._

  private val logger = play.api.Logger("securesocial.controllers.Registration")

  val providerId = UsernamePasswordProvider.UsernamePassword

  val startForm = Form[RegisterNewInfo](
    mapping(
      Email -> nonEmptyText.verifying("Email Already Taken", email => {
        // todo: see if there's a way to avoid waiting here :-\
        import scala.concurrent.duration._
        Await.result(env.userService.find(providerId, email), 20.seconds).isEmpty
      }),
      FullName -> nonEmptyText,
      PhoneNumber -> nonEmptyText,
      Password ->
        tuple(
          Password1 -> nonEmptyText.verifying(PasswordValidator.constraint),
          Password2 -> nonEmptyText
        ).verifying(Messages(PasswordsDoNotMatch), passwords => passwords._1 == passwords._2)
    ) // binding
      ((email, fullName, phoneNumber, password) => RegisterNewInfo(fullName, phoneNumber, email, password._1)) // unbinding
      (info => Some(info.fullName, info.phoneNumber, info.email, ("", "")))
  )

  val formForTextVerification = Form(
    TwilioCode -> nonEmptyText
  )

  val form = startForm

  /**
   * Starts the sign up process
   */
  def startSignUp = Action {
    implicit request =>
      if (SecureSocial.enableRefererAsOriginalUrl) {
        SecureSocial.withRefererAsOriginalUrl(Ok(views.html.startSignUp(startForm)))
      } else {
        Ok(views.html.startSignUp(startForm))
      }
  }

  def handleStartSignUp = Action.async {
    implicit request =>
      startForm.bindFromRequest.fold(
        errors => {
          Future.successful(BadRequest(views.html.startSignUp(errors)))
        },
        newInfo => {
          val email = newInfo.email.toLowerCase
          // check if there is already an account for this email address
          import scala.concurrent.ExecutionContext.Implicits.global
          env.userService.findByEmailAndProvider(email, UsernamePasswordProvider.UsernamePassword).map {
            case Some(user) =>
              // user signed up already, send an email offering to login/recover password
              val content: String = "user " + user.email + " is already signed up"
              logger.info(content)
              Ok(content)
            case None =>
              val token = createAndSaveToken(newInfo)
              logger.info("TwilioToken for " + email + " is " + securesocial.controllers.routes.Registration.signUp(token.uuid).absoluteURL(IdentityProvider.sslEnabled))
              Redirect(routes.Registration.signUp(token.uuid))
          }
        }
      )
  }

  /**
   * Renders the sign up page
   * @return
   */
  def signUp(token: String) = Action.async {
    implicit request =>
      logger.debug("[securesocial] trying sign up with token %s".format(token))
      executeForToken(token, {
        _ =>
          Future.successful(Ok(views.html.signUp(formForTextVerification, token)))
      })
  }

  implicit val jodaDateWrites: Writes[org.joda.time.DateTime] = new Writes[org.joda.time.DateTime] {
    def writes(d: org.joda.time.DateTime): JsValue = JsString(d.toString)
  }
  implicit val HeaderTokenWrites = Json.writes[TokenResponse]
  /**
   * Handles posts from the sign up page
   */
  def handleSignUp(token: String) = Action.async {
    implicit request =>
      import scala.concurrent.ExecutionContext.Implicits.global
      executeForToken(token, {
        token =>
          formForTextVerification.bindFromRequest.fold(
            errors => {
              logger.debug("[securesocial] errors " + errors)
              Future.successful(BadRequest(views.html.signUp(errors, token.uuid)))
            },
            textCode => {
              if (textCode != token.twilioCode) {
                Future.successful(BadRequest("TwilioCode did not match"))
              } else {
                val info = token.info
                val newUser = BasicProfile(
                  providerId,
                  info.phoneNumber,
                  None,
                  None,
                  Some(info.fullName),
                  Some(info.email.toLowerCase),
                  None,
                  AuthenticationMethod.UserPassword,
                  None,
                  passwordInfo = Some(env.currentHasher.hash(info.password))
                )

                val withAvatar = env.avatarService.map {
                  _.urlFor(info.email.toLowerCase).map { url =>
                    if (url != newUser.avatarUrl) newUser.copy(avatarUrl = url) else newUser
                  }
                }.getOrElse(Future.successful(newUser))

                val result = for (
                  toSave <- withAvatar;
                  saved <- env.userService.save(toSave, SaveMode.SignUp);
                  deleted <- deleteToken(token.uuid)
                ) yield {
                  Events.fire(new SignUpEvent(saved)).getOrElse(request.session)
                  env.authenticatorService.find(HttpHeaderAuthenticator.Id).map {
                    _.fromUser(saved).flatMap { authenticator =>
                      val token = TokenResponse(authenticator.id, authenticator.expirationDate)
                      Future.successful(Ok(Json.toJson(token)))
                    }
                  } getOrElse {
                    logger.error(s"[securesocial] There isn't CookieAuthenticator registered in the RuntimeEnvironment")
                    Future.successful(confirmationResult().flashing(Error -> Messages("There was an error signing you up")))
                  }
                }
                result.flatMap(f => f)
              }
            })
      })
  }

}

object BaseRegistration {
  val UserNameAlreadyTaken = "securesocial.signup.userNameAlreadyTaken"
  val ThankYouCheckEmail = "securesocial.signup.thankYouCheckEmail"
  val InvalidLink = "securesocial.signup.invalidLink"
  val SignUpDone = "securesocial.signup.signUpDone"
  val Password = "password"
  val Password1 = "password1"
  val Password2 = "password2"
  val PasswordsDoNotMatch = "securesocial.signup.passwordsDoNotMatch"
}
