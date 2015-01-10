package authentication.providers

import authentication.{WistOAuth2Provider, WistOAuth2Client}
import models.AuthorizedProfile
import securesocial.core.services.{RoutesService, CacheService}
import securesocial.core.{OAuth2Info, AuthenticationException}

import scala.concurrent.Future

/**
 * An Instagram provider
 *
 */
class WistInstagramProvider(routesService: RoutesService,
                        cacheService: CacheService,
                        client: WistOAuth2Client)
  extends WistOAuth2Provider(routesService, client, cacheService) {
  val GetAuthenticatedUser = "https://api.instagram.com/v1/users/self?access_token=%s"
  val AccessToken = "access_token"
  val TokenType = "token_type"
  val Data = "data"
  val Username = "username"
  val FullName = "full_name"
  val ProfilePic = "profile_picture"
  val Id = "id"

  override val id = WistInstagramProvider.Instagram

  def fillProfile(info: OAuth2Info): Future[AuthorizedProfile] = {
    import scala.concurrent.ExecutionContext.Implicits.global
    client.retrieveProfile(GetAuthenticatedUser.format(info.accessToken)).map { me =>
      (me \ "response" \ "user").asOpt[String] match {
        case Some(msg) => {
          logger.error(s"[securesocial] error retrieving profile information from Instagram. Message = $msg")
          throw new AuthenticationException()
        }
        case _ =>
          val userId = (me \ Data \ Id).as[String]
          val fullName = (me \ Data \ FullName).asOpt[String]
          val avatarUrl = (me \ Data \ ProfilePic).asOpt[String]
          AuthorizedProfile(id, userId, fullName, None, authMethod, avatarUrl, oAuth2Info = Some(info))
      }
    } recover {
      case e: AuthenticationException => throw e
      case e: Exception =>
        logger.error("[securesocial] error retrieving profile information from Instagram", e)
        throw new AuthenticationException()
    }
  }
}

object WistInstagramProvider {
  val Instagram = "instagram"
}
