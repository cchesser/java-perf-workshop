package cchesser.javaperf.workshop

import io.gatling.recorder.GatlingRecorder

object Recorder extends App {

	// Use the recorder CLI entry point available in Gatling 3.x. The
	// recorder parses positional arguments as: <simulationsFolder> <resourcesFolder> [pkg] [className]
	// We provide the project's test source and resources locations so
	// recorded scenarios are written into the test tree.

	val sims = IDEPathHelper.recorderOutputDirectory.toString
	val resources = IDEPathHelper.mavenResourcesDirectory.toString
	val pkg = "cchesser.javaperf.workshop"

	GatlingRecorder.main(Array(sims, resources, pkg))
}
