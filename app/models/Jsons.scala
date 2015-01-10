package models

import org.joda.time.DateTime

/**
 * Created by achau on 1/3/15.
 *
 */

case class TokenResponse(token: String, expiresOn: DateTime)