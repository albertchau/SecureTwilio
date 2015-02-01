package authentication.providers.utils

import authentication.WistRuntimeEnvironment
import play.api.data.validation.{Invalid, Valid, Constraint}

/**
 * A trait to define password validators.
 */
trait WistPasswordValidator {
  /**
   * Validates a password
   *
   * @param password the supplied password
   * @return Right if the password is valid or Left with an error message otherwise
   */
  def validate(password: String): Either[(String, Seq[Any]), Unit]
}

object WistPasswordValidator {
  /**
   * A helper method to create a constraint used in forms
   *
   * @param env a RuntimeEnvironment with the WistPasswordValidator implmentation to use
   * @return Valid if the password is valid or Invalid otherwise
   */
  def constraint(implicit env: WistRuntimeEnvironment[_]) = Constraint[String] { s: String =>
    env.passwordValidator.validate(s) match {
      case Right(_) => Valid
      case Left(error) => Invalid(error._1, error._2: _*)
    }
  }

  /**
   * A default password validator that only checks a minimum length.
   *
   * The minimum length can be configured setting a minimumPasswordLength property for userpass.
   * Defaults to 8 if not specified.
   */
  class Default(requiredLength: Int) extends WistPasswordValidator {
    def this() = this({
      val app = play.api.Play.current
      app.configuration.getInt(Default.PasswordLengthProperty).getOrElse(Default.Length)
    })

    override def validate(password: String): Either[(String, Seq[Any]), Unit] = {
      if (password.length >= requiredLength) {
        Right(())
      } else
        Left((Default.InvalidPasswordMessage, Seq(requiredLength)))
    }
  }

  object Default {
    val Length = 8
    val PasswordLengthProperty = "securesocial.userpass.minimumPasswordLength"
    val InvalidPasswordMessage = "securesocial.signup.invalidPassword"
  }
}