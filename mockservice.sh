mvn dependency:copy -Dartifact=com.github.tomakehurst:wiremock-standalone:2.5.1 -Dmdep.stripVersion=true -DoutputDirectory=.

java -jar wiremock-standalone.jar --port 9090 --root-dir java-perf-workshop-server/src/test/resources
