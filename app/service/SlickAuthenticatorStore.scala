package service


import com.github.tototoshi.slick.JdbcJodaSupport._
import org.joda.time.DateTime
import play.api.Logger
import play.api.db.slick.DB
import securesocial.core.authenticator.{Authenticator, CookieAuthenticator, HttpHeaderAuthenticator}
import models.{UserAuthenticator, TableQueries, BasicUser}
import TableQueries.{users, userAuthenticators}

import scala.concurrent.Future
import scala.reflect.ClassTag
import scala.slick.driver.MySQLDriver.simple._
import play.api.Play.current


/**
 * @author Joseph Dessens
 * @since 2014-08-25
 */
class SlickAuthenticatorStore[A <: Authenticator[BasicUser]] extends securesocial.core.authenticator.AuthenticatorStore[A] {
  val logger: Logger = Logger(this.getClass)
  /**
   * Retrieves an Authenticator from the backing store
   *
   * @param id the authenticator id
   * @param ct the class tag for the Authenticator type
   * @return an optional future Authenticator
   */
  override def find(id: String)(implicit ct: ClassTag[A]): Future[Option[A]] = Future successful {
    DB withSession { implicit session =>
      userAuthenticators.filter(_.id === id).firstOption match {
        case Some(userAuthenticator) =>
          users.filter(_.email === userAuthenticator.email).firstOption match {
            case Some(sbu) =>
              val basicUser = sbu.basicUser
              ct.runtimeClass.getSimpleName match {
                case "CookieAuthenticator" =>
                  Option(
                    CookieAuthenticator(
                      userAuthenticator.id,
                      basicUser,
                      userAuthenticator.expirationDate,
                      userAuthenticator.lastUsed,
                      userAuthenticator.creationDate,
                      this.asInstanceOf[SlickAuthenticatorStore[CookieAuthenticator[BasicUser]]]
                    ).asInstanceOf[A]
                  )
                case "HttpHeaderAuthenticator" =>
                  Option(
                    HttpHeaderAuthenticator(
                      userAuthenticator.id,
                      basicUser,
                      userAuthenticator.expirationDate,
                      userAuthenticator.lastUsed,
                      userAuthenticator.creationDate,
                      this.asInstanceOf[SlickAuthenticatorStore[HttpHeaderAuthenticator[BasicUser]]]
                    ).asInstanceOf[A]
                  )
                case _ => None
              }
            case None => None
          }
        case None => None
      }
    }
  }

  /**
   * Deletes an Authenticator from the backing store
   *
   * @param id the authenticator id
   * @return a future of Unit
   */
  override def delete(id: String): Future[Unit] = Future successful {
    DB withSession { implicit session =>
      userAuthenticators.filter(ua => ua.id === id).delete
      ()
    }
  }

  /**
   * Saves/updates an authenticator in the backing store
   *
   * @param authenticator the instance to save
   * @param timeoutInSeconds the timeout. after this time has passed the backing store needs to remove the entry.
   * @return the saved authenticator
   */
  override def save(authenticator: A, timeoutInSeconds: Int): Future[A] = Future successful {
    val userAuthenticator: UserAuthenticator = UserAuthenticator(
      authenticator.id,
      authenticator.user.main.email,
      authenticator.expirationDate,
      authenticator.lastUsed,
      authenticator.creationDate
    )

    DB withSession { implicit session =>
      userAuthenticators.filter(_.id === authenticator.id).firstOption match {
        case Some(ua) => userAuthenticators.update(userAuthenticator)
        case None => userAuthenticators += userAuthenticator
      }
    }
    authenticator
  }
}