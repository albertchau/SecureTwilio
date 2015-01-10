package models

import scala.slick.lifted.TableQuery

/**
 * @author Joseph Dessens
 * @since 9/6/14
 */
object TableQueries {
  object twilioTokens extends TableQuery(new TwilioTokens(_))
  object userAuthenticators extends TableQuery(new UserAuthenticators(_))
  object users extends TableQuery(new Users(_))
  object oauth1s extends TableQuery(new OAuth1s(_))
  object oauth2s extends TableQuery(new OAuth2s(_))
  object passwords extends TableQuery(new Passwords(_))
  object profiles extends TableQuery(new Profiles(_))
}