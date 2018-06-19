package cchesser.javaperf.workshop

import io.gatling.core.Predef._
import io.gatling.http.Predef._

import scala.concurrent.duration._

/**
  *
  */
class WorkshopSimulation extends Simulation {

  val httpConf = http
    .baseURL("http://localhost:8080")
    .acceptHeader("text/html,application/json,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
    .doNotTrackHeader("1")
    .acceptLanguageHeader("en-US,en;q=0.5")
    .acceptEncodingHeader("gzip, deflate")
    .userAgentHeader("Mozilla/5.0 (Windows NT 5.1; rv:31.0) Gecko/20100101 Firefox/31.0")

  val searchTerms = ('a' to 'z').map(x => Map("searchTerm" -> x)).toArray
  val feeder = searchTerms.circular.random
  var scn = scenario("Conference Day")
    .feed(feeder)
    .randomSwitch(
      99d -> Search.search("${searchTerm}"),
      1d -> Search.withContext("${searchTerm}")
    )

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
      )
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

  setUp(
    scn.inject(
      atOnceUsers(200),
      constantUsersPerSec(10) during (30 seconds), // go up to 300, something popular happens
      rampUsersPerSec(20) to (100) during (1 minutes), // more visitors
      constantUsersPerSec(100) during (30 seconds)
    )
  ).protocols(httpConf)
}
