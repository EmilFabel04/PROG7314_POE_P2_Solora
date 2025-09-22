#!/bin/sh

APP_BASE_NAME=`basename "$0"`

DEFAULT_JVM_OPTS=""

CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar

JAVA_EXE="${JAVA_HOME}/bin/java"
if [ -z "$JAVA_HOME" ]; then
  JAVA_EXE=`which java`
fi

exec "$JAVA_EXE" $DEFAULT_JVM_OPTS -cp "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"


