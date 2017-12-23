package cchesser.javaperf.workshop

import io.gatling.core.Predef._
import io.gatling.http.Predef._

import scala.concurrent.duration._

/**
  *
  */
class WorkshopSimulation extends Simulation {

  /**
    * Calls against the "/search" endpoint
    */
  object Search {

    /**
      * Performs a call to "/search?q=${searchTerm}",  additionally the searching user will frantically click on each session to get the abstract
      */
    def search(searchTerm: String) = {
      exec(http(s"/search?q=${searchTerm}")
        .get(s"/search?q=${searchTerm}")
        .check(jsonPath("$.results[*].sessionId").findAll.saveAs("sessions"))
      ).pause(1)
        .foreach("${sessions}", "sessionId") {
          Sessions.sessionId("{sessionId}")
        }
    }

    /**
      * Performs a call to "/search?q=${searchTerm}", retrieving the resultsContext, followed by "/search?q=$[searchTerm}&c=${resultsContext}
      */
    def withContext(searchTerm: String) = {
      exec(http(s"/search?q=${searchTerm}")
        .get(s"/search?q=${searchTerm}")
        .check(jsonPath("$.resultsContext").saveAs("context")))
        .pause(1)
        .exec(http(s"/search?q=${searchTerm}" + "&c=${context}")
          .get(s"/search?q=${searchTerm}" + "&c=${context}"))
    }
  }

  /**
    * Calls against the "/sessions" endpoint
    */
  object Sessions {

    /**
      * Performs a call to "/sessions/${sessionId}"
      */
    def sessionId(sessionId: String) = {
      exec(http("/sessions/${sessionId}").get("/sessions/${sessionId}")).pause(350 milliseconds)
    }

  }


  /**
    * Calls against the "/ascii" endpoint
    */
  object Ascii {

    /**
      * Performs a call to "/ascii?q=${searchTerm}"
      */
    def search(term: String) = {
      exec(http(s"/ascii?q=${term}").get(s"/ascii?q=${term}"))
    }
  }

  val httpConf = http
    .baseURL("http://localhost:8080")
    .acceptHeader("text/html,application/json,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
    .doNotTrackHeader("1")
    .acceptLanguageHeader("en-US,en;q=0.5")
    .acceptEncodingHeader("gzip, deflate")
    .userAgentHeader("Mozilla/5.0 (Windows NT 5.1; rv:31.0) Gecko/20100101 Firefox/31.0")

  val feeder = jsonFile("queries.json").circular.random
  var scn = scenario("Conference Day")
    .feed(feeder)
    .randomSwitch(
      70d -> Search.search("${searchTerm}"),
      20d -> Ascii.search("${searchTerm}"),
      10d -> Search.withContext("${searchTerm}")
    )

  setUp(
    scn.inject(
      atOnceUsers(5),
      constantUsersPerSec(5) during (30 seconds), // go up to 150, something popular happens
      rampUsersPerSec(10)to(30) during (2 minutes), // more visitors
      constantUsersPerSec(10) during(30 seconds)
    )
  ).protocols(httpConf)
}
