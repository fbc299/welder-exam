#!/bin/sh
# Gradle wrapper script for welder-exam
# Uses Gradle from PATH if available, otherwise downloads

APP_HOME="$(cd "${0%/*}" && pwd -P)"
JAR="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"

if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ]; then
    JAVACMD="$JAVA_HOME/bin/java"
else
    JAVACMD="java"
fi

# If wrapper JAR exists, use it
if [ -f "$JAR" ]; then
    exec "$JAVACMD" -Xmx64m -Xms64m -Dorg.gradle.appname=gradlew -classpath "$JAR" org.gradle.wrapper.GradleWrapperMain "$@"
fi

# Otherwise try system gradle
exec gradle "$@"
