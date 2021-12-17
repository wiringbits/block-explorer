package com.xsn.explorer.data.common

import com.spotify.docker.client.DefaultDockerClient
import com.whisk.docker.DockerFactory
import com.whisk.docker.impl.spotify.SpotifyDockerFactory
import com.whisk.docker.scalatest.DockerTestKit
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.must.Matchers
import org.scalatest.time.{Second, Seconds, Span}
import org.scalatest.wordspec.AnyWordSpec
import play.api.db.evolutions.Evolutions
import play.api.db.{Database, Databases}

/** Allow us to write integration tests depending in a postgres database.
  *
  * The database is launched in a docker instance using docker-it-scala library.
  *
  * When the database is started, play evolutions are automatically applied, the idea is to let you write tests like
  * this:
  * {{{
  *   class UserPostgresDALSpec extends PostgresDALSpec {
  *     lazy val dal = new UserPostgresDAL(database)
  *     ...
  *   }
  * }}}
  */
trait PostgresDataHandlerSpec
    extends AnyWordSpec
    with Matchers
    with DockerTestKit
    with DockerPostgresService
    with BeforeAndAfterAll {

  import DockerPostgresService._

  implicit val pc = PatienceConfig(Span(20, Seconds), Span(1, Second))

  override implicit val dockerFactory: DockerFactory = new SpotifyDockerFactory(
    DefaultDockerClient.fromEnv().build()
  )

  override def beforeAll(): Unit = {
    super.beforeAll()
    val _ = isContainerReady(postgresContainer).futureValue mustEqual true
  }

  def database: Database = {
    val database = Databases(
      driver = "org.postgresql.Driver",
      url = s"jdbc:postgresql://localhost:$PostgresExposedPort/$DatabaseName",
      name = "default",
      config = Map(
        "username" -> PostgresUsername,
        "password" -> PostgresPassword
      )
    )

    Evolutions.applyEvolutions(database)

    database
  }

  protected def clearDatabase() = {
    database.withConnection { implicit conn =>
      _root_.anorm.SQL("""DELETE FROM tpos_contracts""").execute()
      _root_.anorm.SQL("""DELETE FROM transaction_inputs""").execute()
      _root_.anorm.SQL("""DELETE FROM transaction_outputs""").execute()
      _root_.anorm.SQL("""DELETE FROM address_transaction_details""").execute()
      _root_.anorm.SQL("""DELETE FROM transactions""").execute()
      _root_.anorm.SQL("""DELETE FROM block_address_gcs""").execute()
      _root_.anorm.SQL("""DELETE FROM block_rewards""").execute()
      _root_.anorm.SQL("""DELETE FROM blocks""").execute()
      _root_.anorm.SQL("""DELETE FROM balances""").execute()
      _root_.anorm.SQL("""DELETE FROM hidden_addresses""").execute()
      _root_.anorm.SQL("""DELETE FROM aggregated_amounts""").execute()
      _root_.anorm
        .SQL("""DELETE FROM block_synchronization_progress""")
        .execute()
    }
  }
}
