#!/bin/sh
#
# Gradle wrapper script
#
# This script downloads the gradle wrapper JAR if it doesn't exist
# and then executes Gradle.

DIR="$(cd "$(dirname "$0")" && pwd)"
GRADLE_WRAPPER_JAR="$DIR/gradle/wrapper/gradle-wrapper.jar"
GRADLE_PROPERTIES="$DIR/gradle/wrapper/gradle-wrapper.properties"

# Download wrapper JAR if missing
if [ ! -f "$GRADLE_WRAPPER_JAR" ] || [ ! -s "$GRADLE_WRAPPER_JAR" ]; then
    WRAPPER_URL="https://github.com/gradle/gradle/raw/v8.4.0/gradle/wrapper/gradle-wrapper.jar"
    mkdir -p "$DIR/gradle/wrapper"
    if command -v wget >/dev/null 2>&1; then
        wget -q "$WRAPPER_URL" -O "$GRADLE_WRAPPER_JAR" 2>/dev/null || curl -sL "$WRAPPER_URL" -o "$GRADLE_WRAPPER_JAR"
    else
        curl -sL "$WRAPPER_URL" -o "$GRADLE_WRAPPER_JAR"
    fi
fi

# Use system gradle if wrapper JAR is missing or invalid
if [ ! -f "$GRADLE_WRAPPER_JAR" ] || [ ! -s "$GRADLE_WRAPPER_JAR" ]; then
    exec gradle "$@"
fi

# Execute via wrapper JAR
exec java -cp "$GRADLE_WRAPPER_JAR" org.gradle.wrapper.GradleWrapperMain "$@"
