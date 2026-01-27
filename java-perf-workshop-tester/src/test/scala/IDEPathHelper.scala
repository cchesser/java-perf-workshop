package cchesser.javaperf.workshop

import java.nio.file.{Path, Paths}

object IDEPathHelper {

	private val gatlingConfUrl = getClass.getClassLoader.getResource("gatling.conf")
	private val gatlingConfPath: Path = Paths.get(gatlingConfUrl.toURI)
	private def ancestor(path: Path, n: Int): Path = (1 to n).foldLeft(path)((p, _) => p.getParent)
	val projectRootDir: Path = ancestor(gatlingConfPath, 3)

	val mavenSourcesDirectory: Path = projectRootDir.resolve("src").resolve("test").resolve("scala")
	val mavenResourcesDirectory: Path = projectRootDir.resolve("src").resolve("test").resolve("resources")
	val mavenTargetDirectory: Path = projectRootDir.resolve("target")
	val mavenBinariesDirectory: Path = mavenTargetDirectory.resolve("test-classes")

	val dataDirectory: Path = mavenResourcesDirectory.resolve("data")
	val bodiesDirectory: Path = mavenResourcesDirectory.resolve("bodies")

	val recorderOutputDirectory: Path = mavenSourcesDirectory
	val resultsDirectory: Path = mavenTargetDirectory.resolve("gatling")

	val recorderConfigFile: Path = mavenResourcesDirectory.resolve("recorder.conf")
}
