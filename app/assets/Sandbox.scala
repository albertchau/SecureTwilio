package assets

import com.mysql.jdbc.jdbc2.optional.MysqlDataSource
import scala.slick.driver.MySQLDriver.simple._

object Sandbox {
  def sesion = {
    val ds = new MysqlDataSource
    ds.setUrl("jdbc:mysql://localhost/test")
    ds.setUser("root")
    ds.setPassword("admin")
    ds.setDatabaseName("test")
    Database.forDataSource(ds).createSession()
  }
}