package authentication


import authentication.providers.{WistUserPassProvider, WistInstagramProvider, WistFacebookProvider}
import authentication.services.WistUserService
import authentication.view_controllers.{Mailer, WistMailTemplates, WistViewTemplates}
import models.BasicUser
import securesocial.core.EventListener
import securesocial.core.authenticator.{HttpHeaderAuthenticatorBuilder, CookieAuthenticatorBuilder, IdGenerator}
import securesocial.core.providers.utils.{PasswordValidator, PasswordHasher}
import securesocial.core.services._
import service.{SlickTwilioUserService, SlickAuthenticatorStore}

import scala.collection.immutable.ListMap

/**
 * A runtime environment where the services needed are available
 */
trait WistRuntimeEnvironment[U] {
  val routes: RoutesService

  //not usable
  val viewTemplates: WistViewTemplates
  val mailTemplates: WistMailTemplates

  val mailer: Mailer

  val currentHasher: PasswordHasher
  val passwordHashers: Map[String, PasswordHasher]
  val passwordValidator: PasswordValidator

  val httpService: HttpService
  val cacheService: CacheService
  val avatarService: Option[AvatarService]

  val providers: Map[String, WistIdentityProvider]

  val idGenerator: IdGenerator
  val authenticatorService: AuthenticatorService[U]

  val eventListeners: List[EventListener[U]]

  val userService: WistUserService[U]
}

object WistRuntimeEnvironment {

  /**
   * A default runtime environment.  All built in services are included.
   * You can start your app with with by only adding a userService to handle users.
   */
  abstract class Default[U] extends WistRuntimeEnvironment[U] {
    override lazy val routes: RoutesService = new RoutesService.Default()
    override lazy val userService: SlickTwilioUserService = new SlickTwilioUserService()

    //not usable
    override lazy val viewTemplates: WistViewTemplates = new WistViewTemplates.Default(this)
    override lazy val mailTemplates: WistMailTemplates = new WistMailTemplates.Default(this)
    override lazy val mailer: Mailer = new Mailer.Default(mailTemplates)

    override lazy val currentHasher: PasswordHasher = new PasswordHasher.Default()
    override lazy val passwordHashers: Map[String, PasswordHasher] = Map(currentHasher.id -> currentHasher)
    override lazy val passwordValidator: PasswordValidator = new PasswordValidator.Default()

    override lazy val httpService: HttpService = new HttpService.Default()
    override lazy val cacheService: CacheService = new CacheService.Default()
    override lazy val avatarService: Option[AvatarService] = Some(new AvatarService.Default(httpService))
    override lazy val idGenerator: IdGenerator = new IdGenerator.Default()

    override lazy val authenticatorService: AuthenticatorService[U] = new AuthenticatorService[U](
      new CookieAuthenticatorBuilder[U](new SlickAuthenticatorStore, idGenerator),
      new HttpHeaderAuthenticatorBuilder[U](new SlickAuthenticatorStore, idGenerator)
    )

    override lazy val eventListeners: List[EventListener[U]] = List()

    protected def include(p: WistIdentityProvider) = p.id -> p
    protected def oauth2ClientFor(provider: String) = new WistOAuth2Client.Default(httpService, WistOAuth2Settings.forProvider(provider))

    override lazy val providers = ListMap(
      // oauth 2 client providers
      include(new WistFacebookProvider(routes, cacheService, oauth2ClientFor(WistFacebookProvider.Facebook))),
      include(new WistInstagramProvider(routes, cacheService, oauth2ClientFor(WistInstagramProvider.Instagram))),
      // username password viewTemplates not usable
      include(new WistUserPassProvider[U](userService, avatarService, viewTemplates, passwordHashers))
    )
  }
}
