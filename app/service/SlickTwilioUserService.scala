/**
 * Copyright 2012 Jorge Aliss (jaliss at gmail dot com) - twitter: @jaliss
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
package service

import authentication.services.WistUserService
import models.TableQueries._
import models._
import play.api.Logger
import play.api.Play.current
import play.api.db.slick.DB
import securesocial.core._
import securesocial.core.providers.{UsernamePasswordProvider => UserPass}
import securesocial.core.services.{SaveMode}

import scala.concurrent.Future
import scala.slick.driver.MySQLDriver.simple._

class SlickTwilioUserService extends WistUserService[BasicUser] {

  implicit def BasicProfile2AuthProfile(value: BasicProfile): AuthorizedProfile = AuthorizedProfile(
    value.providerId,
    value.email.getOrElse(""),
    value.fullName,
    Some(value.userId),
    value.authMethod,
    value.avatarUrl,
    value.oAuth1Info,
    value.oAuth2Info,
    value.passwordInfo)

  implicit def AuthProfile2BasicProfile(value: AuthorizedProfile): BasicProfile = BasicProfile(
    value.providerId,
    value.phoneNumber.getOrElse(""),
    None,
    None,
    value.fullName,
    Some(value.email.toLowerCase),
    None,
    AuthenticationMethod.UserPassword,
    None,
    passwordInfo = value.passwordInfo
  )

  val logger = Logger("application.controllers.InMemoryTextService")

  override def deleteToken(uuid: String): Future[Option[TwilioToken]] = ???

  override def findToken(token: String): Future[Option[TwilioToken]] = ???

  override def deleteExpiredTokens(): Unit = ???

  override def saveToken(token: TwilioToken): Future[TwilioToken] = ???

  override def find(providerId: String, email: String): Future[Option[BasicProfile]] = {
    Future.successful(
      DB withSession { implicit session =>
        profiles
          .filter(sp => sp.providerId === providerId && sp.email === email)
          .firstOption
          .map(sp => sp.authorizedProfile)
      }
        flatMap (f => Some(AuthProfile2BasicProfile(f)))
    )
  }

  override def findByEmailAndProvider(email: String, providerId: String): Future[Option[BasicProfile]] = {
    Future.successful(
      DB withSession { implicit session =>
        profiles
          .filter(sp => sp.email === email && sp.providerId === providerId)
          .firstOption
          .map(sp => sp.authorizedProfile)
      }
        flatMap (f => Some(AuthProfile2BasicProfile(f)))
    )
  }

  def link(current: BasicUser, to: BasicProfile): Future[BasicUser] = Future successful {
    val converted: AuthorizedProfile = to
    if (current.identities.exists(i => i.providerId == converted.providerId && i.userId == converted.userId)) {
      current
    } else {
      current.copy(identities = converted :: current.identities)
    }
  }

  override def passwordInfoFor(user: BasicUser): Future[Option[PasswordInfo]] = {
    Future successful {
      DB withSession { implicit session =>
        profiles
          .filter(p => p.providerId === UserPass.UsernamePassword && p.email === user.main.email)
          .firstOption match {
          case Some(profile) =>
            passwords.filter(_.id === profile.passwordId).firstOption.map(p => p.passwordInfo)
          case None => None
        }
      }
    }
  }

  def save(bp: BasicProfile, mode: SaveMode): Future[BasicUser] = Future successful {
    logger.debug(f"mode: $mode")

    val profile: AuthorizedProfile = bp
    mode match {
      case SaveMode.SignUp =>
        DB withTransaction { implicit session =>
          val oAuth1InfoId = profile.oAuth1Info.map(o1 =>
            (oauth1s returning oauth1s.map(_.id)) += OAuth1(None, o1.token, o1.secret)
          )
          val oAuth2InfoId = profile.oAuth2Info.map(o2 =>
            (oauth2s returning oauth2s.map(_.id)) += OAuth2(None, o2.accessToken, o2.tokenType, o2.expiresIn, o2.refreshToken)
          )
          val passwordInfoId = profile.passwordInfo.map(p => {
            (passwords returning passwords.map(_.id)) += Password(None, p.hasher, p.password, p.salt)
          })
          val profileId = (profiles returning profiles.map(_.id)) += Profile(
            None,
            profile.providerId,
            profile.email,
            profile.fullName,
            profile.phoneNumber,
            profile.authMethod.method,
            profile.avatarUrl,
            oAuth1InfoId,
            oAuth2InfoId,
            passwordInfoId
          )

          users += User(profile.email, profileId)

          users.filter(_.email === profile.email).first.basicUser
        }
      case SaveMode.PasswordChange =>
        DB withSession { implicit session =>
          val passwordId = profiles
            .filter(p => p.email === profile.email && p.providerId === UserPass.UsernamePassword)
            .first.passwordId

          val passwordInfo = profile.passwordInfo.get

          val password = Password(
            passwordId,
            passwordInfo.hasher,
            passwordInfo.password,
            passwordInfo.salt
          )

          passwords.filter(_.id === passwordId).update(password)

          logger.debug("PasswordChange")

          users.filter(_.email === profile.email).first.basicUser
        }
      case _ =>
        DB withSession { implicit session =>
          users.filter(_.email === profile.email).first.basicUser
        }
    }
  }

  override def updatePasswordInfo(user: BasicUser, info: PasswordInfo): Future[Option[BasicProfile]] = Future successful {
    //    Future.successful {
    //      for (
    //        found <- users.values.find(_ == user);
    //        identityWithPasswordInfo <- found.identities.find(_.providerId == WistUserPassProvider.UsernamePassword)
    //      ) yield {
    //        val idx = found.identities.indexOf(identityWithPasswordInfo)
    //        val updated = identityWithPasswordInfo.copy(passwordInfo = info)
    //        val updatedIdentities = found.identities.patch(idx, Seq(updated), 1)
    //        found.copy(identities = updatedIdentities)
    //        updated
    //      }
    //    }
    logger.debug("updatePasswordInfo")

    DB withSession { implicit session =>
      val profile = profiles
        .filter(p => p.email === user.main.email && p.providerId === UserPass.UsernamePassword)
        .firstOption

      profile match {
        case Some(p) =>
          passwords.update(Password(p.passwordId, info.hasher, info.password, info.salt))
          profile.map(p => p.authorizedProfile).flatMap(f => Some(AuthProfile2BasicProfile(f)))
        case None => None
      }
    }
  }
}