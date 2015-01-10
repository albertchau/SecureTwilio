package authentication.services

import models.{AuthorizedProfile, TwilioToken}
import securesocial.core.PasswordInfo

import scala.concurrent.Future


trait WistUserService[U] {

  /**
   * Finds a SocialUser that maches the specified id
   *
   * @param providerId the provider id
   * @param userId the user id
   * @return an optional profile
   */
  def find(providerId: String, userId: String): Future[Option[AuthorizedProfile]]

  /**
   * Finds a profile by email and provider
   *
   * @param email - the user email
   * @param providerId - the provider id
   * @return an optional profile
   */
  def findByEmailAndProvider(email: String, providerId: String): Future[Option[AuthorizedProfile]]

  /**
   * Saves a profile.  This method gets called when a user logs in, registers or changes his password.
   * This is your chance to save the user information in your backing store.
   *
   * @param profile the user profile
   * @param mode a mode that tells you why the save method was called
   */
  def save(profile: AuthorizedProfile, mode: SaveMode): Future[U]

  /**
   * Links the current user to another profile
   *
   * @param current The current user instance
   * @param to the profile that needs to be linked to
   */
  def link(current: U, to: AuthorizedProfile): Future[U]

  /**
   * Returns an optional PasswordInfo instance for a given user
   *
   * @param user a user instance
   * @return returns an optional PasswordInfo
   */
  def passwordInfoFor(user: U): Future[Option[PasswordInfo]]

  /**
   * Updates the PasswordInfo for a given user
   *
   * @param user a user instance
   * @param info the password info
   * @return
   */
  def updatePasswordInfo(user: U, info: PasswordInfo): Future[Option[AuthorizedProfile]]

  /**
   * Saves a mail token.  This is needed for users that
   * are creating an account in the system or trying to reset a password
   *
   * Note: If you do not plan to use the UsernamePassword provider just provide en empty
   * implementation
   *
   * @param token The token to save
   */
  def saveToken(token: TwilioToken): Future[TwilioToken]

  /**
   * Finds a token
   *
   * Note: If you do not plan to use the UsernamePassword provider just provide en empty
   * implementation
   *
   * @param token the token id
   * @return
   */
  def findToken(token: String): Future[Option[TwilioToken]]

  /**
   * Deletes a token
   *
   * Note: If you do not plan to use the UsernamePassword provider just provide en empty
   * implementation
   *
   * @param uuid the token id
   */
  def deleteToken(uuid: String): Future[Option[TwilioToken]]

  /**
   * Deletes all expired tokens
   *
   * Note: If you do not plan to use the UsernamePassword provider just provide en empty
   * implementation
   *
   */
  def deleteExpiredTokens()
}

/**
 * Save modes
 */
case class SaveMode(name: String) {
  def is(m: SaveMode): Boolean = this == m
}

object SaveMode {
  val LoggedIn = SaveMode("loggedIn")
  val SignUp = SaveMode("signUp")
  val PasswordChange = SaveMode("passwordChange")
}
