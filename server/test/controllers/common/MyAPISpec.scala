package controllers.common

import com.alexitc.playsonify.test.PlayAPISpec
import com.xsn.explorer.modules.{
  CurrencySynchronizerModule,
  DatabaseMigrationsModule,
  MasternodeSynchronizerModule,
  MerchantnodeSynchronizerModule,
  NodeStatsSynchronizerModule,
  PollerSynchronizerModule
}
import org.slf4j.LoggerFactory
import play.api.db.{DBApi, Database, Databases}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.{Configuration, Environment, Mode}

import scala.concurrent.Future

trait MyAPISpec extends PlayAPISpec {

  protected val logger = LoggerFactory.getLogger(this.getClass)

  override protected def log[T](
      request: FakeRequest[T],
      response: Future[Result]
  ): Unit = {
    logger.info(
      s"> REQUEST, $request; < RESPONSE, status = ${status(response)}, body = ${contentAsString(response)}"
    )
  }

  /**
   * A dummy [[Database]] and [[DBApi]] just to allow a play application
   * to start without connecting to a real database from application.conf.
   */
  private val dummyDB = Databases.inMemory()

  private val dummyDBApi = new DBApi {
    override def databases(): Seq[Database] = List(dummyDB)
    override def database(name: String): Database = dummyDB
    override def shutdown(): Unit = dummyDB.shutdown()
  }

  /**
   * Loads configuration disabling evolutions on default database.
   *
   * This allows to not write a custom application.conf for testing
   * and ensure play evolutions are disabled.
   */
  private def loadConfigWithoutEvolutions(env: Environment): Configuration = {
    val map = Map("play.evolutions.db.default.enabled" -> false)

    Configuration.from(map).withFallback(Configuration.load(env))
  }

  override val guiceApplicationBuilder: GuiceApplicationBuilder =
    GuiceApplicationBuilder(loadConfiguration = loadConfigWithoutEvolutions)
      .in(Mode.Test)
      .disable(classOf[PollerSynchronizerModule])
      .disable(classOf[DatabaseMigrationsModule])
      .disable(classOf[CurrencySynchronizerModule])
      .disable(classOf[MasternodeSynchronizerModule])
      .disable(classOf[MerchantnodeSynchronizerModule])
      .disable(classOf[NodeStatsSynchronizerModule])
      .overrides(bind[Database].to(dummyDB))
      .overrides(bind[DBApi].to(dummyDBApi))
}
