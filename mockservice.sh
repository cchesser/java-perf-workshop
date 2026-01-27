#!/usr/bin/env bash
set -euo pipefail

# Downloads the latest WireMock 2.x standalone JAR (resolved by Maven) and runs it.
# Uses a version range [2,3) so Maven picks the highest 2.x available in your repositories.

OUTPUT_DIR="."
WIREMOCK_JAR="wiremock-standalone.jar"
PORT="9090"
ROOT_DIR="java-perf-workshop-server/src/test/resources"

DEFAULT_VERSION="2.27.2"
METADATA_URL="https://repo1.maven.org/maven2/com/github/tomakehurst/wiremock-standalone/maven-metadata.xml"

download_with_wget() {
	url="$1"
	out="$2"
	if command -v wget >/dev/null 2>&1; then
		wget -q "$url" -O "$out"
	elif command -v curl >/dev/null 2>&1; then
		curl -sSfL "$url" -o "$out"
	else
		echo "Either wget or curl is required to download WireMock." >&2
		return 2
	fi
}

if [ ! -f "$OUTPUT_DIR/$WIREMOCK_JAR" ]; then
	echo "Resolving latest WireMock 2.x from Maven Central..."
	if metadata=$(curl -sSfL "$METADATA_URL" 2>/dev/null || true); then
		version=$(printf "%s" "$metadata" | grep -oE '<version>[^<]+' | sed 's/<version>//' | grep '^2\.' | sort -V | tail -n1 || true)
	fi
	if [ -z "${version:-}" ]; then
		echo "Could not resolve latest 2.x version, falling back to $DEFAULT_VERSION"
		version="$DEFAULT_VERSION"
	else
		echo "Resolved WireMock version: $version"
	fi

	jar_url="https://repo1.maven.org/maven2/com/github/tomakehurst/wiremock-standalone/${version}/wiremock-standalone-${version}.jar"
	echo "Downloading WireMock $version from Maven Central..."
	download_with_wget "$jar_url" "$OUTPUT_DIR/$WIREMOCK_JAR"
fi

if [ ! -f "$OUTPUT_DIR/$WIREMOCK_JAR" ]; then
	echo "Failed to download WireMock standalone jar." >&2
	exit 3
fi

echo "Starting WireMock (port=$PORT, root=$ROOT_DIR)"
exec java -jar "$OUTPUT_DIR/$WIREMOCK_JAR" --port "$PORT" --root-dir "$ROOT_DIR"
