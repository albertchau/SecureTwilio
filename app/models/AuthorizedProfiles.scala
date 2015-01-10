package models

import models.TableQueries.{passwords, oauth2s, oauth1s}
import securesocial.core.{PasswordInfo, OAuth2Info, OAuth1Info, AuthenticationMethod}
import scala.slick.driver.MySQLDriver.simple._

case class AuthorizedProfile(providerId: String,
                             email: String,
                             fullName: Option[String],
                             phoneNumber: Option[String],
                             authMethod: AuthenticationMethod,
                             avatarUrl: Option[String],
                             oAuth1Info: Option[OAuth1Info] = None,
                             oAuth2Info: Option[OAuth2Info] = None,
                             passwordInfo: Option[PasswordInfo] = None)

case class Profile(id: Option[Long] = None,
                   providerId: String,
                   email: String,
                   fullName: Option[String],
                   phoneNumber: Option[String],
                   authMethod: String,
                   avatarUrl: Option[String],
                   oAuth1Id: Option[Long] = None,
                   oAuth2Id: Option[Long] = None,
                   passwordId: Option[Long]) {
  def authorizedProfile(implicit session: Session): AuthorizedProfile = {
    AuthorizedProfile(
      providerId,
      email,
      fullName,
      phoneNumber,
      authMethod match {
        case "oauth1" => AuthenticationMethod.OAuth1
        case "oauth2" => AuthenticationMethod.OAuth2
        case "openId" => AuthenticationMethod.OpenId
        case "userPassword" => AuthenticationMethod.UserPassword
      },
      avatarUrl,
      oauth1s.filter(_.id === oAuth1Id).firstOption.map(o1 => o1.oAuth1Info),
      oauth2s.filter(_.id === oAuth2Id).firstOption.map(o2 => o2.oAuth2Info),
      passwords.filter(_.id === passwordId).firstOption.map(p => p.passwordInfo)
    )
  }
}


class Profiles(tag: Tag) extends Table[Profile](tag, "profile") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)

  def providerId = column[String]("provider_id")

  def email = column[String]("email")

  def fullName = column[Option[String]]("full_name")

  def phoneNumber = column[Option[String]]("phone_number")

  def authMethod = column[String]("auth_method")

  def avatarUrl = column[Option[String]]("avatar_url")

  def oAuth1Id = column[Option[Long]]("oauth1_id")

  def oAuth2Id = column[Option[Long]]("oauth2_id")

  def passwordId = column[Option[Long]]("password_id")

  def * = (
    id.?,
    providerId,
    email,
    fullName,
    phoneNumber,
    authMethod,
    avatarUrl,
    oAuth1Id,
    oAuth2Id,
    passwordId
    ) <>(Profile.tupled, Profile.unapply)

  def idk = index("profile_idx", (providerId, email))
}

