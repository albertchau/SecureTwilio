package models

import _root_.java.sql.Date
import com.github.tototoshi.slick.JdbcJodaSupport._

import scala.slick.driver.MySQLDriver.simple._
import scala.slick.lifted.ProvenShape
import org.joda.time.DateTime

/**
 * @author Joseph Dessens
 * @since 2014-08-07
 */

case class TwilioToken(uuid: String, twilioCode: String, info: RegisterNewInfo, creationTime: DateTime, expirationTime: DateTime) {
  def isExpired = expirationTime.isBeforeNow
}


class TwilioTokens(tag: Tag) extends Table[TwilioToken](tag, "twilio_token") {

  implicit def tuple2RegisterNewInfo(tuple: (String, String, String, String)): RegisterNewInfo = tuple match {
    case (fullName: String, phoneNumber: String, email: String, password: String) => RegisterNewInfo(fullName, phoneNumber, email, password)
    case _ => RegisterNewInfo("INVALID", "INVALID", "INVALID", "INVALID")
  }

  def uuid = column[String]("uuid", O.PrimaryKey)

  def twilioCode = column[String]("twilio_code")

  //RegisterNewInfo
  def fullName = column[String]("full_name")

  def phoneNumber = column[String]("phone_number")

  def email = column[String]("email")

  def password = column[String]("password")

  def creationTime = column[DateTime]("creation_time")

  def expirationTime = column[DateTime]("expiration_time")

  def * : ProvenShape[TwilioToken] = {
    val shapedValue = (uuid,
      twilioCode,
      fullName,
      phoneNumber,
      email,
      password,
      creationTime,
      expirationTime).shaped

    shapedValue.<>({
      tuple =>
        TwilioToken.apply(
          tuple._1,
          tuple._2,
          tuple2RegisterNewInfo(
            tuple._3,
            tuple._4,
            tuple._5,
            tuple._6
          ),
          tuple._7,
          tuple._8
        )
    }, {
      (t: TwilioToken) =>
        Some {
          (
            t.uuid,
            t.twilioCode,
            t.info.fullName,
            t.info.phoneNumber,
            t.info.email,
            t.info.password,
            t.creationTime,
            t.expirationTime
            )
        }
    })
  }
}