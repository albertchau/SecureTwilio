package models

import models.TableQueries.profiles

import scala.slick.driver.MySQLDriver.simple._
/**
 * @author Joseph Dessens
 * @since 2014-09-01
 */
case class User(email: String, mainId: Long) {
  def basicUser(implicit session: Session): BasicUser = {
    val main = profiles.filter(_.id === mainId).first
    val identities = profiles.filter(p => p.email === email && p.id =!= mainId).list

    BasicUser(main.authorizedProfile, identities.map(i => i.authorizedProfile))
  }
}

class Users(tag: Tag) extends Table[User](tag, "user") {
  def email = column[String]("email", O.PrimaryKey)
  def mainId = column[Long]("main_id")

  def * = (email, mainId) <> (User.tupled, User.unapply)
}