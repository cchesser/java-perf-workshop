import io.gatling.app.Gatling

object Engine extends App {

  // Programmatic-ish runner: construct CLI args programmatically and
  // invoke Gatling's main entry point. This avoids direct access to the
  // internal Runner API while still allowing control over simulation
  // class and results directory.

  val simulationClass = "cchesser.javaperf.workshop.WorkshopSimulation"
  val resultsDir = cchesser.javaperf.workshop.IDEPathHelper.resultsDirectory.toString

  val cliArgs = Array("-s", simulationClass, "-rf", resultsDir)

  Gatling.main(cliArgs)
}
