import io.gatling.core.Predef._
import io.gatling.http.Predef._

import scala.concurrent.duration._

import java.util.Date._
import java.text.SimpleDateFormat._

class uc6 extends Simulation {

  val baseUrl                = System.getProperty("baseUrl")
  val sessionToken           = System.getProperty("sessionToken")
  val messagesFile           = System.getProperty("messagesFile")

  val model                       = System.getProperty("model")
  val steps                       = Integer.getInteger("steps", 5)
  val maxHostConcurrentUsers      = Integer.getInteger("maxHostConcurrentUsers", 10)
  val maxHostIncrementUsersPerSec = Integer.getInteger("maxHostIncrementUsersPerSec", 10)

  val messagesFeeders = csv(s"$messagesFile").eager.random

  val httpConf = http
    .baseUrl(baseUrl)
    .authorizationHeader(s"Bearer $sessionToken")
    .header("Content-Type", "application/json")

  object GetMessage {

    val getMessage = 
      exec(
        http("GET /api/v1/messages/{id_message}")
          .get("/api/v1/messages/${messageId}")
      )
      .pause(1000 milliseconds)
      
  }
  
  val users = scenario("Users").feed(messagesFeeders).exec(GetMessage.getMessage)

  model match {

    case "open"  => 
    // Open Model
    val incrementUsers = maxHostIncrementUsersPerSec.toFloat/steps
    setUp(
      users.inject(
        incrementUsersPerSec(incrementUsers.toInt) // Double
          .times(steps.toInt) // repetition
          .eachLevelLasting(30 seconds) // time between next increment
          .separatedByRampsLasting(10 seconds)
          .startingFrom(incrementUsers.toInt) // Double
      ).protocols(httpConf)
    )

    case "closed"  => 
    // Closed Model
    val incrementUsers = maxHostConcurrentUsers.toFloat/steps
    setUp(
      users.inject(
        incrementConcurrentUsers(incrementUsers.toInt) // Double
          .times(steps.toInt) // repetition
          .eachLevelLasting(30 seconds) // time between next increment
          .separatedByRampsLasting(10 seconds)
          .startingFrom(incrementUsers.toInt) // Double
      ).protocols(httpConf)
    )

  }

}
