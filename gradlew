#!/bin/sh
# Gradle wrapper script - downloads Gradle automatically
set -e

GRADLE_VERSION="8.8"
GRADLE_HOME="${HOME}/.gradle/wrapper/dists/gradle-${GRADLE_VERSION}-bin"
GRADLE_BIN="${GRADLE_HOME}/gradle-${GRADLE_VERSION}/bin/gradle"

if [ ! -f "$GRADLE_BIN" ]; then
  echo "Downloading Gradle ${GRADLE_VERSION}..."
  mkdir -p "$GRADLE_HOME"
  curl -fL "https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip" \
    -o "/tmp/gradle-${GRADLE_VERSION}-bin.zip"
  unzip -qo "/tmp/gradle-${GRADLE_VERSION}-bin.zip" -d "$GRADLE_HOME"
  rm -f "/tmp/gradle-${GRADLE_VERSION}-bin.zip"
fi

exec "$GRADLE_BIN" "$@"
