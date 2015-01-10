package models

case class BasicUser(main: AuthorizedProfile, identities: List[AuthorizedProfile])

