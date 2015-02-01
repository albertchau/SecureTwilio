package authentication.providers

import WistAuthenticationResult.{NavigationFlow, Authenticated}
import authentication.WistApiSupport
import authentication.services.WistUserService
import authentication.view_controllers.WistViewTemplates
import play.api.Play._
import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc._
import securesocial.core.{AuthenticationMethod}
import securesocial.core.providers.utils.PasswordHasher
import securesocial.core.services.{AvatarService}

import scala.concurrent.Future


/**
 * A username password provider
 */
class WistUserPassProvider[U](userService: WistUserService[U],
                                  avatarService: Option[AvatarService],
                                  viewTemplates: WistViewTemplates,
                                  passwordHashers: Map[String, PasswordHasher])
  extends WistIdentityProvider with WistApiSupport {

  override val id = WistUserPassProvider.UsernamePassword

  def authMethod = AuthenticationMethod.UserPassword

  val InvalidCredentials = "securesocial.login.invalidCredentials"

  def authenticateForApi(implicit request: Request[AnyContent]): Future[WistAuthenticationResult] = {
    doAuthentication(apiMode = true)
  }

  def authenticate()(implicit request: Request[AnyContent]): Future[WistAuthenticationResult] = {
    doAuthentication()
  }

  private def doAuthentication[A](apiMode: Boolean = false)(implicit request: Request[A]): Future[WistAuthenticationResult] = {
    import scala.concurrent.ExecutionContext.Implicits.global
    val form = WistUserPassProvider.loginForm.bindFromRequest()
    form.fold(
      errors => Future.successful {
        if (apiMode)
          WistAuthenticationResult.Failed("Invalid credentials")
        else
          WistAuthenticationResult.NavigationFlow(badRequest(errors)(request))
      },
      credentials => {
        val userId = credentials._1.toLowerCase
        userService.find(id, userId).flatMap { maybeUser =>
          val loggedIn = for (
            user <- maybeUser;
            pinfo <- user.passwordInfo;
            hasher <- passwordHashers.get(pinfo.hasher) if hasher.matches(pinfo, credentials._2)
          ) yield {
            user
          }

          val authenticatedAndUpdated = for (
            u <- loggedIn;
            service <- avatarService
          ) yield {
            service.urlFor(u.email).map {
              case avatar if avatar != u.avatarUrl => u.copy(avatarUrl = avatar)
              case _ => u
            } map {
              Authenticated
            }
          }

          authenticatedAndUpdated.getOrElse {
            Future.successful {
              if (apiMode)
                WistAuthenticationResult.Failed("Invalid credentials")
              else
                NavigationFlow(badRequest(WistUserPassProvider.loginForm, Some(InvalidCredentials)))
            }
          }
        }
      })
  }

  private def badRequest[A](f: Form[(String, String)], msg: Option[String] = None)(implicit request: Request[A]): Result = {
    Results.BadRequest(viewTemplates.getLoginPage(f, msg))
  }
}

object WistUserPassProvider {
  val UsernamePassword = "userpass"
  private val Key = "securesocial.userpass.withUserNameSupport"
  private val SendWelcomeEmailKey = "securesocial.userpass.sendWelcomeEmail"
  private val Hasher = "securesocial.userpass.hasher"
  private val EnableTokenJob = "securesocial.userpass.enableTokenJob"
  private val SignupSkipLogin = "securesocial.userpass.signupSkipLogin"

  val loginForm = Form(
    tuple(
      "username" -> nonEmptyText,
      "password" -> nonEmptyText
    )
  )

  lazy val withUserNameSupport = current.configuration.getBoolean(Key).getOrElse(false)
  lazy val sendWelcomeEmail = current.configuration.getBoolean(SendWelcomeEmailKey).getOrElse(true)
  lazy val hasher = current.configuration.getString(Hasher).getOrElse(PasswordHasher.id)
  lazy val enableTokenJob = current.configuration.getBoolean(EnableTokenJob).getOrElse(true)
  lazy val signupSkipLogin = current.configuration.getBoolean(SignupSkipLogin).getOrElse(false)
}