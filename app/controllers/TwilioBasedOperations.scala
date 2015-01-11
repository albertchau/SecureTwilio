package controllers

import java.util.UUID

import authentication.WistSecureSocial
import models.TableQueries.twilioTokens
import models.{RegisterNewInfo, TwilioToken}
import org.joda.time.DateTime
import play.api.Play
import play.api.db.slick.DB
import play.api.mvc._
import scala.slick.driver.MySQLDriver.simple._

import scala.concurrent.Future

abstract class TwilioBasedOperations[U] extends WistSecureSocial[U] {
  val Success = "success"
  val Error = "error"
  val Email = "email"
  val FullName = "fullName"
  val PhoneNumber = "phoneNumber"
  val TwilioCode = "twilioCode"
  val TokenDurationKey = "securesocial.userpass.tokenDuration"
  val DefaultDuration = 60
  val TokenDuration = Play.current.configuration.getInt(TokenDurationKey).getOrElse(DefaultDuration)

  def createAndSaveToken(info: RegisterNewInfo): TwilioToken = {
    val now = DateTime.now
    val twilioToken = TwilioToken(
      UUID.randomUUID().toString, "temp", info, now, now.plusMinutes(TokenDuration)
    )

    import play.api.Play.current
    DB withSession { implicit session =>
        twilioTokens += twilioToken
    }
    twilioToken
  }

  protected def executeForToken(uuid: String,
                                f: TwilioToken => Future[Result])(implicit request: RequestHeader): Future[Result] = {
    import scala.concurrent.ExecutionContext.Implicits.global
    findTwilioToken(uuid).flatMap {
      case Some(t) if !t.isExpired => f(t)
      case _ =>
        Future.successful(Ok("Expired or bad registration code"))
    }
  }

  def findTwilioToken(uuid: String): Future[Option[TwilioToken]] = {
    Future.successful {
      import play.api.Play.current
      DB withSession { implicit session =>
        twilioTokens.filter(_.uuid === uuid).firstOption
      }
    }
  }

  def deleteToken(uuid: String): Future[Option[TwilioToken]] =  Future successful {
    import play.api.Play.current
    DB withSession { implicit session =>
      twilioTokens.filter(_.uuid === uuid).firstOption.map(tt => {
        twilioTokens.filter(_.uuid === uuid).delete
        tt
      })
    }
  }

  /**
   * The result sent after the start page is handled
   *
   * @param request the current request
   * @return the action result
   */
  protected def handleStartResult()(implicit request: RequestHeader): Result = Redirect(env.routes.loginPageUrl)

  /**
   * The result sent after the operation has been completed by the user
   *
   * @param request the current request
   * @return the action result
   */
  protected def confirmationResult()(implicit request: RequestHeader): Result = Redirect(env.routes.loginPageUrl)
}



