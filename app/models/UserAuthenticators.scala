package models
import com.github.tototoshi.slick.JdbcJodaSupport._
import org.joda.time.DateTime

import scala.slick.ast.ColumnOption.DBType
import scala.slick.driver.MySQLDriver.simple._

/**
 * @author Joseph Dessens
 * @since 2014-09-01
 */
case class UserAuthenticator(id: String, email: String, expirationDate: DateTime, lastUsed: DateTime, creationDate: DateTime)

class UserAuthenticators(tag: Tag) extends Table[UserAuthenticator](tag, "authenticator") {
  def id = column[String]("id", DBType("varchar(200)"), O.PrimaryKey)
  def email = column[String]("email")
  def expirationDate = column[DateTime]("expiration_date")
  def lastUsed = column[DateTime]("last_used")
  def creationDate = column[DateTime]("creation_date")

  def * = (id, email, expirationDate, lastUsed, creationDate) <> (UserAuthenticator.tupled, UserAuthenticator.unapply)
}