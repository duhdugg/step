#!/usr/bin/env bash

export JAVA_HOME=/home/$USER/.local/share/graalvm
export PATH=/home/$USER/.local/share/graalvm/bin:$PATH
export MAVEN_OPTS="-Xmx8g"

java --version
native-image --version
